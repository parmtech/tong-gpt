package au.parmtech.tonggpt

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Tong GPT — minimal independent Android shell for the official ChatGPT website.
 * Never inject scripts, use a JavaScript bridge, or read login form contents.
 */
class MainActivity : Activity() {
    companion object {
        private const val HOME = "https://chatgpt.com/"
        private const val PREFS = "tong_gpt_preferences"
        private const val PREF_KEEP_AWAKE = "keep_screen_awake"
        private const val PICK_FILE = 121
        private const val REQUEST_MEDIA = 122
        private const val REQUEST_MIC_TEST = 123
    }

    private lateinit var root: FrameLayout
    private lateinit var browser: WebView
    private lateinit var progress: ProgressBar
    private var blobSaver: BlobImageSaver? = null
    private lateinit var menuButton: TextView
    private val toolbarHeight by lazy { dp(44) }
    private var errorOverlay: View? = null
    private var pendingFiles: ValueCallback<Array<Uri>>? = null
    private var pendingMediaRequest: PermissionRequest? = null
    private var pendingMediaResources: Array<String> = emptyArray()
    private var fullScreenView: View? = null
    private var fullScreenCallback: WebChromeClient.CustomViewCallback? = null
    private var keepAwake = true
    private var lastBackTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepAwake = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getBoolean(PREF_KEEP_AWAKE, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.setBackgroundDrawableResource(R.color.background)
        volumeControlStream = AudioManager.STREAM_MUSIC
        // Render the app edge-to-edge but apply the phone's safe insets ourselves.
        // This avoids clipped text behind status/navigation bars and camera cutouts.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                 View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                 View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        root = FrameLayout(this).apply { setBackgroundColor(getColor(R.color.background)) }
        setContentView(root)
        configureSafeArea()
        makeBrowser()
        addLoadingBar()
        addAppToolbar()
        updateScreenAwake()

        if (savedInstanceState == null || browser.restoreState(savedInstanceState) == null) {
            browser.loadUrl(HOME)
        }
    }

    private fun makeBrowser() {
        browser = WebView(this).apply {
            setBackgroundColor(getColor(R.color.background))
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isSaveEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = 100
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                // WebRTC remote voice playback may begin after user starts the voice session.
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (request == null || !request.isForMainFrame) return false
                    val url = request.url.toString()
                    if (UrlPolicy.isInternal(url)) return false
                    openExternal(url)
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (this@MainActivity::progress.isInitialized) {
                        this@MainActivity.progress.visibility = View.VISIBLE
                        this@MainActivity.progress.progress = 8
                    }
                    errorOverlay?.visibility = View.GONE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (this@MainActivity::progress.isInitialized) this@MainActivity.progress.visibility = View.GONE
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        showLoadError(getString(R.string.web_unavailable))
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler?, error: SslError?
                ) {
                    handler?.cancel()
                    // Report a whole-page error only when the main document is impacted.
                    // A blocked subresource should not hide a working conversation.
                    val failingUrl = error?.url ?: ""
                    if (failingUrl == browser.url || UrlPolicy.isInternal(failingUrl) && browser.url == null) {
                        showLoadError(getString(R.string.secure_connection_failed))
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?, detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    if (view == browser) {
                        root.removeView(browser)
                        browser.destroy()
                        makeBrowser()
                        browser.loadUrl(HOME)
                    }
                    return true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (this@MainActivity::progress.isInitialized) {
                        this@MainActivity.progress.progress = newProgress.coerceIn(0, 100)
                        this@MainActivity.progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (filePathCallback == null || fileChooserParams == null) return false
                    pendingFiles?.onReceiveValue(null)
                    pendingFiles = filePathCallback
                    return try {
                        startActivityForResult(fileChooserParams.createIntent(), PICK_FILE)
                        true
                    } catch (_: ActivityNotFoundException) {
                        pendingFiles?.onReceiveValue(null)
                        pendingFiles = null
                        toast(getString(R.string.no_file_picker))
                        true
                    }
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request == null) return
                    runOnUiThread {
                        val origin = request.origin?.toString() ?: ""
                        val requested = request.resources.toSet()
                        val allowedResources = setOf(
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        )
                        // Never grant resources from other sites, or unknown WebView resources.
                        if (!UrlPolicy.isTrustedChatGptOrigin(origin) ||
                            requested.isEmpty() || !allowedResources.containsAll(requested)) {
                            request.deny()
                            return@runOnUiThread
                        }

                        val missing = mutableListOf<String>()
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in requested &&
                            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            missing.add(Manifest.permission.RECORD_AUDIO)
                        }
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in requested &&
                            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            missing.add(Manifest.permission.CAMERA)
                        }
                        if (missing.isEmpty()) {
                            request.grant(request.resources)
                        } else {
                            pendingMediaRequest?.deny()
                            pendingMediaRequest = request
                            pendingMediaResources = request.resources
                            requestPermissions(missing.toTypedArray(), REQUEST_MEDIA)
                        }
                    }
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                    if (pendingMediaRequest == request) {
                        pendingMediaRequest = null
                        pendingMediaResources = emptyArray()
                    }
                }

                // The web player can request a dedicated fullscreen view for video.
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null || callback == null || fullScreenView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    fullScreenView = view
                    fullScreenCallback = callback
                    root.addView(view, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    menuButton.visibility = View.GONE
                }

                override fun onHideCustomView() {
                    exitFullScreenPlayer()
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                downloadFile(url, userAgent, contentDisposition, mimeType)
            }
        }
        blobSaver = BlobImageSaver(this, browser).also { it.install() }
        val debug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        WebView.setWebContentsDebuggingEnabled(debug)
        root.addView(browser, 0, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).apply { topMargin = toolbarHeight })
    }

    private fun addLoadingBar() {
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progressTintList = ColorStateList.valueOf(getColor(R.color.aqua))
            progressBackgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            visibility = View.GONE
        }
        root.addView(progress, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP
        ).apply { topMargin = toolbarHeight })
    }

    /** Dedicated native toolbar: never overlays ChatGPT image/download controls. */
    private fun addAppToolbar() {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(getColor(R.color.background))
            setPadding(dp(13), 0, dp(5), 0)
        }
        bar.addView(TextView(this).apply {
            text = "TONG GPT"
            textSize = 12f
            letterSpacing = 0.13f
            setTextColor(getColor(R.color.aqua))
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = "Tong GPT app toolbar"
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        menuButton = TextView(this).apply {
            text = "⋮"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedSurface()
            contentDescription = "Tong GPT menu and About app"
            isClickable = true
            isFocusable = true
            setOnClickListener { showSettings() }
        }
        bar.addView(menuButton, LinearLayout.LayoutParams(dp(46), dp(40)))
        root.addView(bar, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, toolbarHeight, Gravity.TOP
        ))
    }

    private fun showSettings() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(10), dp(22), dp(8))
        }
        layout.addView(TextView(this).apply {
            text = getString(R.string.settings_description)
            textSize = 14f
            setTextColor(getColor(R.color.body_text))
            setPadding(0, dp(2), 0, dp(14))
        })
        layout.addView(Switch(this).apply {
            text = getString(R.string.keep_awake)
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = keepAwake
            showText = false
            setOnCheckedChangeListener { _, checked ->
                keepAwake = checked
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(PREF_KEEP_AWAKE, checked).apply()
                updateScreenAwake()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
        layout.addView(TextView(this).apply {
            text = getString(R.string.keep_awake_description)
            textSize = 12f
            setTextColor(getColor(R.color.body_text))
            setPadding(0, 0, 0, dp(18))
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.windows_remote)
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(this@MainActivity, WindowsRemoteActivity::class.java))
            }
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.codex_web)
            isAllCaps = false
            setOnClickListener {
                errorOverlay?.visibility = View.GONE
                browser.loadUrl("https://chatgpt.com/codex")
            }
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.reload_chat)
            isAllCaps = false
            setOnClickListener {
                errorOverlay?.visibility = View.GONE
                browser.reload()
            }
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.open_browser)
            isAllCaps = false
            setOnClickListener { openExternal(HOME) }
        })
        layout.addView(TextView(this).apply {
            text = getString(R.string.disclaimer)
            textSize = 12f
            setTextColor(getColor(R.color.body_text))
            setPadding(0, dp(18), 0, 0)
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.about_app)
            isAllCaps = false
            setOnClickListener { showAboutApp() }
        })
        layout.addView(Button(this).apply {
            text = getString(R.string.test_microphone)
            isAllCaps = false
            setOnClickListener { testMicrophone() }
        })
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(layout)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings))
            .setView(scroll)
            .setPositiveButton(getString(R.string.done), null)
            .show()
    }

    private fun testMicrophone() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC_TEST)
            return
        }
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val otherRecordings = audioManager.activeRecordingConfigurations.size
        Thread {
            val rate = 16000
            val buffer = AudioRecord.getMinBufferSize(
                rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            var result = "Microphone test could not start. Another app may be recording, " +
                "or your Android microphone privacy switch may be OFF."
            if (buffer > 0) {
                var record: AudioRecord? = null
                try {
                    record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(buffer * 2, 4096)
                    )
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        record.startRecording()
                        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            val samples = ShortArray(2048)
                            val size = record.read(samples, 0, samples.size)
                            val silenced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                record.activeRecordingConfiguration?.isClientSilenced == true
                            result = when {
                                silenced -> "Android reports this app microphone capture is silenced. " +
                                    "Close other recording apps and check Settings > Privacy > Microphone."
                                size > 0 -> "Android microphone test succeeded. If ChatGPT Voice " +
                                    "still shows a microphone error, Android WebView or another active " +
                                    "voice session may be blocking WebRTC. Try Voice in Chrome."
                                else -> "Android gave no microphone samples. Check Privacy > Microphone, " +
                                    "then close other apps recording audio."
                            }
                        }
                    }
                } catch (_: Exception) {
                    result = "Microphone hardware test failed. Close other voice/recording apps, " +
                        "check Android microphone privacy and try again."
                } finally {
                    try { if (record?.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() } catch (_: Exception) {}
                    record?.release()
                }
            }
            val details = if (otherRecordings > 0)
                "Android reported $otherRecordings recording session(s) before this test. " +
                "This does not identify which apps are using the microphone.\n\n" else ""
            runOnUiThread {
                if (!isFinishing && !isDestroyed) AlertDialog.Builder(this)
                    .setTitle("Microphone diagnostic")
                    .setMessage(details + result)
                    .setPositiveButton("Done", null)
                    .setNeutralButton("Voice in browser") { _, _ -> openExternal(HOME) }
                    .show()
            }
        }.start()
    }

    private fun showAboutApp() {
        val version = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) { "Unknown" }
        AlertDialog.Builder(this)
            .setTitle("About Tong GPT")
            .setMessage(
                "Tong GPT · Version " + version + "\n" +
                "Developed by ParmTech\n\n" +
                "Independent Android app for the official ChatGPT website, " +
                "with optional Windows Codex Remote. Not affiliated with or endorsed by OpenAI.\n\n" +
                "Website voice, sign-in and downloads may vary by Android System WebView version. " +
                "Use the external browser fallback if a web feature is not compatible."
            )
            .setPositiveButton(getString(R.string.done), null)
            .show()
    }

    private fun showLoadError(message: String) {
        if (isFinishing || isDestroyed) return
        errorOverlay?.let { root.removeView(it) }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(30), dp(30), dp(30), dp(30))
            setBackgroundColor(getColor(R.color.background))
        }
        panel.addView(TextView(this).apply {
            text = getString(R.string.cannot_load)
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        panel.addView(TextView(this).apply {
            text = message
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.body_text))
            setPadding(0, dp(12), 0, dp(16))
        })
        panel.addView(Button(this).apply {
            text = getString(R.string.retry)
            setOnClickListener { errorOverlay?.visibility = View.GONE; browser.loadUrl(HOME) }
        })
        panel.addView(Button(this).apply {
            text = getString(R.string.open_browser)
            setOnClickListener { openExternal(HOME) }
        })
        root.addView(panel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        errorOverlay = panel
        menuButton.bringToFront()
    }

    private fun downloadFile(
        url: String, userAgent: String, contentDisposition: String?, mimeType: String?
    ) {
        val suggested = URLUtil.guessFileName(url, contentDisposition, mimeType)
        if (url.startsWith("blob:", ignoreCase = true) ||
            url.startsWith("data:", ignoreCase = true)) {
            if (blobSaver?.handle(url, suggested) == true) return
            toast("Cannot save this image here. Use Chrome instead.")
            return
        }
        val uri = try { Uri.parse(url) } catch (_: Exception) { return }
        if (uri.scheme != "https") {
            toast(getString(R.string.unsupported_download))
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            openExternal(url)
            return
        }
        try {
            val safeName = suggested.replace(Regex("[^a-zA-Z0-9._() -]"), "_")
                .take(100).ifBlank { "TongGPT-download" }
            val request = DownloadManager.Request(uri).apply {
                setTitle(safeName)
                setDescription(getString(R.string.download_description))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
                if (!mimeType.isNullOrEmpty()) setMimeType(mimeType)
                if (UrlPolicy.isTrustedChatGptOrigin(url)) {
                    CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                }
                addRequestHeader("User-Agent", userAgent)
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            toast(getString(R.string.download_started))
        } catch (_: Exception) {
            toast(getString(R.string.unsupported_download))
        }
    }

    private fun exitFullScreenPlayer() {
        fullScreenView?.let { root.removeView(it) }
        fullScreenView = null
        fullScreenCallback?.onCustomViewHidden()
        fullScreenCallback = null
        if (::menuButton.isInitialized) menuButton.visibility = View.VISIBLE
    }

    private fun openExternal(url: String) {
        if (!UrlPolicy.canLaunchExternally(url)) {
            toast(getString(R.string.link_blocked))
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            toast(getString(R.string.browser_unavailable))
        }
    }

    private fun roundedSurface(): GradientDrawable = GradientDrawable().apply {
        setColor(Color.rgb(15, 29, 49))
        cornerRadius = dp(12).toFloat()
        setStroke(dp(1), Color.rgb(40, 104, 123))
    }

    private fun updateScreenAwake() {
        if (keepAwake && !isFinishing && !isDestroyed) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun configureSafeArea() {
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val safe = insets.getInsets(
                    WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars() or
                    WindowInsets.Type.displayCutout()
                )
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                view.setPadding(
                    safe.left,
                    safe.top,
                    safe.right,
                    maxOf(safe.bottom, keyboard.bottom)
                )
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            // Do not consume insets: the website and Android system can still
            // measure the current viewport and soft keyboard correctly.
            insets
        }
        root.requestApplyInsets()
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value + 0.5f).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onResume() {
        super.onResume()
        if (::browser.isInitialized) browser.onResume()
        updateScreenAwake()
    }

    override fun onPause() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (::browser.isInitialized) browser.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::browser.isInitialized) browser.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Platform file chooser result handler, intentionally without AndroidX.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE) {
            pendingFiles?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            pendingFiles = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_TEST) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                testMicrophone()
            } else {
                toast("Allow microphone permission to run the diagnostic.")
            }
        }
        if (requestCode == REQUEST_MEDIA) {
            pendingMediaRequest?.let { request ->
                val allGranted = permissions.indices.all { index ->
                    grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                }
                if (allGranted && permissions.isNotEmpty()) {
                    request.grant(pendingMediaResources)
                } else {
                    request.deny()
                }
            }
            pendingMediaRequest = null
            pendingMediaResources = emptyArray()
        }
    }

    @Deprecated("Platform back navigation; keeps WebView history intact.")
    override fun onBackPressed() {
        if (fullScreenView != null) {
            exitFullScreenPlayer()
            return
        }
        if (::browser.isInitialized && browser.canGoBack()) {
            browser.goBack()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackTime < 1700L) {
            super.onBackPressed()
        } else {
            toast(getString(R.string.back_to_exit))
            lastBackTime = now
        }
    }

    override fun onDestroy() {
        pendingFiles?.onReceiveValue(null)
        pendingFiles = null
        pendingMediaRequest?.deny()
        pendingMediaRequest = null
        pendingMediaResources = emptyArray()
        if (fullScreenView != null) exitFullScreenPlayer()
        if (::browser.isInitialized) {
            root.removeView(browser)
            browser.stopLoading()
            browser.destroy()
        }
        super.onDestroy()
    }
}
