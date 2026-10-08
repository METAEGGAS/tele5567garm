package com.sys.update2

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PricesActivity : AppCompatActivity() {

    private val TAG = "PricesActivity"
    private lateinit var statusView: TextView
    private lateinit var pricesView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(40, 80, 40, 20)
        }
        pricesView = TextView(this).apply {
            textSize = 14f
            setPadding(40, 20, 40, 40)
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(statusView)
            addView(pricesView)
        }
        setContentView(ScrollView(this).apply { addView(layout) })

        val ssid = getSharedPreferences("qtx", MODE_PRIVATE)
            .getString("ssid", null)

        if (ssid.isNullOrBlank()) {
            statusView.text = "❌ لا يوجد SSID — أعد تسجيل الدخول"
            return
        }

        statusView.text = "🔄 جاري الاتصال بـ Quotex..."
        QuotexSocket.connect(
            ssid = ssid,
            onStatus = { msg -> runOnUiThread { statusView.text = msg } },
            onPrice = { sym, price ->
                runOnUiThread {
                    pricesView.text = "$sym → $price\n${pricesView.text}"
                }
            },
            onError = { err ->
                Log.e(TAG, "socket err: $err")
                runOnUiThread { statusView.text = "❌ $err" }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        QuotexSocket.disconnect()
    }
}
