package io.github.noelmuthomi.churchschool

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var offlineView: View
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Hosts that stay inside the app (Apps Script + Google sign-in). Everything else opens externally.
    private val inAppHosts = listOf("script.google.com", "script.googleusercontent.com", "accounts.google.com")

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Edge-to-edge with navy bars; pad content so nothing hides under system bars / keyboard
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(R.id.root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }

        webView = findViewById(R.id.webview)
        progress = findViewById(R.id.progress)
        offlineView = findViewById(R.id.offline)
        findViewById<Button>(R.id.retry).setOnClickListener { loadApp() }

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            textZoom = 100
            userAgentString = userAgentString + " ChurchSchoolApp/1.0"
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true) // Apps Script runs inside a Google-hosted frame
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                CookieManager.getInstance().flush()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                return handleUrl(request.url)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showOffline(true)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                if (newProgress >= 100) progress.visibility = View.GONE
            }

            // <input type="file"> support (photos, documents)
            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    fileChooser.launch(params.createIntent())
                    true
                } catch (e: ActivityNotFoundException) {
                    filePathCallback = null
                    false
                }
            }

            // target="_blank" / window.open → open in the browser, not inside the app
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                val temp = WebView(this@MainActivity)
                temp.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                        openExternal(r.url)
                        v.destroy()
                        return true
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = temp
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val req = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    addRequestHeader("User-Agent", userAgent)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType)
                    )
                }
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                Toast.makeText(this, "Downloading…", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                openExternal(Uri.parse(url))
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState != null) webView.restoreState(savedInstanceState) else loadApp()
    }

    private fun loadApp() {
        showOffline(!isOnline())
        if (isOnline()) webView.loadUrl(BuildConfig.APP_URL)
    }

    private fun showOffline(show: Boolean) {
        offlineView.visibility = if (show) View.VISIBLE else View.GONE
        if (show) progress.visibility = View.GONE
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** true = we handled it (opened externally); false = let the WebView load it. */
    private fun handleUrl(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return false
        if (scheme == "http" || scheme == "https") {
            val host = uri.host ?: return false
            if (inAppHosts.any { host == it }) return false
        }
        openExternal(uri) // tel:, mailto:, whatsapp:, other websites, etc.
        return true
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { webView.onPause(); CookieManager.getInstance().flush(); super.onPause() }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
