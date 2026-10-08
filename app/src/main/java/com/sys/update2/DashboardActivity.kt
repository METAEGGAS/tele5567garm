package com.sys.update2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * DashboardActivity — الواجهة الرئيسية
 * - شارت بسيط للأسعار
 * - قائمة الأزواج
 * - تنفيذ صفقات Demo
 */
class DashboardActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    // Views
    private lateinit var statusText: TextView
    private lateinit var balanceText: TextView
    private lateinit var chartView: ChartView
    private lateinit var priceText: TextView
    private lateinit var assetSpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var durationSpinner: Spinner
    private lateinit var buyButton: Button
    private lateinit var sellButton: Button
    private lateinit var historyText: TextView

    // State
    private var selectedAsset = "EURUSD_otc"
    private var currentPrice = 0.0
    private val historyLines = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 60, 24, 24)
        }

        // ─── Header ───
        statusText = TextView(this).apply {
            text = "🔄 جاري الاتصال..."
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }
        root.addView(statusText)

        balanceText = TextView(this).apply {
            text = "💰 الرصيد: --"
            textSize = 16f
            setTextColor(0xFF2ECC71.toInt())
            setPadding(0, 0, 0, 16)
        }
        root.addView(balanceText)

        // ─── اختيار الزوج ───
        root.addView(TextView(this).apply {
            text = "الزوج:"
            textSize = 14f
        })

        assetSpinner = Spinner(this)
        root.addView(assetSpinner, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 12 })

        // ─── السعر الحالي ───
        priceText = TextView(this).apply {
            text = "--"
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        root.addView(priceText)

        // ─── الشارت ───
        chartView = ChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300
            )
        }
        root.addView(chartView)

        // ─── المبلغ ───
        root.addView(TextView(this).apply {
            text = "المبلغ (\$):"
            textSize = 14f
            setPadding(0, 16, 0, 4)
        })

        amountInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "1.00"
            setText("1")
        }
        root.addView(amountInput)

        // ─── المدة ───
        root.addView(TextView(this).apply {
            text = "المدة:"
            textSize = 14f
            setPadding(0, 12, 0, 4)
        })

        durationSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@DashboardActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("30 ثانية (30)", "1 دقيقة (60)", "2 دقيقة (120)", "5 دقائق (300)")
            )
        }
        root.addView(durationSpinner)

        // ─── أزرار ───
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
            weightSum = 2f
        }

        buyButton = Button(this).apply {
            text = "🟢 شراء"
            setBackgroundColor(0xFF2ECC71.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setOnClickListener { executeTrade("call") }
        }
        buttonRow.addView(buyButton, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginEnd = 8 })

        sellButton = Button(this).apply {
            text = "🔴 بيع"
            setBackgroundColor(0xFFE74C3C.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setOnClickListener { executeTrade("put") }
        }
        buttonRow.addView(sellButton, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { marginStart = 8 })

        root.addView(buttonRow)

        // ─── السجل ───
        root.addView(TextView(this).apply {
            text = "📜 آخر العمليات:"
            textSize = 14f
            setPadding(0, 24, 0, 6)
        })

        historyText = TextView(this).apply {
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        root.addView(historyText)

        setContentView(ScrollView(this).apply { addView(root) })

        // ─── Spinner listener ───
        assetSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: android.view.View?,
                position: Int, id: Long
            ) {
                val list = QuotexSocket.getInstrumentList()
                    .ifEmpty { QuotexSocket.getDefaultOtcList() }
                if (position < list.size) {
                    selectedAsset = list[position].ticker
                    currentPrice = QuotexSocket.getPrice(selectedAsset) ?: 0.0
                    updatePrice(currentPrice)
                    chartView.setData(selectedAsset, emptyList())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ─── ابدأ الاتصال ───
        connectQuotex()
        populateAssets()
    }

    private fun connectQuotex() {
        val ssid = getSharedPreferences("qtx", MODE_PRIVATE)
            .getString("ssid", null)

        if (ssid.isNullOrBlank()) {
            statusText.text = "❌ مفيش SSID — أعد تسجيل الدخول"
            return
        }

        QuotexSocket.onStatus = { msg ->
            handler.post { statusText.text = msg }
        }

        QuotexSocket.onPrice = { asset, price ->
            handler.post {
                if (asset == selectedAsset) {
                    currentPrice = price
                    updatePrice(price)
                    chartView.pushPoint(price)
                }
            }
        }

        QuotexSocket.onBalanceUpdate = { bal ->
            handler.post {
                balanceText.text = "💰 الرصيد: \$${String.format("%.2f", bal)}"
            }
        }

        QuotexSocket.onTradeResult = { ok, msg ->
            handler.post {
                val icon = if (ok) "✅" else "❌"
                addHistory("$icon $msg")
            }
        }

        QuotexSocket.onInstrumentsLoaded = { list ->
            handler.post { populateAssets() }
        }

        QuotexSocket.connect(ssid)
    }

    private fun populateAssets() {
        val list = QuotexSocket.getInstrumentList()
            .ifEmpty { QuotexSocket.getDefaultOtcList() }

        val labels = list.map { "${it.name}  (${it.ticker})" }
        assetSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
    }

    private fun updatePrice(price: Double) {
        priceText.text = when {
            price >= 1000 -> String.format("$%,.2f", price)
            price >= 1 -> String.format("$%.4f", price)
            else -> String.format("$%.6f", price)
        }
    }

    private fun executeTrade(direction: String) {
        val amountStr = amountInput.text.toString()
        val amount = amountStr.toDoubleOrNull() ?: 1.0

        if (amount < 1) {
            Toast.makeText(this, "الحد الأدنى \$1", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount > 10000) {
            Toast.makeText(this, "الحد الأقصى \$10000", Toast.LENGTH_SHORT).show()
            return
        }

        val durationIdx = durationSpinner.selectedItemPosition
        val duration = listOf(30, 60, 120, 300)[durationIdx]

        val dirLabel = if (direction == "call") "شراء" else "بيع"

        // ⚠️ تأكيد
        android.app.AlertDialog.Builder(this)
            .setTitle("تأكيد الصفقة")
            .setMessage(
                "الزوج: $selectedAsset\n" +
                "الاتجاه: $dirLabel\n" +
                "المبلغ: \$${String.format("%.2f", amount)}\n" +
                "المدة: $duration ثانية\n\n" +
                "⚠️ صفقة Demo (تجريبية)"
            )
            .setPositiveButton("تنفيذ") { _, _ ->
                QuotexSocket.executeTrade(
                    selectedAsset, amount, direction, duration
                )
                addHistory("📤 $dirLabel $selectedAsset \$$amount ${duration}s")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun addHistory(line: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss",
            java.util.Locale.US).format(java.util.Date())
        historyLines.addFirst("[$time] $line")
        while (historyLines.size > 20) historyLines.removeLast()
        historyText.text = historyLines.joinToString("\n")
    }

    override fun onDestroy() {
        super.onDestroy()
        QuotexSocket.disconnect()
    }
}
