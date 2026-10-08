package com.sys.update2

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * QuotexSocket — يتعامل مع Socket.IO لـ Quotex
 * - المصادقة (authorization)
 * - استقبال الأسعار (candles/quotes)
 * - تنفيذ صفقات (buy/sell) — Demo فقط
 */
object QuotexSocket {

    private const val TAG = "QuotexSocket"

    // ⚠️ إعداد إجباري: Demo فقط
    private const val IS_DEMO = 1

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var ssid: String? = null
    private var handler = Handler(Looper.getMainLooper())

    // Callbacks
    var onStatus: ((String) -> Unit)? = null
    var onPrice: ((String, Double) -> Unit)? = null
    var onTradeResult: ((Boolean, String) -> Unit)? = null
    var onBalanceUpdate: ((Double) -> Unit)? = null

    // أحدث الأسعار
    private val prices = HashMap<String, Double>()

    fun getPrice(asset: String): Double? = prices[asset]

    /**
     * الاتصال بـ Quotex
     */
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
                emit("🔌 متصل — handshake")
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
    //  معالجة الرسائل
    // ═══════════════════════════════════════════
    private fun handleMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "← ${text.take(250)}")

        when {
            text.startsWith("0{") -> webSocket.send("40")

            text == "40" -> {
                emit("✅ القناة مفتوحة — إرسال التوكن")
                sendAuthorization(webSocket)
            }

            text == "2" -> webSocket.send("3")   // ping → pong

            // نتيجة مصادقة
            text.contains("authorization") -> {
                emit("🎉 تم الدخول — جاري جلب الرصيد")
                subscribeAllAssets(webSocket)
            }

            // الرصيد
            text.contains("\"balance\"") -> parseBalance(text)

            // رسالة خطأ
            text.startsWith("44") || text.contains("error", true) -> {
                emit("⚠️ السيرفر: ${text.take(120)}")
            }

            // بيانات الأسعار
            text.contains("candles") || text.contains("quotes")
                || text.contains("instruments") -> parsePrices(text)

            // نتيجة الصفقة
            text.contains("trade") && (text.contains("success") || text.contains("error")) -> {
                parseTradeResult(text)
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
    //  اشتراك
    // ═══════════════════════════════════════════
    private val defaultAssets = listOf(
        "EURUSD", "GBPUSD", "USDJPY", "AUDUSD",
        "BTCUSD", "ETHUSD", "SOLUSD", "XRPUSD"
    )

    fun subscribeAsset(asset: String, period: Int = 60) {
        val ws = ws ?: return
        val payload = JSONObject().apply {
            put("asset", asset)
            put("period", period)
        }
        ws.send("42[\"subscribe_candles\",$payload]")
        emit("📊 اشتراك في $asset")
    }

    private fun subscribeAllAssets(ws: WebSocket) {
        defaultAssets.forEachIndexed { i, asset ->
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val payload = JSONObject().apply {
                        put("asset", asset)
                        put("period", 60)
                    }
                    ws.send("42[\"subscribe_candles\",$payload]")
                } catch (_: Exception) {}
            }, (i * 300).toLong())
        }
        emit("📊 جاري الاشتراك في ${defaultAssets.size} عملة")
    }

    // ═══════════════════════════════════════════
    //  تنفيذ صفقة (Demo)
    // ═══════════════════════════════════════════
    /**
     * @param asset اسم العملة (مثلاً "BTCUSD")
     * @param amount المبلغ ($1 - $10000)
     * @param direction "call" (شراء) أو "put" (بيع)
     * @param durationSec مدة الصفقة بالثواني (60 = دقيقة)
     */
    fun executeTrade(
        asset: String,
        amount: Double,
        direction: String,  // "call" أو "put"
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
            // 1) حدد الـ asset ID — يحتاج يكون معروف
            val assetId = assetIdFor(asset)

            val payload = JSONObject().apply {
                put("asset", asset)
                put("amount", amount)
                put("direction", direction)
                put("duration", durationSec)
                put("isDemo", IS_DEMO)
                put("optionType", 100)    // 100 = binary
            }

            // رسالة place_order (Socket.IO event)
            val event = JSONObject().apply {
                put("name", "place_order")
                put("msg", payload)
            }
            ws.send("42[\"place_order\",$payload]")

            emit("📤 تم إرسال: $direction $asset \$$amount ${durationSec}s")
            handler.post { onTradeResult?.invoke(true, "✅ تم إرسال الصفقة") }
        } catch (e: Exception) {
            Log.e(TAG, "trade err: ${e.message}")
            handler.post { onTradeResult?.invoke(false, "❌ ${e.message}") }
        }
    }

    /**
     * يستخدم asset ID من خريطة داخلية.
     * Quotex بتستخدم أرقام مش أسماء.
     */
    private fun assetIdFor(asset: String): Int {
        return when (asset) {
            "EURUSD" -> 1
            "GBPUSD" -> 2
            "USDJPY" -> 3
            "BTCUSD" -> 86
            "ETHUSD" -> 87
            "SOLUSD" -> 88
            "XRPUSD" -> 89
            else -> 0
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
                    val asset = c.optString("asset", "")
                    val close = c.optDouble("close", Double.NaN)
                    if (asset.isNotEmpty() && !close.isNaN()) {
                        prices[asset] = close
                        handler.post { onPrice?.invoke(asset, close) }
                    }
                }
                is JSONObject -> {
                    val asset = data.optString("asset", "")
                    val close = data.optDouble("close", Double.NaN)
                    if (asset.isNotEmpty() && !close.isNaN()) {
                        prices[asset] = close
                        handler.post { onPrice?.invoke(asset, close) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parsePrices: ${e.message}")
        }
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
            onTradeResult?.invoke(success, if (success) "✅ تم تنفيذ الصفقة" else "❌ فشلت الصفقة")
        }
    }
}
