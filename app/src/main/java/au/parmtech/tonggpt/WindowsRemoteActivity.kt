package au.parmtech.tonggpt

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.content.res.Configuration
import android.os.Build
import android.view.WindowInsets
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Windows Remote beta: Codex App Server JSON-RPC + MeshCentral desktop WebView.
 * One Android application, two independently authenticated private services.
 * Never opens remote command permissions silently.
 */
class WindowsRemoteActivity : Activity(), TextToSpeech.OnInitListener {
    companion object {
        private const val SPEECH_REQUEST = 301
        private const val PAIRING_QR_REQUEST = 302
        private const val PREFS = "tg_remote_windows"
    }

    private val net = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private var ws: WebSocket? = null
    private var connected = false
    private var sequence = 0
    private val pending = HashMap<Int, (JSONObject) -> Unit>()
    private var activeThread: String? = null
    private var activeTurn: String? = null
    private var threadLoaded = false
    private var replyBuffer = StringBuilder()
    private lateinit var root: LinearLayout
    private lateinit var paneHolder: LinearLayout
    private lateinit var codexPane: LinearLayout
    private lateinit var desktopPane: LinearLayout
    private lateinit var connectForm: LinearLayout
    private lateinit var endpoint: EditText
    private lateinit var tokenField: EditText
    private lateinit var desktopUrlField: EditText
    private lateinit var status: TextView
    private lateinit var transcript: LinearLayout
    private lateinit var transcriptScroll: ScrollView
    private lateinit var prompt: EditText
    private lateinit var desktopWeb: WebView
    private var desktopHost = ""
    private var tts: TextToSpeech? = null
    private var sayReplies = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = Color.rgb(10, 17, 29)
        window.navigationBarColor = Color.rgb(10, 17, 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.setDecorFitsSystemWindows(false)
        tts = TextToSpeech(this, this)
        createUi()
        if (intent?.getBooleanExtra("scan_pairing_qr", false) == true) {
            root.post { openScanner() }
        }
    }

    private fun createUi() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(4), dp(7), dp(7))
            setBackgroundColor(Color.rgb(10, 17, 29))
        }
        setContentView(root)
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val safe = insets.getInsets(WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                v.setPadding(dp(10) + safe.left, safe.top + dp(6),
                    dp(10) + safe.right, maxOf(keyboard.bottom, safe.bottom) + dp(8))
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(dp(10) + insets.systemWindowInsetLeft,
                    dp(6) + insets.systemWindowInsetTop, dp(10) + insets.systemWindowInsetRight,
                    dp(8) + insets.systemWindowInsetBottom)
            }
            insets
        }
        root.requestApplyInsets()

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
            background = roundedPanel(0xFF14263A.toInt(), dp(18))
        }
        tabs.addView(btn("← Chat") { finish() }, weighted())
        tabs.addView(btn("Codex") { setMode(0) }, weighted())
        tabs.addView(btn("Desktop") { setMode(1) }, weighted())
        tabs.addView(btn("Split") { setMode(2) }, weighted())
        root.addView(tabs)

        status = text("Windows remote · Not connected", 12, Color.rgb(155, 198, 213)).apply {
            setPadding(dp(9), dp(5), dp(9), dp(7))
        }
        root.addView(status)

        paneHolder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(paneHolder, paneParams())
        codexPane = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), 0, dp(3), 0)
        }
        paneHolder.addView(codexPane, paneParams())

        connectForm = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        codexPane.addView(connectForm)
        connectForm.addView(text("Codex App Server · private WSS address", 13, 0xFF72E2D9.toInt()))
        endpoint = field("wss://your-pc.tailnet.ts.net:8443/").apply {
            setSingleLine(true)
            setText(prefs.getString("codex_endpoint", ""))
        }
        connectForm.addView(endpoint)
        connectForm.addView(text("Strong Codex pairing token", 13, 0xFF72E2D9.toInt()))
        tokenField = field("Token generated on your own Windows PC").apply {
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(SecureCodexToken(this@WindowsRemoteActivity).read() ?: "")
        }
        connectForm.addView(tokenField)
        connectForm.addView(btn("📷 Scan pairing QR") { openScanner() })
        connectForm.addView(btn("Connect Codex securely") { connect() })

        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.addView(btn("📷 QR") { openScanner() }, weighted())
        tools.addView(btn("Pair") {
            connectForm.visibility = if (connectForm.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }, weighted())
        tools.addView(btn("Chats") { listThreads() }, weighted())
        tools.addView(btn("New") { newThread() }, weighted())
        tools.addView(btn("Stop") { stopTurn() }, weighted())
        tools.addView(btn(if (sayReplies) "🔊 ON" else "🔇 OFF") { button ->
            sayReplies = !sayReplies
            button.text = if (sayReplies) "🔊 ON" else "🔇 OFF"
            if (!sayReplies) tts?.stop()
        }, weighted())
        codexPane.addView(tools)

        transcriptScroll = ScrollView(this).apply { isFillViewport = true }
        transcript = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(8), dp(7), dp(10))
        }
        transcriptScroll.addView(transcript)
        codexPane.addView(transcriptScroll, paneParams())
        append("Tip", "To view your existing Windows Codex CLI/App Server chats, connect the secure Windows endpoint. ChatGPT Desktop's proprietary chats may not all be available.")

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        prompt = field("Type a Codex instruction…").apply {
            maxLines = 3
            minLines = 1
        }
        composer.addView(prompt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        composer.addView(btn("🎤") { voiceInput() })
        composer.addView(btn("➤") { submitPrompt(prompt.text.toString()) })
        codexPane.addView(composer)

        desktopPane = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        paneHolder.addView(desktopPane, paneParams())
        desktopPane.addView(text("Windows live desktop · MeshCentral (private HTTPS)", 13, 0xFF72E2D9.toInt()))
        val desktopControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        desktopUrlField = field("https://your-pc.tailnet.ts.net:8444/").apply {
            setSingleLine(true)
            setText(prefs.getString("desktop_endpoint", ""))
        }
        desktopControls.addView(desktopUrlField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        desktopControls.addView(btn("Open") { openDesktop() })
        desktopPane.addView(desktopControls)

        desktopWeb = WebView(this).apply {
            setBackgroundColor(Color.rgb(16, 24, 36))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.safeBrowsingEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (request == null || !request.isForMainFrame) return false
                    val target = request.url.toString()
                    val uri = try { URI(target) } catch (_: Exception) { null }
                    if (uri?.scheme.equals("https", ignoreCase = true) &&
                        uri?.host?.lowercase(Locale.ROOT) == desktopHost) return false
                    if (uri?.scheme.equals("https", ignoreCase = true)) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                    }
                    return true
                }
                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler?, error: SslError?
                ) {
                    handler?.cancel()
                    status("Desktop TLS certificate failed. Connection stopped.")
                }
            }
            webChromeClient = WebChromeClient()
        }
        desktopPane.addView(desktopWeb, paneParams())
        setMode(0)
    }

    private fun setMode(mode: Int) {
        // Parent is vertical: weight must apply to HEIGHT, never width.
        // Landscape split shows columns, portrait split shows rows.
        val wide = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        paneHolder.orientation = if (mode == 2 && wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        codexPane.visibility = if (mode == 1) View.GONE else View.VISIBLE
        desktopPane.visibility = if (mode == 0) View.GONE else View.VISIBLE
        val params = if (paneHolder.orientation == LinearLayout.HORIZONTAL) {
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        } else {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        codexPane.layoutParams = LinearLayout.LayoutParams(params.width, params.height, params.weight)
        desktopPane.layoutParams = LinearLayout.LayoutParams(params.width, params.height, params.weight)
        // Reduce setup form in split mode; Codex session and live desktop remain visible.
        connectForm.visibility = if (mode == 2 && connected) View.GONE else View.VISIBLE
        paneHolder.requestLayout()
        status(
            when(mode) {
                0 -> if (connected) "Codex connected" else "Codex · Connect your Windows PC"
                1 -> "Desktop · Secure MeshCentral connection"
                else -> "Split · Codex and live Windows screen"
            }
        )
    }

    private fun paneParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)

    private fun openDesktop() {
        val raw = desktopUrlField.text.toString().trim()
        val uri = try { URI(raw) } catch (_: Exception) { null }
        if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || uri.fragment != null) {
            toast("Enter your own HTTPS MeshCentral address (TLS required).")
            return
        }
        desktopHost = uri.host.lowercase(Locale.ROOT)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString("desktop_endpoint", raw).apply()
        desktopWeb.loadUrl(raw)
        status("Desktop portal loading. Sign in directly to your MeshCentral account.")
    }

    private fun connect() {
        val host = endpoint.text.toString().trim()
        val secret = tokenField.text.toString().trim()
        val uri = try { URI(host) } catch (_: Exception) { null }
        if (uri == null || uri.scheme != "wss" || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || uri.fragment != null ||
            secret.length < 32 || secret.any { it.isWhitespace() }) {
            toast("Enter a valid secure wss:// endpoint and a 32+ character private token.")
            return
        }
        disconnect()
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString("codex_endpoint", host).apply()
        SecureCodexToken(this).save(secret)
        status("Connecting securely to Windows Codex…")
        val request = try {
            Request.Builder().url(host)
                .header("Authorization", "Bearer " + secret)
                .build()
        } catch (_: Exception) {
            status("Invalid endpoint address"); return
        }
        ws = net.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    if (webSocket !== ws) return@runOnUiThread
                    connected = true
                    status("Connected. Initialising Codex App Server…")
                    rpc("initialize", JSONObject().put("clientInfo",
                        JSONObject().put("name", "tong_gpt_android")
                            .put("title", "Tong GPT Remote")
                            .put("version", "0.3.0")
                    )) { answer ->
                        if (answer.has("error")) {
                            status("Codex handshake rejected. Check server version.")
                            return@rpc
                        }
                        sendJson(JSONObject().put("method", "initialized")
                            .put("params", JSONObject()))
                        connectForm.visibility = View.GONE
                        listThreads()
                    }
                }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread { if (ws === webSocket) handleMessage(text) }
            }
            override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
                runOnUiThread {
                    if (webSocket === ws) {
                        connected = false
                        connectForm.visibility = View.VISIBLE
                        status("Codex connection failed (" + error.javaClass.simpleName + "). Check Tailscale, WSS and token.")
                    }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    if (webSocket === ws) {
                        connected = false
                        connectForm.visibility = View.VISIBLE
                        status("Codex disconnected.")
                    }
                }
            }
        })
    }

    private fun handleMessage(raw: String) {
        val value = try { JSONObject(raw) } catch (_: Exception) { return }
        if (value.has("id") && value.has("method")) {
            approvalRequest(value)
            return
        }
        if (value.has("id")) {
            pending.remove(value.optInt("id", -1))?.invoke(value)
            return
        }
        val params = value.optJSONObject("params") ?: JSONObject()
        when (value.optString("method")) {
            "turn/started" -> {
                activeTurn = params.optJSONObject("turn")?.optString("id")
                status("Codex working · live events connected")
            }
            "item/agentMessage/delta" -> {
                replyBuffer.append(params.optString("delta"))
                status("Codex speaking… " + replyBuffer.length + " characters")
            }
            "item/commandExecution/outputDelta" -> {
                val delta = params.optString("delta")
                if (delta.isNotEmpty()) append("Terminal", delta.takeLast(500))
            }
            "item/started" -> {
                val item = params.optJSONObject("item")
                if (item?.optString("type") == "commandExecution") {
                    append("Running command", item.optString("command").take(500))
                }
            }
            "item/completed" -> {
                val item = params.optJSONObject("item") ?: return
                when (item.optString("type")) {
                    "agentMessage" -> {
                        val answer = item.optString("text").take(12000)
                        if (answer.isNotBlank()) {
                            replyBuffer = StringBuilder(answer)
                            append("Codex", answer)
                        }
                    }
                    "commandExecution" -> {
                        append("Command", "Status: " + item.optString("status") +
                            " · Exit: " + item.optString("exitCode"))
                    }
                    "fileChange" -> append("Files", "File change: " + item.optString("status"))
                }
            }
            "turn/completed" -> {
                val speech = replyBuffer.toString()
                activeTurn = null
                status("Codex finished. Desktop stays connected.")
                if (sayReplies && speech.isNotBlank()) {
                    tts?.speak(speech.take(2400), TextToSpeech.QUEUE_FLUSH, null, "tg_codex_reply")
                }
                replyBuffer.clear()
            }
            "error" -> {
                val e = params.optJSONObject("error")
                status("Codex error: " + (e?.optString("message") ?: "Unknown error").take(170))
            }
        }
    }

    private fun approvalRequest(message: JSONObject) {
        val method = message.optString("method")
        val id = message.opt("id")
        val params = message.optJSONObject("params") ?: JSONObject()
        if (method != "item/commandExecution/requestApproval" &&
            method != "item/fileChange/requestApproval") {
            sendJson(JSONObject().put("id", id).put("error",
                JSONObject().put("code", -32601)
                    .put("message", "Client does not support this approval")))
            return
        }
        val details = params.optString("reason", "Codex needs permission").take(550)
        val command = params.optString("command", "").take(1400)
        val cwd = params.optString("cwd", "").take(250)
        val summary = "Windows Codex requests permission:\n" + details +
            (if (cwd.isNotBlank()) "\nFolder: " + cwd else "") +
            (if (command.isNotBlank()) "\nCommand: " + command else "") +
            "\n\nOnly approve actions you understand."
        AlertDialog.Builder(this)
            .setTitle("Codex approval required")
            .setMessage(summary)
            .setPositiveButton("Approve once") { _, _ ->
                sendJson(JSONObject().put("id", id)
                    .put("result", JSONObject().put("decision", "accept")))
            }
            .setNegativeButton("Deny") { _, _ ->
                sendJson(JSONObject().put("id", id)
                    .put("result", JSONObject().put("decision", "decline")))
            }
            .setOnCancelListener {
                sendJson(JSONObject().put("id", id)
                    .put("result", JSONObject().put("decision", "decline")))
            }.show()
    }

    private fun rpc(method: String, params: JSONObject, callback: (JSONObject) -> Unit) {
        if (!connected) {
            status("Connect to Windows Codex first.")
            return
        }
        val id = ++sequence
        pending[id] = callback
        if (!sendJson(JSONObject().put("id", id).put("method", method)
                .put("params", params))) {
            pending.remove(id)
            status("Could not send; reconnect to Windows.")
        }
    }

    private fun sendJson(data: JSONObject): Boolean = ws?.send(data.toString()) ?: false

    private fun listThreads() {
        if (!connected) {
            status("Connect Codex first.")
            return
        }
        transcript.removeAllViews()
        activeThread = null
        threadLoaded = false
        append("Codex", "Loading your Windows sessions…")
        rpc("thread/list", JSONObject().put("limit", 35)
            .put("sortKey", "updated_at")
            .put("sourceKinds", JSONArray().put("cli").put("vscode")
                .put("appServer").put("exec").put("unknown"))
            .put("modelProviders", JSONArray())) { result ->
            val data = result.optJSONObject("result")?.optJSONArray("data")
            transcript.removeAllViews()
            if (data == null) {
                append("Codex", "Cannot load threads. Check app-server version and account.")
                return@rpc
            }
            append("Windows Codex chats", "Tap a session to see its history and continue it from your phone.")
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val thread = item.optString("id")
                if (thread.isEmpty()) continue
                val title = item.optString("name")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: item.optString("preview").take(100).ifBlank { "Untitled Codex chat" }
                transcript.addView(btn(title) { openThread(thread) })
            }
            if (data.length() == 0) append("No sessions found",
                "ChatGPT Desktop proprietary sessions may not be accessible in Codex CLI history.")
            status("Connected · Windows Codex")
        }
    }

    private fun openThread(id: String) {
        activeThread = id
        threadLoaded = false
        transcript.removeAllViews()
        append("Loading chat", id.take(70))
        rpc("thread/turns/list", JSONObject().put("threadId", id)
            .put("limit", 20)
            .put("sortDirection", "desc")
            .put("itemsView", "full")) { result ->
            if (activeThread != id) return@rpc
            transcript.removeAllViews()
            append("Windows Codex", "Session " + id.take(20) + " · latest turns")
            val turns = result.optJSONObject("result")?.optJSONArray("data") ?: JSONArray()
            for (t in turns.length() - 1 downTo 0) {
                val items = turns.optJSONObject(t)?.optJSONArray("items") ?: continue
                for (i in 0 until items.length()) {
                    val it = items.optJSONObject(i) ?: continue
                    when (it.optString("type")) {
                        "agentMessage" -> append("Codex", it.optString("text").take(9000))
                        "userMessage" -> {
                            val content = it.optJSONArray("content") ?: JSONArray()
                            for (k in 0 until content.length()) {
                                val text = content.optJSONObject(k)?.optString("text") ?: ""
                                if (text.isNotBlank()) append("You", text.take(9000))
                            }
                        }
                        "commandExecution" -> append("Command", it.optString("command").take(700))
                    }
                }
            }
            status("Loading session in Windows Codex…")
            rpc("thread/resume", JSONObject().put("threadId", id).put("excludeTurns", true)) { resume ->
                threadLoaded = resume.optJSONObject("result") != null
                status(if (threadLoaded) "Ready · send an instruction or tap 🎤" else
                    "Could not resume chat. Check Windows Codex settings.")
            }
        }
    }

    private fun newThread() {
        if (!connected) { status("Connect Codex first."); return }
        rpc("thread/start", JSONObject().put("serviceName", "tong_gpt_android")) { result ->
            val id = result.optJSONObject("result")?.optJSONObject("thread")?.optString("id")
            if (id.isNullOrBlank()) {
                status("Could not create a new Codex chat.")
                return@rpc
            }
            activeThread = id
            threadLoaded = true
            transcript.removeAllViews()
            append("Codex", "New remote chat ready. Work runs on your Windows PC.")
            status("Ready · New Windows Codex session")
        }
    }

    private fun submitPrompt(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (!connected) { toast("Connect Windows Codex first"); return }
        if (!threadLoaded || activeThread == null) {
            toast("Select an existing session or tap New")
            return
        }
        val id = activeThread ?: return
        if (activeTurn != null) {
            status("A task is already running. Stop it first or wait for completion.")
            return
        }
        prompt.setText("")
        replyBuffer.clear()
        append("You", clean)
        val input = JSONArray().put(JSONObject().put("type", "text").put("text", clean))
        rpc("turn/start", JSONObject().put("threadId", id).put("input", input)) { response ->
            if (response.has("error")) {
                val message = response.optJSONObject("error")?.optString("message") ?: "Rejected"
                status("Codex rejected the instruction: " + message.take(170))
            } else {
                activeTurn = response.optJSONObject("result")?.optJSONObject("turn")?.optString("id")
                status("Codex is working… watch live updates below.")
            }
        }
    }

    private fun stopTurn() {
        val thread = activeThread
        val turn = activeTurn
        if (thread == null || turn == null) {
            status("No active Codex task to interrupt.")
            return
        }
        rpc("turn/interrupt", JSONObject().put("threadId", thread)
            .put("turnId", turn)) {
            status("Stop requested for current Codex task.")
        }
    }

    private fun openScanner() {
        try {
            startActivityForResult(
                Intent(this, PairingQrScannerActivity::class.java), PAIRING_QR_REQUEST
            )
        } catch (_: ActivityNotFoundException) {
            toast("Camera scanner is not available on this device.")
        }
    }

    private fun handleScannedPairing(code: String) {
        when (val result = PairingCodeParser.parse(code)) {
            is PairingCodeParser.Parsed.Companion -> {
                val host = try { URI(result.endpoint).host ?: "your computer" }
                    catch (_: Exception) { "your computer" }
                AlertDialog.Builder(this)
                    .setTitle("Pair Tong GPT Windows Codex")
                    .setMessage(
                        "Connect to " + host + "?\n\n" +
                        "This QR includes a private connection token. " +
                        "Only scan a QR shown on a computer you own and trust. " +
                        "No connection starts until you approve."
                    )
                    .setPositiveButton("Connect") { _, _ ->
                        endpoint.setText(result.endpoint)
                        tokenField.setText(result.token)
                        result.desktopUrl?.let { desktopUrlField.setText(it) }
                        connectForm.visibility = View.VISIBLE
                        setMode(0)
                        connect()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            is PairingCodeParser.Parsed.Official -> {
                AlertDialog.Builder(this)
                    .setTitle("Official Codex Desktop QR")
                    .setMessage(
                        "This is the official ChatGPT device-pairing code. " +
                        "Its connection approval uses OpenAI's official mobile app, " +
                        "not Tong GPT's companion protocol.\n\n" +
                        "Open it in the official ChatGPT Android app? " +
                        "If pairing does not open automatically, scan the QR " +
                        "again using the official app."
                    )
                    .setPositiveButton("Open official ChatGPT") { _, _ ->
                        try {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(result.link))
                                    .setPackage("com.openai.chatgpt")
                            )
                        } catch (_: Exception) {
                            AlertDialog.Builder(this)
                                .setMessage("Open the official ChatGPT app and " +
                                    "use Codex / Remote to scan the Windows QR.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            PairingCodeParser.Parsed.Unsupported -> {
                AlertDialog.Builder(this)
                    .setTitle("Unrecognized QR code")
                    .setMessage(
                        "Tong GPT can pair with its own Windows companion QR, " +
                        "but cannot directly approve OpenAI's private " +
                        "ChatGPT Desktop Remote pairing code.\n\n" +
                        "Use the official ChatGPT app for OpenAI Remote, or " +
                        "generate a Tong GPT companion QR on your Windows PC."
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun voiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak an instruction for Windows Codex")
        try {
            startActivityForResult(intent, SPEECH_REQUEST)
        } catch (_: ActivityNotFoundException) {
            toast("No Android speech recognition service found. Type your instruction instead.")
        }
    }

    @Deprecated("Uses a system speech recognition activity for user-initiated dictation.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAIRING_QR_REQUEST && resultCode == RESULT_OK) {
            val scanned = data?.getStringExtra(PairingQrScannerActivity.RESULT_QR_CONTENT)
            if (!scanned.isNullOrBlank()) handleScannedPairing(scanned)
        }
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!words.isNullOrEmpty()) {
                prompt.setText(words)
                // Do not auto-execute spoken commands: users can review/edit them first.
                status("Voice transcription ready · review the text, then press ➤")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(1.0f)
        }
    }

    private fun append(title: String, body: String) {
        if (body.isBlank()) return
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(7), dp(11), dp(10))
            setBackgroundColor(if (title == "You") 0xFF1D4050.toInt() else 0xFF15263A.toInt())
        }
        block.addView(text(title, 12, 0xFF57DCCF.toInt()))
        block.addView(text(body.take(12000), 14, Color.WHITE).apply {
            setTextIsSelectable(true)
        })
        val margin = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) }
        transcript.addView(block, margin)
        // Bound the number of live DOM-like TextViews during a long terminal session.
        while (transcript.childCount > 90) transcript.removeViewAt(0)
        transcriptScroll.post { transcriptScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun status(message: String) { status.text = message }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private fun text(body: String, size: Int, color: Int) = TextView(this).apply {
        text = body
        textSize = size.toFloat()
        setTextColor(color)
        setPadding(dp(4), dp(5), dp(4), dp(5))
    }
    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 13f
        setTextColor(Color.WHITE)
        setHintTextColor(0xFF9EAEC0.toInt())
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = roundedPanel(0xFF1C3147.toInt(), dp(12))
    }
    private fun btn(title: String, onClick: (Button) -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 12f
        minHeight = dp(44)
        minimumHeight = dp(44)
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF30445D.toInt())
        setPadding(dp(8), 0, dp(8), 0)
        setOnClickListener { onClick(this) }
    }

    private fun roundedPanel(color: Int, corners: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = corners.toFloat() }
    private fun weighted() = LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
    )
    private fun dp(n: Int) = (resources.displayMetrics.density * n + 0.5f).toInt()

    private fun disconnect() {
        connected = false
        activeTurn = null
        pending.clear()
        ws?.close(1000, "User disconnected")
        ws = null
    }

    override fun onPause() {
        if (::desktopWeb.isInitialized) desktopWeb.onPause()
        tts?.stop()
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        if (::desktopWeb.isInitialized) desktopWeb.onResume()
    }
    override fun onDestroy() {
        disconnect()
        if (::desktopWeb.isInitialized) {
            desktopWeb.stopLoading()
            desktopWeb.destroy()
        }
        tts?.shutdown()
        net.dispatcher.executorService.shutdown()
        net.connectionPool.evictAll()
        super.onDestroy()
    }
}
