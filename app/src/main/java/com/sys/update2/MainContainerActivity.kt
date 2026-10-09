package com.sys.update2

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainContainerActivity : AppCompatActivity() {

    private val TAG = "MainContainer"
    private lateinit var contentArea: FrameLayout
    private lateinit var statusBar: TextView

    private lateinit var loginScreen: LinearLayout
    private lateinit var tradeScreen: LinearLayout
    private lateinit var ordersScreen: LinearLayout
    private lateinit var statusScreen: LinearLayout

    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var balanceText: TextView
    private lateinit var historyList: LinearLayout
    private lateinit var priceText: TextView
    private lateinit var assetSpinner: Spinner

    private var selectedScreen = 0
    private val orders = mutableListOf<Order>()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class Order(
        val id: String,
        val asset: String,
        val direction: String,
        val amount: Double,
        val duration: Int,
        val time: Long,
        var status: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusBar = TextView(this).apply {
            text = "🔌 جاري التحميل..."
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(20, 60, 20, 12)
        }
        root.addView(statusBar)

        contentArea = FrameLayout(this)
        root.addView(contentArea, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(buildBottomBar())

        setContentView(root)

        buildLoginScreen()
        buildTradeScreen()
        buildStatusScreen()
        buildOrdersScreen()

        switchTo(0)
    }

    private fun buildBottomBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(0, 8, 0, 24)
            weightSum = 4f
        }
        bar.addView(bottomItem("🔐", "تسجيل", 0), weight())
        bar.addView(bottomItem("💹", "صفقات", 1), weight())
        bar.addView(bottomItem("📋", "حالة", 2), weight())
        bar.addView(bottomItem("📊", "الطلبات", 3), weight())

        val topLine = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#30363D"))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(topLine, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2
            ))
            addView(bar)
        }
    }

    private fun weight() = LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
    )

    private fun bottomItem(icon: String, label: String, index: Int): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
            isClickable = true
            setOnClickListener { switchTo(index) }
        }
        item.addView(TextView(this).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
        })
        item.addView(TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.parseColor("#8B949E"))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        })
        return item
    }

    private fun switchTo(index: Int) {
        selectedScreen = index
        contentArea.removeAllViews()
        when (index) {
            0 -> contentArea.addView(loginScreen)
            1 -> contentArea.addView(tradeScreen)
            2 -> contentArea.addView(statusScreen)
            3 -> contentArea.addView(ordersScreen)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildLoginScreen() {
        loginScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val manualBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(16, 12, 16, 12)
        }

        val tokenInput = EditText(this).apply {
            hint = "Token..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 11f
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(12, 8, 12, 8)
        }
        manualBar.addView(tokenInput)

        val manualBtn = Button(this).apply {
            text = "استخدام"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#1F6FEB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val t = tokenInput.text.toString().trim()
                if (t.isNotBlank()) {
                    getSharedPreferences("qtx", MODE_PRIVATE).edit()
                        .putString("token", t).apply()
                    Toast.makeText(this@MainContainerActivity,
                        "✅ تم الحفظ", Toast.LENGTH_SHORT).show()
                    switchTo(1)
                    connectQuotex()
                }
            }
        }
        manualBar.addView(manualBtn)
        loginScreen.addView(manualBar)

        val digestBtn = Button(this).apply {
            text = "🌐 جلب من digest"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#238636"))
            setTextColor(Color.WHITE)
            setOnClickListener { fetchDigestAndConnect() }
        }
        loginScreen.addView(digestBtn)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                userAgentString = "Mozilla/5.0 (Linux; Android 13) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, true)
        cm.flush()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page: $url")
                if (url?.contains("qxbroker.com") == true) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        fetchDigestAndConnect()
                    }, 3000)
                }
            }
        }

        webView.loadUrl("https://qxbroker.com/ar/sign-in")
        loginScreen.addView(webView)
    }

    private fun fetchDigestAndConnect() {
        val cm = CookieManager.getInstance()
        cm.flush()
        val cookies = cm.getCookie("https://qxbroker.com") ?: ""
        val ua = webView.settings.userAgentString ?: ""

        if (cookies.isBlank()) {
            statusBar.text = "⚠️ مفيش كوكيز"
            return
        }

        Thread {
            try {
                val req = Request.Builder()
                    .url("https://qxbroker.com/api/v1/cabinets/digest")
                    .header("Cookie", cookies)
                    .header("User-Agent", ua)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Referer", "https://qxbroker.com/ar/trade")
                    .header("Origin", "https://qxbroker.com")
                    .get()
                    .build()

                val resp = http.newCall(req).execute()
                val code = resp.code
                val body = resp.body?.string() ?: ""
                resp.close()

                Log.d(TAG, "Digest HTTP $code")

                if (code != 200) {
                    runOnUiThread { statusBar.text = "⚠️ HTTP $code" }
                    return@Thread
                }

                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val token = data.optString("token", "")
                val csrf = data.optString("csrf", "")

                if (token.isNotBlank()) {
                    getSharedPreferences("qtx", MODE_PRIVATE).edit()
                        .putString("token", token)
                        .putString("csrf", csrf)
                        .putString("cookie", cookies)
                        .putString("ua", ua)
                        .apply()

                    runOnUiThread {
                        Toast.makeText(this, "✅ Token OK", Toast.LENGTH_SHORT).show()
                        statusBar.text = "✅ Token: ${token.take(20)}..."
                        switchTo(1)
                        connectQuotex()
                    }
                } else {
                    runOnUiThread { statusBar.text = "❌ مفيش token" }
                }

            } catch (e: Exception) {
                Log.e(TAG, "digest err: ${e.message}", e)
                runOnUiThread { statusBar.text = "❌ ${e.message?.take(40)}" }
            }
        }.start()
    }

    private fun buildTradeScreen() {
        tradeScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(24, 24, 24, 24)
        }

        balanceText = TextView(this).apply {
            text = "💰 الرصيد: --"
            textSize = 16f
            setTextColor(Color.parseColor("#2ECC71"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        tradeScreen.addView(balanceText)

        tradeScreen.addView(TextView(this).apply {
            text = "الزوج:"
            setTextColor(Color.WHITE)
            textSize = 14f
        })

        assetSpinner = Spinner(this)
        tradeScreen.addView(assetSpinner, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        priceText = TextView(this).apply {
            text = "--"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }
        tradeScreen.addView(priceText)

        tradeScreen.addView(TextView(this).apply {
            text = "المبلغ (\$):"
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 12, 0, 4)
        })

        val amountInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "1"
            setText("1")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        tradeScreen.addView(amountInput)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            setPadding(0, 24, 0, 0)
        }

        val buyBtn = Button(this).apply {
            text = "🟢 شراء"
            setBackgroundColor(Color.parseColor("#2ECC71"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setOnClickListener {
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 1.0
                executeTrade("call", amount)
            }
        }
        row.addView(buyBtn, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginEnd = 8 })

        val sellBtn = Button(this).apply {
            text = "🔴 بيع"
            setBackgroundColor(Color.parseColor("#E74C3C"))
            setTextColor(Color.WHITE)
            textSize = 18f
            setOnClickListener {
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 1.0
                executeTrade("put", amount)
            }
        }
        row.addView(sellBtn, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginStart = 8 })

        tradeScreen.addView(row)

        populateAssets()
    }

    private fun buildStatusScreen() {
        statusScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(24, 24, 24, 24)
        }
        statusScreen.addView(TextView(this).apply {
            text = "📋 حالة الطلبات"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        })
        statusText = TextView(this).apply {
            text = "لا توجد طلبات بعد"
            textSize = 13f
            setTextColor(Color.parseColor("#8B949E"))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        statusScreen.addView(statusText)
    }

    private fun renderStatus() {
        if (orders.isEmpty()) {
            statusText.text = "لا توجد طلبات بعد"
            return
        }
        val sb = StringBuilder()
        orders.takeLast(20).reversed().forEach { o ->
            val icon = when (o.status) {
                "success" -> "✅"
                "failed" -> "❌"
                else -> "⏳"
            }
            sb.append("$icon ${o.asset} ${if (o.direction == "call") "شراء" else "بيع"} \$${o.amount}\n\n")
        }
        statusText.text = sb.toString()
    }

    private fun buildOrdersScreen() {
        ordersScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(16, 16, 16, 16)
        }
        ordersScreen.addView(TextView(this).apply {
            text = "📊 سجل الطلبات"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        })
        historyList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply { addView(historyList) }
        ordersScreen.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
    }

    private fun renderOrdersList() {
        historyList.removeAllViews()
        if (orders.isEmpty()) {
            historyList.addView(TextView(this).apply {
                text = "لا توجد طلبات"
                setTextColor(Color.parseColor("#8B949E"))
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 0)
            })
            return
        }
        orders.reversed().forEach { o ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#161B22"))
                setPadding(16, 16, 16, 16)
            }
            item.addView(TextView(this).apply {
                text = "${o.asset} · ${if (o.direction == "call") "شراء" else "بيع"}"
                setTextColor(Color.WHITE)
                textSize = 15f
            })
            historyList.addView(item)
        }
    }

    private fun connectQuotex() {
        val prefs = getSharedPreferences("qtx", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val cookie = prefs.getString("cookie", "") ?: ""

        if (token.isNullOrBlank()) {
            statusBar.text = "🔐 سجل دخول أولاً"
            return
        }

        QuotexSocket.onStatus = { msg ->
            runOnUiThread { statusBar.text = msg }
        }
        QuotexSocket.onPrice = { asset, price ->
            runOnUiThread {
                val list = QuotexSocket.getDefaultOtcList()
                val pos = assetSpinner.selectedItemPosition
                if (pos in list.indices && list[pos].ticker == asset) {
                    priceText.text = String.format("%.5f", price)
                }
            }
        }
        QuotexSocket.onBalanceUpdate = { bal ->
            runOnUiThread {
                balanceText.text = "💰 الرصيد: \$${String.format("%.2f", bal)}"
            }
        }
        QuotexSocket.onTradeResult = { ok, msg ->
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                statusBar.text = msg
                if (orders.isNotEmpty()) {
                    val last = orders.last()
                    val idx = orders.indexOf(last)
                    orders[idx] = last.copy(status = if (ok) "success" else "failed")
                    renderStatus()
                    renderOrdersList()
                }
            }
        }

        QuotexSocket.attach(webView, token, cookie)
    }

    private fun populateAssets() {
        val list = QuotexSocket.getDefaultOtcList()
        val labels = list.map { it.ticker }
        assetSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, labels
        )
    }

    private fun executeTrade(direction: String, amount: Double) {
        val list = QuotexSocket.getDefaultOtcList()
        if (assetSpinner.selectedItemPosition >= list.size) return
        val asset = list[assetSpinner.selectedItemPosition].ticker
        val order = Order(
            id = System.currentTimeMillis().toString(),
            asset = asset,
            direction = direction,
            amount = amount,
            duration = 60,
            time = System.currentTimeMillis(),
            status = "pending"
        )
        orders.add(order)
        QuotexSocket.executeTrade(asset, amount, direction, 60)
        renderStatus()
        renderOrdersList()
    }

    override fun onBackPressed() {
        if (selectedScreen != 0) switchTo(0)
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        QuotexSocket.disconnect()
    }
}
