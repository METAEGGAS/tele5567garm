package com.sys.update2

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object QuotexSocket {

    private const val TAG = "QuotexSocket"
    private const val IS_DEMO = 1
    private const val WS_URL = "wss://ws2.qxbroker.com/socket.io/?EIO=4&transport=websocket"
    private const val MAX_RECONNECT = 5

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var ws: WebSocket? = null
    private var ssid: String? = null
    private var lastCookie: String = ""
    private var lastUa: String = ""
    private val handler = Handler(Looper.getMainLooper())

    private val prices = HashMap<String, Double>()
    private val instruments = HashMap<String, Instrument>()
    private var demoBalance: Double = 0.0
    private var liveBalance: Double = 0.0

    private var reconnectAttempts = 0
    private var manualDisconnect = false

    data class Instrument(
        val id: Int,
        val ticker: String,
        val name: String,
        val isOtc: Boolean,
        val payout: Int = 0
    )

    var onStatus: ((String) -> Unit)? = null
    var onPrice: ((String, Double) -> Unit)? = null
    var onInstrumentsLoaded: ((List<Instrument>) -> Unit)? = null
    var onTradeResult: ((Boolean, String) -> Unit)? = null
    var onBalanceUpdate: ((Double) -> Unit)? = null

    fun getPrice(asset: String): Double? = prices[asset]
    fun getInstrumentList(): List<Instrument> = instruments.values.toList()
    fun getBalance(): Double = if (IS_DEMO == 1) demoBalance else liveBalance
    fun isConnected(): Boolean = ws != null

    fun getDefaultOtcList(): List<Instrument> {
        val list = mutableListOf<Instrument>()
        var id = 1000
        val all = listOf(
            "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc", "AUDUSD_otc",
            "USDCAD_otc", "USDCHF_otc", "NZDUSD_otc", "EURGBP_otc",
            "EURJPY_otc", "EURCHF_otc", "GBPJPY_otc", "GBPCHF_otc",
            "AUDJPY_otc", "CADJPY_otc", "CHFJPY_otc", "NZDJPY_otc",
            "BTCUSD_otc", "ETHUSD_otc", "SOLUSD_otc", "XRPUSD_otc",
            "DOGEUSD_otc", "LTCUSD_otc", "BNBUSD_otc", "ADAUSD_otc",
            "XAUUSD_otc", "XAGUSD_otc", "UKBrent_otc", "USCrude_otc"
        )
        for (t in all) {
            list.add(Instrument(id++, t, t.replace("_", " ").uppercase(), true))
        }
        return list
    }

    // ═══════════════════════════════════════════
    //  Connect — مع Cookie + UA كاملين
    // ═══════════════════════════════════════════
    fun connect(token: String, cookie: String = "", ua: String = "") {
        manualDisconnect = false
        reconnectAttempts = 0
        this.ssid = token
        this.lastCookie = cookie
        this.lastUa = ua
        openSocket(token, cookie, ua)
    }

    private fun openSocket(token: String, cookie: String, ua: String) {
        // أغلق أي اتصال قديم قبل فتح جديد
        try { ws?.close(1000, "reconnect") } catch (_: Exception) {}
        ws = null

        val builder = Request.Builder()
            .url(WS_URL)
            .addHeader("Origin", "https://qxbroker.com")
            .addHeader(
                "User-Agent",
                if (ua.isNotBlank()) ua else
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .addHeader("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
            .addHeader("Pragma", "no-cache")
            .addHeader("Cache-Control", "no-cache")

        if (cookie.isNotBlank()) {
            builder.addHeader("Cookie", cookie)
        }

        val req = builder.build()
        Log.d(TAG, "Connecting to WS...")

        ws = client.newWebSocket(req, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WS opened — code ${response.code}")
                reconnectAttempts = 0
                emit("🔌 متصل — handshake")
                webSocket.send("40")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code ?: 0
                Log.e(TAG, "WS failed: $code / ${t.message}")
                ws = null
                emit("❌ فشل ($code): ${t.message?.take(40)}")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS closed: $code / $reason")
                ws = null
                emit("🔌 انقطع ($code)")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (manualDisconnect) return
        val token = ssid ?: return
        if (reconnectAttempts >= MAX_RECONNECT) {
            emit("❌ توقفت إعادة المحاولة — أعد تسجيل الدخول")
            return
        }
        reconnectAttempts++
        val delayMs = (reconnectAttempts * 3000L).coerceAtMost(15000L)
        emit("🔄 إعادة اتصال ${reconnectAttempts}/$MAX_RECONNECT بعد ${delayMs / 1000}ث")
        handler.postDelayed({
            if (!manualDisconnect) openSocket(token, lastCookie, lastUa)
        }, delayMs)
    }

    fun disconnect() {
        manualDisconnect = true
        handler.removeCallbacksAndMessages(null)
        try { ws?.close(1000, "bye") } catch (_: Exception) {}
        ws = null
    }

    private fun emit(s: String) {
        handler.post { onStatus?.invoke(s) }
    }

    private fun handleMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "← ${text.take(200)}")

        when {
            // رد handshake الأولي من السيرفر
            text.startsWith("0{") -> {
                webSocket.send("40")
                return
            }
            // القناة انفتحت — أرسل المصادقة
            text == "40" || text.startsWith("40{") -> {
                emit("✅ القناة مفتوحة — إرسال Token")
                sendAuthorization(webSocket)
                return
            }
            // Ping من السيرفر — رد بـ Pong (إجباري)
            text == "2" -> {
                webSocket.send("3")
                return
            }
            // إغلاق منطقي من Socket.IO
            text == "41" -> {
                ws = null
                emit("🔌 السيرفر أغلق القناة")
                scheduleReconnect()
                return
            }
        }

        if (!text.startsWith("42")) {
            if (text.contains("demoBalance", true) ||
                text.contains("liveBalance", true)) {
                parseBalance(text)
            }
            return
        }

        val jsonStr = text.substring(2).trim()

        try {
            val arr = JSONArray(jsonStr)
            if (arr.length() < 2) return

            val eventName = arr.optString(0, "")
            val payload = arr.opt(1)

            when (eventName) {
                "authorizationStatus" -> handleAuthStatus(payload)
                "instruments/list", "instruments/update" -> parseInstruments(payload)
                "quotes/stream", "quotes", "tick" -> parseQuotes(payload)
                "history/list", "history/list/v2", "history/load" -> parseQuotes(payload)
                "candles", "candles-generate" -> parseCandles(payload)
                "orders/open" -> handleOrderOpen(payload)
                "orders/close" -> handleOrderClose(payload)
                "orders/error" -> handleOrderError(payload)
                else -> if (jsonStr.contains("balance", true)) parseBalance(jsonStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse err: ${e.message}")
        }

        if (jsonStr.contains("demoBalance", true) ||
            jsonStr.contains("liveBalance", true)) {
            parseBalance(jsonStr)
        }
    }

    private fun sendAuthorization(w: WebSocket) {
        val payload = JSONObject().apply {
            put("session", ssid ?: "")
            put("isDemo", IS_DEMO)
            put("tournamentId", 0)
        }
        w.send("""42["authorization",$payload]""")
    }

    private fun handleAuthStatus(payload: Any?) {
        val status = when (payload) {
            is Boolean -> payload
            is String -> payload == "true" || payload.contains("success", true)
            is JSONObject -> payload.optBoolean("status", true)
            else -> true
        }

        if (status) {
            emit("🎉 تم الدخول — جلب الأدوات")
            ws?.send("""42["instruments/list"]""")
            ws?.send("""42["pending/list"]""")
            subscribeDefault()
        } else {
            emit("❌ فشل المصادقة — التوكن منتهي")
        }
    }

    private fun parseInstruments(payload: Any?) {
        try {
            val list = mutableListOf<Instrument>()

            when (payload) {
                is JSONArray -> {
                    for (i in 0 until payload.length()) {
                        val obj = payload.optJSONObject(i) ?: continue
                        addInstrument(obj, list)
                    }
                }
                is JSONObject -> {
                    val data = payload.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val obj = data.optJSONObject(i) ?: continue
                            addInstrument(obj, list)
                        }
                    } else {
                        val keys = payload.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val v = payload.optJSONObject(k) ?: continue
                            addInstrument(v, list)
                        }
                    }
                }
            }

            if (list.isNotEmpty()) {
                emit("✅ ${list.size} أداة")
                handler.post { onInstrumentsLoaded?.invoke(list) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseInstruments: ${e.message}")
        }
    }

    private fun addInstrument(obj: JSONObject, out: MutableList<Instrument>) {
        val id = obj.optInt("id", -1)
        val ticker = obj.optString("ticker",
            obj.optString("asset",
            obj.optString("symbol", "")))
        if (ticker.isBlank()) return

        val name = obj.optString("name", ticker)
        val payout = obj.optInt("payout", obj.optInt("payout_percent", 0))
        val isOtc = ticker.contains("_otc", true)

        instruments[ticker] = Instrument(
            id = if (id > 0) id else ticker.hashCode(),
            ticker = ticker,
            name = name,
            isOtc = isOtc,
            payout = payout
        )
        out.add(instruments[ticker]!!)
    }

    private fun parseQuotes(payload: Any?) {
        when (payload) {
            is JSONArray -> for (i in 0 until payload.length()) {
                val o = payload.optJSONObject(i) ?: continue
                addQuote(o)
            }
            is JSONObject -> {
                val keys = payload.keys()
                var handled = false
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = payload.opt(k)
                    if (v is Double && k.contains("_otc", true)) {
                        prices[k] = v
                        handler.post { onPrice?.invoke(k, v) }
                        handled = true
                    } else if (v is JSONObject) {
                        addQuote(v)
                        handled = true
                    }
                }
                if (!handled) addQuote(payload)
            }
        }
    }

    // الشموع تحمل السعر في حقول close / price
    private fun parseCandles(payload: Any?) {
        try {
            when (payload) {
                is JSONArray -> {
                    for (i in 0 until payload.length()) {
                        val o = payload.optJSONObject(i) ?: continue
                        addQuote(o)
                    }
                }
                is JSONObject -> {
                    val data = payload.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val o = data.optJSONObject(i) ?: continue
                            addQuote(o)
                        }
                    } else addQuote(payload)
                }
            }
        } catch (_: Exception) {}
    }

    private fun addQuote(obj: JSONObject) {
        val asset = obj.optString("asset",
            obj.optString("ticker",
            obj.optString("symbol", "")))
        val price = obj.optDouble("price",
            obj.optDouble("close",
            obj.optDouble("value", Double.NaN)))

        if (asset.isNotBlank() && !price.isNaN()) {
            prices[asset] = price
            handler.post { onPrice?.invoke(asset, price) }
        }
    }

    private val defaultAssets = listOf(
        "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc",
        "BTCUSD_otc", "ETHUSD_otc", "XAUUSD_otc"
    )

    private fun subscribeDefault() {
        defaultAssets.forEachIndexed { i, asset ->
            handler.postDelayed({ subscribeAsset(asset) }, (i * 300).toLong())
        }
    }

    fun subscribeAsset(asset: String, period: Int = 60) {
        val w = ws ?: return
        try {
            val payload = JSONObject().apply {
                put("asset", asset)
                put("period", period)
            }
            w.send("""42["subscribe_candles",$payload]""")
            w.send("""42["history/list/v2",$payload]""")
        } catch (_: Exception) {}
    }

    fun executeTrade(
        asset: String,
        amount: Double,
        direction: String,
        durationSec: Int
    ) {
        val w = ws ?: run {
            handler.post { onTradeResult?.invoke(false, "❌ لا يوجد اتصال") }
            return
        }

        try {
            val expiresAt = (System.currentTimeMillis() / 1000L) + durationSec
            val payload = JSONObject().apply {
                put("asset", asset)
                put("direction", direction)
                put("amount", amount)
                put("expiresAt", expiresAt)
                put("optionType", 100)
            }

            w.send("""42["orders/open",$payload]""")
            emit("📤 صفقة: $direction $asset \$$amount (${durationSec}s)")
            handler.post { onTradeResult?.invoke(true, "⏳ جاري التنفيذ...") }
        } catch (e: Exception) {
            handler.post { onTradeResult?.invoke(false, "❌ ${e.message}") }
        }
    }

    private fun handleOrderOpen(payload: Any?) {
        try {
            val obj = payload as? JSONObject ?: return
            val dealId = obj.optString("deal_idt", obj.optString("dealId", ""))
            handler.post { onTradeResult?.invoke(true, "✅ صفقة مفتوحة ($dealId)") }
            if (obj.has("demoBalance")) parseBalance(obj.toString())
        } catch (_: Exception) {}
    }

    private fun handleOrderClose(payload: Any?) {
        try {
            val obj = payload as? JSONObject ?: return
            val profit = obj.optDouble("amount_profit", 0.0)
            val ok = profit >= 0
            handler.post {
                onTradeResult?.invoke(
                    ok,
                    if (ok) "🎉 ربح \$${String.format("%.2f", profit)}"
                    else "📉 خسارة \$${String.format("%.2f", Math.abs(profit))}"
                )
            }
        } catch (_: Exception) {}
    }

    private fun handleOrderError(payload: Any?) {
        handler.post {
            onTradeResult?.invoke(false, "❌ فشلت: ${payload.toString().take(80)}")
        }
    }

    private fun parseBalance(text: String) {
        try {
            val start = text.indexOf("{")
            if (start == -1) return
            val obj = JSONObject(text.substring(start))

            val demo = obj.optDouble("demoBalance", Double.NaN)
            val live = obj.optDouble("liveBalance", Double.NaN)

            if (!demo.isNaN()) demoBalance = demo
            if (!live.isNaN()) liveBalance = live

            val current = if (IS_DEMO == 1) demoBalance else liveBalance
            handler.post { onBalanceUpdate?.invoke(current) }
        } catch (_: Exception) {}
    }
}
