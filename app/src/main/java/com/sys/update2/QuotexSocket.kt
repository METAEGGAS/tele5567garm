package com.sys.update2

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * QuotexSocket — Socket.IO client لـ Quotex
 * - مصادقة تلقائية
 * - استقبال قائمة الأزواج تلقائياً
 * - استقبال الأسعار OTC
 * - تنفيذ صفقات Demo
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

    // ═══════════════════════════════════════════
    //  State
    // ═══════════════════════════════════════════
    private val prices = HashMap<String, Double>()          // السعر الحالي
    private val instruments = HashMap<String, Instrument>()  // قائمة الأزواج من Quotex
    private val balance = hashMapOf<String, Double>()        // الأرصدة

    data class Instrument(
        val id: Int,
        val ticker: String,
        val name: String,
        val isOtc: Boolean
    )

    // ═══════════════════════════════════════════
    //  Callbacks
    // ═══════════════════════════════════════════
    var onStatus: ((String) -> Unit)? = null
    var onPrice: ((String, Double) -> Unit)? = null
    var onInstrumentsLoaded: ((List<Instrument>) -> Unit)? = null
    var onTradeResult: ((Boolean, String) -> Unit)? = null
    var onBalanceUpdate: ((Double) -> Unit)? = null

    // ═══════════════════════════════════════════
    //  Public helpers
    // ═══════════════════════════════════════════
    fun getPrice(asset: String): Double? = prices[asset]
    fun getInstrumentList(): List<Instrument> = instruments.values.toList()

    /**
     * قائمة OTC الافتراضية (تُستخدم لحين ما يوصل الرد من Quotex)
     */
    fun getDefaultOtcList(): List<Instrument> {
        val list = mutableListOf<Instrument>()
        var id = 1

        // Forex OTC الرئيسية
        val forexOtc = listOf(
            "EURUSD_otc" to "EUR/USD OTC",
            "GBPUSD_otc" to "GBP/USD OTC",
            "USDJPY_otc" to "USD/JPY OTC",
            "AUDUSD_otc" to "AUD/USD OTC",
            "USDCAD_otc" to "USD/CAD OTC",
            "USDCHF_otc" to "USD/CHF OTC",
            "NZDUSD_otc" to "NZD/USD OTC",
            "EURGBP_otc" to "EUR/GBP OTC",
            "EURJPY_otc" to "EUR/JPY OTC",
            "EURCHF_otc" to "EUR/CHF OTC",
            "EURCAD_otc" to "EUR/CAD OTC",
            "EURAUD_otc" to "EUR/AUD OTC",
            "EURNZD_otc" to "EUR/NZD OTC",
            "GBPJPY_otc" to "GBP/JPY OTC",
            "GBPCHF_otc" to "GBP/CHF OTC",
            "GBPCAD_otc" to "GBP/CAD OTC",
            "GBPAUD_otc" to "GBP/AUD OTC",
            "AUDJPY_otc" to "AUD/JPY OTC",
            "AUDCHF_otc" to "AUD/CHF OTC",
            "AUDCAD_otc" to "AUD/CAD OTC",
            "CADJPY_otc" to "CAD/JPY OTC",
            "CADCHF_otc" to "CAD/CHF OTC",
            "CHFJPY_otc" to "CHF/JPY OTC",
            "NZDJPY_otc" to "NZD/JPY OTC"
        )
        for ((t, n) in forexOtc) {
            list.add(Instrument(id++, t, n, true))
        }

        // Crypto OTC
        val cryptoOtc = listOf(
            "BTCUSD_otc" to "BTC/USD OTC",
            "ETHUSD_otc" to "ETH/USD OTC",
            "BNBUSD_otc" to "BNB/USD OTC",
            "SOLUSD_otc" to "SOL/USD OTC",
            "XRPUSD_otc" to "XRP/USD OTC",
            "DOGEUSD_otc" to "DOGE/USD OTC",
            "LTCUSD_otc" to "LTC/USD OTC",
            "ADAUSD_otc" to "ADA/USD OTC"
        )
        for ((t, n) in cryptoOtc) {
            list.add(Instrument(id++, t, n, true))
        }

        // Commodities OTC
        val commOtc = listOf(
            "XAUUSD_otc" to "Gold OTC",
            "XAGUSD_otc" to "Silver OTC",
            "UKBrent_otc" to "Brent OTC",
            "USCrude_otc" to "Crude OTC"
        )
        for ((t, n) in commOtc) {
            list.add(Instrument(id++, t, n, true))
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
                emit("🔌 متصل — بدء handshake")
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

        when {
            text.startsWith("0{") -> webSocket.send("40")

            text == "40" -> {
                emit("✅ القناة مفتوحة — إرسال التوكن")
                sendAuthorization(webSocket)
            }

            text == "2" -> webSocket.send("3")

            // مصادقة ناجحة
            text.contains("authorization") -> {
                emit("🎉 دخول ناجح — جاري جلب الأزواج")
                // اطلب قائمة الأزواج
                requestInstruments(webSocket)
                // اشترك في الأزواج الافتراضية
                subscribeDefaultAssets(webSocket)
            }

            // قائمة الأزواج
            text.contains("instruments") -> parseInstruments(text)

            // الرصيد
            text.contains("\"balance\"") -> parseBalance(text)

            // بيانات الأسعار
            text.contains("candles") || text.contains("quotes")
                || text.contains("tick") -> parsePrices(text)

            // نتيجة صفقة
            text.contains("trade") && (text.contains("success")
                || text.contains("error")) -> parseTradeResult(text)

            // خطأ
            text.startsWith("44") || text.contains("error", true) -> {
                emit("⚠️ السيرفر: ${text.take(120)}")
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Auth
    // ═══════════════════════════════════════════
    private fun sendAuthorization(ws: WebSocket) {
        val auth = JSONObject().apply {
            put("session", ssid ?: "")
            put("isDemo", IS_DEMO)
            put("tournamentId", 0)
        }
        ws.send("42[\"authorization\",$auth]")
    }

    // ═══════════════════════════════════════════
    //  Instruments list
    // ═══════════════════════════════════════════
    private fun requestInstruments(ws: WebSocket) {
        try {
            ws.send("42[\"instruments_list\"]")
            ws.send("42[\"get_instruments\"]")
            emit("📋 طلب قائمة الأزواج")
        } catch (_: Exception) {}
    }

    private fun parseInstruments(text: String) {
        try {
            val idx = text.indexOf("[")
            if (idx == -1) return
            val arr = JSONArray(text.substring(idx))
            if (arr.length() < 2) return

            val payload = arr.opt(1) ?: return
            val list = mutableListOf<Instrument>()

            if (payload is JSONArray) {
                for (i in 0 until payload.length()) {
                    val obj = payload.optJSONObject(i) ?: continue
                    val id = obj.optInt("id", -1)
                    val ticker = obj.optString("ticker",
                        obj.optString("asset", ""))
                    val name = obj.optString("name", ticker)
                    val isOtc = ticker.contains("_otc", true)

                    if (id > 0 && ticker.isNotEmpty()) {
                        val inst = Instrument(id, ticker, name, isOtc)
                        instruments[ticker] = inst
                        list.add(inst)
                    }
                }
            }

            if (list.isNotEmpty()) {
                emit("✅ تم تحميل ${list.size} زوج من Quotex")
                handler.post { onInstrumentsLoaded?.invoke(list) }
            } else {
                emit("⚠️ لم يتم استلام قائمة الأزواج — استخدام الافتراضي")
                handler.post {
                    onInstrumentsLoaded?.invoke(getDefaultOtcList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseInstruments: ${e.message}")
            handler.post {
                onInstrumentsLoaded?.invoke(getDefaultOtcList())
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Subscribe
    // ═══════════════════════════════════════════
    private val defaultAssets = listOf(
        "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc",
        "AUDUSD_otc", "USDCAD_otc",
        "BTCUSD_otc", "ETHUSD_otc", "SOLUSD_otc",
        "XRPUSD_otc", "DOGEUSD_otc",
        "XAUUSD_otc", "XAGUSD_otc"
    )

    fun subscribeAsset(asset: String, period: Int = 60) {
        val ws = ws ?: return
        try {
            val payload = JSONObject().apply {
                put("asset", asset)
                put("period", period)
            }
            ws.send("42[\"subscribe_candles\",$payload]")
            emit("📊 اشتراك في $asset")
        } catch (_: Exception) {}
    }

    private fun subscribeDefaultAssets(ws: WebSocket) {
        defaultAssets.forEachIndexed { i, asset ->
            handler.postDelayed({
                try {
                    val payload = JSONObject().apply {
                        put("asset", asset)
                        put("period", 60)
                    }
                    ws.send("42[\"subscribe_candles\",$payload]")
                } catch (_: Exception) {}
            }, (i * 250).toLong())
        }
        emit("📊 اشتراك في ${defaultAssets.size} زوج OTC")
    }

    // ═══════════════════════════════════════════
    //  Trade (Demo Only)
    // ═══════════════════════════════════════════
    /**
     * @param asset اسم الزوج (مثلاً "EURUSD_otc")
     * @param amount المبلغ بالدولار
     * @param direction "call" للشراء أو "put" للبيع
     * @param durationSec مدة الصفقة بالثواني
     */
    fun executeTrade(
        asset: String,
        amount: Double,
        direction: String,
        durationSec: Int
    ) {
        val ws = ws ?: run {
            handler.post { onTradeResult?.invoke(false, "❌ لا يوجد اتصال") }
            return
        }
        if (IS_DEMO != 1) {
            handler.post { onTradeResult?.invoke(false, "❌ Live mode disabled") }
            return
        }

        try {
            val inst = instruments[asset]
            val payload = JSONObject().apply {
                put("asset", asset)
                put("amount", amount)
                put("direction", direction)
                put("duration", durationSec)
                put("isDemo", IS_DEMO)
                put("optionType", 100)
                if (inst != null) put("assetId", inst.id)
            }

            ws.send("42[\"place_order\",$payload]")
            emit("📤 صفقة: $direction $asset \$$amount (${durationSec}s)")
            handler.post { onTradeResult?.invoke(true, "⏳ جاري التنفيذ...") }
        } catch (e: Exception) {
            handler.post { onTradeResult?.invoke(false, "❌ ${e.message}") }
        }
    }

    // ═══════════════════════════════════════════
    //  Parsing
    // ═══════════════════════════════════════════
    private fun parsePrices(text: String) {
        try {
            val idx = text.indexOf("[")
            if (idx == -1) return
            val arr = JSONArray(text.substring(idx))
            if (arr.length() < 2) return

            when (val data = arr.opt(1)) {
                is JSONArray -> for (i in 0 until data.length()) {
                    val c = data.optJSONObject(i) ?: continue
                    val asset = c.optString("asset",
                        c.optString("ticker", ""))
                    val close = c.optDouble("close",
                        c.optDouble("price", Double.NaN))
                    if (asset.isNotEmpty() && !close.isNaN()) {
                        prices[asset] = close
                        handler.post { onPrice?.invoke(asset, close) }
                    }
                }
                is JSONObject -> {
                    val asset = data.optString("asset",
                        data.optString("ticker", ""))
                    val close = data.optDouble("close",
                        data.optDouble("price", Double.NaN))
                    if (asset.isNotEmpty() && !close.isNaN()) {
                        prices[asset] = close
                        handler.post { onPrice?.invoke(asset, close) }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseBalance(text: String) {
        try {
            val obj = JSONObject(text.substring(text.indexOf("{")))
            val bal = obj.optDouble("balance", Double.NaN)
            if (!bal.isNaN()) {
                handler.post { onBalanceUpdate?.invoke(bal) }
            }
        } catch (_: Exception) {}
    }

    private fun parseTradeResult(text: String) {
        val success = text.contains("success", true)
        handler.post {
            onTradeResult?.invoke(
                success,
                if (success) "✅ تم تنفيذ الصفقة" else "❌ فشلت الصفقة"
            )
        }
    }
}
