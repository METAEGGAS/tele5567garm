package com.sys.update2

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * QuotexSocket — مبني على بروتوكول QTXSY المؤكد:
 * - authorization → authorizationStatus
 * - instruments/list → instruments/update
 * - quotes/stream + history/list/v2 (الأسعار)
 * - orders/open → orders/open (نجاح) أو orders/error (فشل)
 * - orders/close
 * - orders/opened/list, orders/closed/list
 * - pending/list
 * - demoBalance, liveBalance
 */
object QuotexSocket {

    private const val TAG = "QuotexSocket"
    private const val IS_DEMO = 1

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var ssid: String? = null
    private val handler = Handler(Looper.getMainLooper())

    // الحالة
    private val prices = HashMap<String, Double>()
    private val instruments = HashMap<String, Instrument>()
    private var demoBalance: Double = 0.0
    private var liveBalance: Double = 0.0

    data class Instrument(
        val id: Int,
        val ticker: String,
        val name: String,
        val isOtc: Boolean,
        val payout: Int = 0
    )

    // Callbacks
    var onStatus: ((String) -> Unit)? = null
    var onPrice: ((String, Double) -> Unit)? = null
    var onInstrumentsLoaded: ((List<Instrument>) -> Unit)? = null
    var onTradeResult: ((Boolean, String) -> Unit)? = null
    var onBalanceUpdate: ((Double) -> Unit)? = null

    // ═══════════════════════════════════════════
    //  Public
    // ═══════════════════════════════════════════
    fun getPrice(asset: String): Double? = prices[asset]
    fun getInstrumentList(): List<Instrument> = instruments.values.toList()
    fun getBalance(): Double = if (IS_DEMO == 1) demoBalance else liveBalance

    fun getDefaultOtcList(): List<Instrument> {
        val list = mutableListOf<Instrument>()
        var id = 1000

        val forex = listOf(
            "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc", "AUDUSD_otc",
            "USDCAD_otc", "USDCHF_otc", "NZDUSD_otc", "EURGBP_otc",
            "EURJPY_otc", "EURCHF_otc", "GBPJPY_otc", "GBPCHF_otc",
            "AUDJPY_otc", "CADJPY_otc", "CHFJPY_otc", "NZDJPY_otc"
        )
        val crypto = listOf(
            "BTCUSD_otc", "ETHUSD_otc", "SOLUSD_otc", "XRPUSD_otc",
            "DOGEUSD_otc", "LTCUSD_otc", "BNBUSD_otc", "ADAUSD_otc"
        )
        val comm = listOf(
            "XAUUSD_otc", "XAGUSD_otc", "UKBrent_otc", "USCrude_otc"
        )

        for (t in forex + crypto + comm) {
            list.add(Instrument(
                id = id++,
                ticker = t,
                name = t.replace("_", " ").uppercase(),
                isOtc = true
            ))
        }
        return list
    }

    // ═══════════════════════════════════════════
    //  Connect
    // ═══════════════════════════════════════════
    fun connect(ssid: String) {
        this.ssid = ssid

        val url = "wss://ws2.qxbroker.com/socket.io/?EIO=4&transport=websocket"
        val req = Request.Builder()
            .url(url)
            .addHeader("Origin", "https://qxbroker.com")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .build()

        ws = client.newWebSocket(req, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WS opened")
                emit("🔌 متصل — إرسال handshake")
                webSocket.send("40")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS failed: ${t.message}", t)
                emit("❌ فشل: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS closed: $code / $reason")
                emit("🔌 انقطع ($code)")
            }
        })
    }

    fun disconnect() {
        try { ws?.close(1000, "bye") } catch (_: Exception) {}
        ws = null
    }

    private fun emit(s: String) {
        handler.post { onStatus?.invoke(s) }
    }

    // ═══════════════════════════════════════════
    //  Handle messages
    // ═══════════════════════════════════════════
    private fun handleMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "← ${text.take(300)}")

        // Engine.IO handshake
        when {
            text.startsWith("0{") -> {
                webSocket.send("40")
                return
            }

            text == "40" -> {
                emit("✅ القناة مفتوحة — إرسال SSID")
                sendAuthorization(webSocket)
                return
            }

            text == "2" -> {
                webSocket.send("3")
                return
            }
        }

        // Socket.IO events (تبدأ بـ 42)
        if (!text.startsWith("42")) {
            // بحث يدوي للرصيد خارج events
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
                "history/list", "history/list/v2", "history/load" -> parseHistory(payload)
                "orders/open" -> handleOrderOpen(payload)
                "orders/close" -> handleOrderClose(payload)
                "orders/error" -> handleOrderError(payload)
                "orders/opened/list" -> handleOpenedList(payload)
                "orders/closed/list" -> handleClosedList(payload)
                "pending/list" -> handlePendingList(payload)
                else -> {
                    // fallback
                    if (jsonStr.contains("balance", true)) parseBalance(jsonStr)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse err: ${e.message}")
        }

        // بحث يدوي للرصيد
        if (jsonStr.contains("demoBalance", true) ||
            jsonStr.contains("liveBalance", true)) {
            parseBalance(jsonStr)
        }
    }

    // ═══════════════════════════════════════════
    //  Auth
    // ═══════════════════════════════════════════
    private fun sendAuthorization(ws: WebSocket) {
        val payload = JSONObject().apply {
            put("session", ssid ?: "")
            put("isDemo", IS_DEMO)
            put("tournamentId", 0)
        }
        ws.send("""42["authorization",$payload]""")
    }

    private fun handleAuthStatus(payload: Any?) {
        val status = when (payload) {
            is Boolean -> payload
            is String -> payload == "true" || payload.contains("success", true)
            is JSONObject -> payload.optBoolean("status", true)
            else -> true
        }

        if (status) {
            emit("🎉 تم الدخول — جاري جلب الأدوات")
            ws?.send("""42["instruments/list"]""")
            ws?.send("""42["pending/list"]""")
            subscribeDefault()
        } else {
            emit("❌ فشل المصادقة — التوكن منتهي")
        }
    }

    // ═══════════════════════════════════════════
    //  Instruments
    // ═══════════════════════════════════════════
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
        val payout = obj.optInt("payout",
            obj.optInt("payout_percent", 0))
        val isOtc = ticker.contains("_otc", true)

        val inst = Instrument(
            id = if (id > 0) id else ticker.hashCode(),
            ticker = ticker,
            name = name,
            isOtc = isOtc,
            payout = payout
        )
        instruments[ticker] = inst
        out.add(inst)
    }

    // ═══════════════════════════════════════════
    //  Quotes / Prices
    // ═══════════════════════════════════════════
    private fun parseQuotes(payload: Any?) {
        when (payload) {
            is JSONArray -> for (i in 0 until payload.length()) {
                val o = payload.optJSONObject(i) ?: continue
                addQuote(o)
            }
            is JSONObject -> {
                // ممكن تكون {"EURUSD_otc": 1.2345} خريطة
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

    private fun parseHistory(payload: Any?) {
        parseQuotes(payload)
    }

    // ═══════════════════════════════════════════
    //  Subscribe
    // ═══════════════════════════════════════════
    private val defaultAssets = listOf(
        "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc",
        "BTCUSD_otc", "ETHUSD_otc",
        "XAUUSD_otc"
    )

    private fun subscribeDefault() {
        defaultAssets.forEachIndexed { i, asset ->
            handler.postDelayed({
                subscribeAsset(asset)
            }, (i * 300).toLong())
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

    // ═══════════════════════════════════════════
    //  Orders
    // ═══════════════════════════════════════════
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
        if (IS_DEMO != 1) {
            handler.post { onTradeResult?.invoke(false, "❌ Live mode disabled") }
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
            val dealId = obj.optString("deal_idt",
                obj.optString("dealId", ""))
            handler.post {
                onTradeResult?.invoke(true, "✅ صفقة مفتوحة ($dealId)")
            }
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
            onTradeResult?.invoke(
                false,
                "❌ فشلت الصفقة: ${payload.toString().take(80)}"
            )
        }
    }

    private fun handleOpenedList(payload: Any?) {}
    private fun handleClosedList(payload: Any?) {}
    private fun handlePendingList(payload: Any?) {}

    // ═══════════════════════════════════════════
    //  Balance
    // ═══════════════════════════════════════════
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
