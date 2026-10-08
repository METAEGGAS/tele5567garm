package com.sys.update2

import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object QuotexSocket {

    private const val TAG = "QuotexSocket"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var ssid: String? = null
    private var onStatus: ((String) -> Unit)? = null
    private var onPrice: ((String, String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun connect(
        ssid: String,
        onStatus: (String) -> Unit,
        onPrice: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        this.ssid = ssid
        this.onStatus = onStatus
        this.onPrice = onPrice
        this.onError = onError

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
                onStatus?.invoke("🔌 متصل — بدء handshake")
                webSocket.send("40")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS failed: ${t.message}", t)
                onError?.invoke("فشل: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS closed: $code / $reason")
                onStatus?.invoke("🔌 انقطع ($code)")
            }
        })
    }

    private fun handleMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "← ${text.take(200)}")

        when {
            text.startsWith("0{") -> webSocket.send("40")

            text == "40" -> {
                onStatus?.invoke("✅ القناة مفتوحة — إرسال SSID")
                val auth = """42["authorization",{"session":"${ssid}","isDemo":1,"tournamentId":0}]"""
                webSocket.send(auth)
            }

            text == "2" -> webSocket.send("3")

            text.startsWith("44") || text.contains("error", true) -> {
                onError?.invoke("السيرفر: ${text.take(120)}")
            }

            text.contains("candles") || text.contains("quotes") -> parsePrices(text)
        }
    }

    private fun parsePrices(text: String) {
        try {
            val idx = text.indexOf("[")
            if (idx == -1) return
            val arr = JSONArray(text.substring(idx))
            if (arr.length() < 2) return

            when (val data = arr.opt(1)) {
                is JSONArray -> for (i in 0 until data.length()) {
                    val c = data.optJSONObject(i) ?: continue
                    val symbol = c.optString("asset", "")
                    val close = c.optDouble("close", Double.NaN)
                    if (symbol.isNotEmpty() && !close.isNaN()) {
                        onPrice?.invoke(symbol, String.format("%.5f", close))
                    }
                }
                is JSONObject -> {
                    val symbol = data.optString("asset", "")
                    val close = data.optDouble("close", Double.NaN)
                    if (symbol.isNotEmpty() && !close.isNaN()) {
                        onPrice?.invoke(symbol, String.format("%.5f", close))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse err: ${e.message}")
        }
    }

    fun disconnect() {
        try { ws?.close(1000, "bye") } catch (_: Exception) {}
        ws = null
    }
}
