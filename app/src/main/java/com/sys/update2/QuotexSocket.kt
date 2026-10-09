package com.sys.update2

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * QuotexSocket — يستخدم WebView كـ proxy للاتصال بـ Quotex
 * بيحقن JavaScript في الصفحة، والصفحة تفتح WebSocket مباشرة
 */
object QuotexSocket {

    private const val TAG = "QuotexSocket"

    private var webView: WebView? = null
    private var isConnected = false

    var onStatus: ((String) -> Unit)? = null
    var onPrice: ((String, Double) -> Unit)? = null
    var onTradeResult: ((Boolean, String) -> Unit)? = null
    var onBalanceUpdate: ((Double) -> Unit)? = null

    /**
     * حقن JavaScript في WebView الموجود
     * الـ JS بيفتح WebSocket + يتصل بـ Quotex
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun attach(webView: WebView, token: String, cookie: String) {
        this.webView = webView

        // 1) سجّل Android Bridge
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onStatus(msg: String) {
                Log.d(TAG, "JS Status: $msg")
                onStatus?.invoke(msg)
            }

            @JavascriptInterface
            fun onPrice(asset: String, price: String) {
                val p = price.toDoubleOrNull() ?: return
                onPrice?.invoke(asset, p)
            }

            @JavascriptInterface
            fun onTradeResult(ok: String, msg: String) {
                onTradeResult?.invoke(ok == "1", msg)
            }

            @JavascriptInterface
            fun onBalance(bal: String) {
                val b = bal.toDoubleOrNull() ?: return
                onBalanceUpdate?.invoke(b)
            }
        }, "QTXBridge")

        // 2) الحقن — JavaScript يفتح WebSocket من داخل الصفحة
        val js = """
            (function() {
              if (window.__qtxSocket) return;
              
              window.__qtxSocket = true;
              var TOKEN = '$token';
              var socket = null;
              
              function log(msg) {
                window.QTXBridge.onStatus(msg);
              }
              
              function connect() {
                try {
                  log('🔌 محاولة الاتصال من داخل الصفحة...');
                  
                  socket = io('wss://ws2.qxbroker.com', {
                    transports: ['websocket'],
                    path: '/socket.io/',
                    forceNew: true,
                    reconnection: true
                  });
                  
                  socket.on('connect', function() {
                    log('✅ متصل بـ WebSocket');
                    
                    socket.emit('authorization', {
                      session: TOKEN,
                      isDemo: 1,
                      tournamentId: 0
                    });
                    log('📤 تم إرسال Token');
                  });
                  
                  socket.on('authorizationStatus', function(data) {
                    log('✅ مصادقة: ' + JSON.stringify(data));
                    socket.emit('instruments/list');
                    socket.emit('pending/list');
                    
                    ['EURUSD_otc', 'GBPUSD_otc', 'USDJPY_otc',
                     'BTCUSD_otc', 'ETHUSD_otc', 'XAUUSD_otc'].forEach(function(a) {
                      socket.emit('subscribe_candles', { asset: a, period: 60 });
                    });
                  });
                  
                  socket.on('quotes/stream', function(data) {
                    if (data && data.asset && data.price) {
                      window.QTXBridge.onPrice(data.asset, '' + data.price);
                    }
                  });
                  
                  socket.on('instruments/list', function(data) {
                    log('📋 قائمة الأدوات: ' + JSON.stringify(data).slice(0, 200));
                  });
                  
                  socket.on('orders/open', function(data) {
                    log('✅ صفقة مفتوحة: ' + JSON.stringify(data).slice(0, 200));
                    window.QTXBridge.onTradeResult('1', 'صفقة مفتوحة');
                  });
                  
                  socket.on('orders/close', function(data) {
                    var profit = (data && data.amount_profit) || 0;
                    log('📊 إغلاق: ربح ' + profit);
                    window.QTXBridge.onTradeResult(profit >= 0 ? '1' : '0',
                      (profit >= 0 ? 'ربح ' : 'خسارة ') + profit);
                  });
                  
                  socket.on('orders/error', function(data) {
                    log('❌ خطأ: ' + JSON.stringify(data).slice(0, 200));
                    window.QTXBridge.onTradeResult('0', 'فشلت الصفقة');
                  });
                  
                  socket.on('disconnect', function() {
                    log('🔌 انقطع');
                  });
                  
                  socket.on('error', function(err) {
                    log('❌ خطأ: ' + err);
                  });
                  
                  window.__socket = socket;
                  
                } catch (e) {
                  log('❌ استثناء: ' + e.message);
                }
              }
              
              // انتظر Socket.IO library
              if (typeof io === 'undefined') {
                log('⏳ انتظار Socket.IO...');
                var checkInterval = setInterval(function() {
                  if (typeof io !== 'undefined') {
                    clearInterval(checkInterval);
                    connect();
                  }
                }, 500);
              } else {
                connect();
              }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    fun executeTrade(asset: String, amount: Double, direction: String, duration: Int) {
        val w = webView ?: return
        val expiresAt = (System.currentTimeMillis() / 1000L) + duration
        val js = """
            (function() {
              if (window.__socket && window.__socket.connected) {
                window.__socket.emit('orders/open', {
                  asset: '$asset',
                  direction: '$direction',
                  amount: $amount,
                  expiresAt: $expiresAt,
                  optionType: 100
                });
              }
            })();
        """.trimIndent()
        w.evaluateJavascript(js, null)
    }

    fun disconnect() {
        val w = webView ?: return
        w.evaluateJavascript("if (window.__socket) window.__socket.disconnect();", null)
        isConnected = false
    }

    fun getDefaultOtcList() = listOf(
        "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc", "AUDUSD_otc",
        "BTCUSD_otc", "ETHUSD_otc", "SOLUSD_otc", "XAUUSD_otc"
    )

    fun getPrice(asset: String): Double? = null
    fun getInstrumentList(): List<Any> = emptyList()

    data class Instrument(
        val id: Int, val ticker: String, val name: String,
        val isOtc: Boolean, val payout: Int = 0
    )
}
