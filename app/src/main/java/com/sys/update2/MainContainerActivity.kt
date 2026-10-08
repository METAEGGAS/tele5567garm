package com.sys.update2

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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
            isFocusable = true
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

    // ═══════════════════════════════════════════
    //  Login Screen
    // ═══════════════════════════════════════════
    @SuppressLint("SetJavaScriptEnabled")
    private fun buildLoginScreen() {
        loginScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // شريط يدوي لـ SSID (احتياطي)
        val manualBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#161B22"))
            setPadding(16, 12, 16, 12)
        }

        val ssidInput = EditText(this).apply {
            hint = "أدخل SSID يدوياً..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 12f
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(12, 8, 12, 8)
        }

        val manualBtn = Button(this).apply {
            text = "استخدام"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#1F6FEB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val v = ssidInput.text.toString().trim()
                if (v.isNotBlank()) {
                    saveSSID(v)
                    Toast.makeText(this@MainContainerActivity,
                        "✅ تم استخدام SSID يدوي", Toast.LENGTH_SHORT).show()
                }
            }
        }

        manualBar.addView(ssidInput, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginEnd = 8 })
        manualBar.addView(manualBtn)

        loginScreen.addView(manualBar)

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

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onSSID(ssid: String) {
                Log.d(TAG, "SSID extracted: ${ssid.take(20)}...")
                runOnUiThread {
                    if (ssid.isNotBlank()) {
                        saveSSID(ssid)
                        Toast.makeText(
                            this@MainContainerActivity,
                            "✅ تم استلام الجلسة",
                            Toast.LENGTH_SHORT
                        ).show()
                        switchTo(1)
                        connectQuotex()
                    }
                }
            }
        }, "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("qxbroker.com") == true) {
                    injectScript()
                }
            }
        }

        webView.loadUrl("https://qxbroker.com/ar/sign-in")

        loginScreen.addView(webView)
    }

    private fun saveSSID(ssid: String) {
        getSharedPreferences("qtx", MODE_PRIVATE)
            .edit()
            .putString("ssid", ssid)
            .apply()
    }

    // ═══════════════════════════════════════════
    //  Inject JS — يبحث عن SSID من أي مكان
    // ═══════════════════════════════════════════
    private fun injectScript() {
        val js = """
            (function() {
              if (window.__qtxHooked) return;
              window.__qtxHooked = true;

              function grab() {
                try {
                  var c = document.cookie || '';

                  // نمط 1: ssid
                  var m1 = c.match(/(?:^|;\s*)ssid=([^;]+)/);
                  if (m1 && m1[1] && m1[1].length > 20) {
                    window.AndroidBridge.onSSID(decodeURIComponent(m1[1]));
                    return true;
                  }

                  // نمط 2: session
                  var m2 = c.match(/(?:^|;\s*)session=([^;]+)/);
                  if (m2 && m2[1] && m2[1].length > 20) {
                    window.AndroidBridge.onSSID(decodeURIComponent(m2[1]));
                    return true;
                  }

                  // نمط 3: token
                  var m3 = c.match(/(?:^|;\s*)token=([^;]+)/);
                  if (m3 && m3[1] && m3[1].length > 20) {
                    window.AndroidBridge.onSSID(decodeURIComponent(m3[1]));
                    return true;
                  }
                } catch (e) {}

                // localStorage
                try {
                  for (var i = 0; i < localStorage.length; i++) {
                    var k = localStorage.key(i);
                    if (!k) continue;
                    var lk = k.toLowerCase();
                    if (lk.indexOf('ssid') > -1 ||
                        lk.indexOf('session') > -1 ||
                        lk.indexOf('token') > -1) {
                      var v = localStorage.getItem(k);
                      if (v && v.length > 20) {
                        window.AndroidBridge.onSSID(v);
                        return true;
                      }
                    }
                  }
                } catch (e) {}

                return false;
              }

              setInterval(grab, 700);
              document.addEventListener('submit', function() {
                setTimeout(grab, 400);
              }, true);
              document.addEventListener('click', function(e) {
                var t = e.target.closest('button, [type="submit"], [role="button"], a');
                if (t) setTimeout(grab, 400);
              }, true);
              grab();
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    // ═══════════════════════════════════════════
    //  Trade Screen
    // ═══════════════════════════════════════════
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

        assetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val list = QuotexSocket.getInstrumentList()
                    .ifEmpty { QuotexSocket.getDefaultOtcList() }
                if (position < list.size) {
                    val price = QuotexSocket.getPrice(list[position].ticker)
                    if (price != null) {
                        priceText.text = String.format("%.5f", price)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        populateAssets()
    }

    // ═══════════════════════════════════════════
    //  Status Screen
    // ═══════════════════════════════════════════
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

        val clearBtn = Button(this).apply {
            text = "🗑 مسح السجل"
            setBackgroundColor(Color.parseColor("#30363D"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                orders.clear()
                renderStatus()
                renderOrdersList()
                Toast.makeText(this@MainContainerActivity, "تم المسح", Toast.LENGTH_SHORT).show()
            }
        }
        statusScreen.addView(clearBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 30 })
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
            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(o.time))
            sb.append("$icon ${o.asset}  ")
            sb.append("${if (o.direction == "call") "شراء" else "بيع"}  ")
            sb.append("\$${o.amount}  ")
            sb.append("${o.duration}s  ")
            sb.append("[$timeStr]\n\n")
        }
        statusText.text = sb.toString()
    }

    // ═══════════════════════════════════════════
    //  Orders Screen
    // ═══════════════════════════════════════════
    private fun buildOrdersScreen() {
        ordersScreen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(16, 16, 16, 16)
        }

        ordersScreen.addView(TextView(this).apply {
            text = "📊 سجل الطلبات الكامل"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        })

        historyList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(historyList)
        }
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
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            val iconColor = when (o.status) {
                "success" -> "#2ECC71"
                "failed" -> "#E74C3C"
                else -> "#F0B27A"
            }

            item.addView(TextView(this).apply {
                text = "${o.asset}  ·  ${if (o.direction == "call") "🟢 شراء" else "🔴 بيع"}"
                setTextColor(Color.WHITE)
                textSize = 15f
            })

            item.addView(TextView(this).apply {
                text = "\$${o.amount}   ·   ${o.duration}s   ·   ${o.status.uppercase()}"
                setTextColor(Color.parseColor(iconColor))
                textSize = 13f
                setPadding(0, 6, 0, 0)
            })

            val timeStr = java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.US).format(java.util.Date(o.time))
            item.addView(TextView(this).apply {
                text = timeStr
                setTextColor(Color.parseColor("#8B949E"))
                textSize = 11f
            })

            historyList.addView(item)
        }
    }

    // ═══════════════════════════════════════════
    //  Connect Quotex
    // ═══════════════════════════════════════════
    private fun connectQuotex() {
        val ssid = getSharedPreferences("qtx", MODE_PRIVATE)
            .getString("ssid", null)

        if (ssid.isNullOrBlank()) {
            statusBar.text = "🔐 سجل دخول أولاً"
            return
        }

        QuotexSocket.onStatus = { msg ->
            runOnUiThread { statusBar.text = msg }
        }

        QuotexSocket.onPrice = { asset, price ->
            runOnUiThread {
                val list = QuotexSocket.getInstrumentList()
                    .ifEmpty { QuotexSocket.getDefaultOtcList() }
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
                    orders[idx] = last.copy(
                        status = if (ok) "success" else "failed"
                    )
                    renderStatus()
                    renderOrdersList()
                }
            }
        }

        QuotexSocket.onInstrumentsLoaded = { _ ->
            runOnUiThread { populateAssets() }
        }

        QuotexSocket.connect(ssid)
    }

    private fun populateAssets() {
        val list = QuotexSocket.getInstrumentList()
            .ifEmpty { QuotexSocket.getDefaultOtcList() }

        val labels = list.map { it.ticker }
        assetSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, labels
        )
    }

    private fun executeTrade(direction: String, amount: Double) {
        val list = QuotexSocket.getInstrumentList()
            .ifEmpty { QuotexSocket.getDefaultOtcList() }

        if (assetSpinner.selectedItemPosition >= list.size) {
            Toast.makeText(this, "اختر زوجاً", Toast.LENGTH_SHORT).show()
            return
        }

        val asset = list[assetSpinner.selectedItemPosition].ticker
        val duration = 60

        val order = Order(
            id = System.currentTimeMillis().toString(),
            asset = asset,
            direction = direction,
            amount = amount,
            duration = duration,
            time = System.currentTimeMillis(),
            status = "pending"
        )
        orders.add(order)

        QuotexSocket.executeTrade(asset, amount, direction, duration)

        renderStatus()
        renderOrdersList()

        Toast.makeText(
            this,
            "📤 ${if (direction == "call") "شراء" else "بيع"} $asset \$$amount",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onBackPressed() {
        if (selectedScreen != 0) {
            switchTo(0)
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        QuotexSocket.disconnect()
    }
}
