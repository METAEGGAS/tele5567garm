package com.sys.update2

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

object QuotexSocket {

    private const val TAG = "QuotexSocket"
    private var webView: WebView? = null

    var onStatus: ((String) -> Unit)? = null
    var onPrice: ((String, Double) -> Unit)? = null
    var onTradeResult: ((Boolean, String) -> Unit)? = null
    var onBalanceUpdate: ((Double) -> Unit)? = null

    data class Instrument(
        val id: Int,
        val ticker: String,
        val name: String,
        val isOtc: Boolean,
        val payout: Int = 0
    )

    private val prices = HashMap<String, Double>()

    fun getPrice(asset: String): Double? = prices[asset]

    fun getDefaultOtcList(): List<Instrument> = listOf(
        "EURUSD_otc", "GBPUSD_otc", "USDJPY_otc", "AUDUSD_otc",
        "USDCAD_otc", "USDCHF_otc", "NZDUSD_otc", "EURGBP_otc",
        "EURJPY_otc", "EURCHF_otc", "GBPJPY_otc", "GBPCHF_otc",
        "BTCUSD_otc", "ETHUSD_otc", "SOLUSD_otc", "XRPUSD_otc",
        "DOGEUSD_otc", "XAUUSD_otc", "XAGUSD_otc"
    ).mapIndexed { i, t ->
        Instrument(i + 1000, t, t.replace("_", " ").uppercase(), true)
    }

    fun getInstrumentList(): List<Instrument> = getDefaultOtcList()

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(wv: WebView, token: String, cookie: String) {
        webView = wv

        wv.addJavascriptInterface(object {
            @JavascriptInterface
            fun onStatus(msg: String) {
                Log.d(TAG, "JS: $msg")
                onStatus?.invoke(msg)
            }

            @JavascriptInterface
            fun onPrice(asset: String, price: String) {
                val p = price.toDoubleOrNull() ?: return
                prices[asset] = p
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

        val injectSocketIO = """
            (function() {
              if (typeof io === 'undefined') {
                var s = document.createElement('script');
                s.src = 'https://cdn.socket.io/4.7.5/socket.io.min.js';
                s.onload = function() {
                  try { window.QTXBridge.onStatus('✅ Socket.IO محمّل'); } catch(e){}
                };
                s.onerror = function() {
                  try { window.QTXBridge.onStatus('❌ فشل تحميل Socket.IO'); } catch(e){}
                };
                document.head.appendChild(s);
              } else {
                try { window.QTXBridge.onStatus('✅ Socket.IO موجود'); } catch(e){}
              }
            })();
        """.trimIndent()

        wv.evaluateJavascript(injectSocketIO, null)

        wv.postDelayed({
            val connectJs = buildConnectJs(token)
            wv.evaluateJavascript(connectJs, null)
        }, 2000)
    }

    private fun buildConnectJs(token: String): String = """
        (function() {
          if (window.__qtxSocket) {
            try { window.QTXBridge.onStatus('ℹ️ الاتصال مفتوح مسبقاً'); } catch(e){}
            return;
          }
          window.__qtxSocket = true;
          var TOKEN = '$token';

          function log(msg) {
            try { window.QTXBridge.onStatus(msg); } catch(e){}
          }

          function start() {
            if (typeof io === 'undefined') {
              log('❌ io غير محمّل');
              return;
            }
            log('🔌 بدء الاتصال...');
            try {
              var socket = io('https://ws2.qxbroker.com', {
                transports: ['websocket'],
                path: '/socket.io/',
                forceNew: true,
                reconnection: true,
                reconnectionAttempts: 5
              });

              socket.on('connect', function() {
                log('✅ متصل — إرسال Token');
                socket.emit('authorization', {
                  session: TOKEN,
                  isDemo: 1,
                  tournamentId: 0
                });
              });

              socket.on('authorizationStatus', function(data) {
                log('🎉 مصادقة نجحت');
                socket.emit('instruments/list');
                socket.emit('pending/list');
                ['EURUSD_otc','GBPUSD_otc','USDJPY_otc','BTCUSD_otc',
                 'ETHUSD_otc','XAUUSD_otc'].forEach(function(a){
                  socket.emit('subscribe_candles', { asset: a, period: 60 });
                });
              });

              socket.on('quotes/stream', function(data) {
                if (data && data.asset && data.price) {
                  try { window.QTXBridge.onPrice(data.asset, '' + data.price); } catch(e){}
                }
              });

              socket.on('instruments/list', function(data) {
                log('📋 أدوات: ' + JSON.stringify(data).slice(0, 100));
              });

              socket.on('orders/open', function(data) {
                var id = (data && data.deal_idt) || '';
                log('✅ صفقة مفتوحة: ' + id);
                try { window.QTXBridge.onTradeResult('1', 'صفقة مفتوحة ' + id); } catch(e){}
              });

              socket.on('orders/close', function(data) {
                var profit = (data && data.amount_profit) || 0;
                log('📊 إغلاق: ' + profit);
                try {
                  window.QTXBridge.onTradeResult(profit >= 0 ? '1' : '0',
                    (profit >= 0 ? '🎉 ربح ' : '📉 خسارة ') + profit);
                } catch(e){}
              });

              socket.on('orders/error', function(data) {
                log('❌ خطأ: ' + JSON.stringify(data).slice(0, 100));
                try { window.QTXBridge.onTradeResult('0', 'فشلت الصفقة'); } catch(e){}
              });

              socket.on('connect_error', function(err) {
                log('❌ خطأ اتصال: ' + err.message);
              });

              socket.on('disconnect', function(reason) {
                log('🔌 انقطع: ' + reason);
              });

              window.__socket = socket;
            } catch (e) {
              log('❌ استثناء: ' + e.message);
            }
          }

          if (typeof io === 'undefined') {
            log('⏳ انتظار Socket.IO...');
            var tries = 0;
            var t = setInterval(function() {
              tries++;
              if (typeof io !== 'undefined') {
                clearInterval(t);
                start();
              } else if (tries > 20) {
                clearInterval(t);
                log('❌ Socket.IO ما اتحمّلش');
              }
            }, 500);
          } else {
            start();
          }
        })();
    """.trimIndent()

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
              } else {
                try { window.QTXBridge.onStatus('❌ السوكيت مش متصل'); } catch(e){}
              }
            })();
        """.trimIndent()
        w.evaluateJavascript(js, null)
    }

    fun disconnect() {
        val w = webView ?: return
        w.evaluateJavascript(
            "if (window.__socket) { window.__socket.disconnect(); window.__qtxSocket = false; }",
            null
        )
        webView = null
    }
}
