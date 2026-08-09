package ng.leafsolar.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = true
            webViewClient = object : WebViewClient() {
                // Keep all navigation inside the app (package links open externally)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    return if (url.startsWith("file:///android_asset/")) false
                    else { view.context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)); true }
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl("file:///android_asset/www/index.html")
        }
        setContentView(web)
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
