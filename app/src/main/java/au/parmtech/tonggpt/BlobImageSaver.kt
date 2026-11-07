package au.parmtech.tonggpt

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.WebView
import android.widget.Toast
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.util.UUID

/** User-initiated ChatGPT blob-image saver. Never reads page DOM, cookies or login data. */
internal class BlobImageSaver(private val activity: Activity, private val web: WebView) {
    companion object { private const val MAX = 25 * 1024 * 1024 }
    private val handler = Handler(Looper.getMainLooper())
    private var activeId: String? = null
    private var activeName: String? = null

    fun install() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(
            web, "TongImageSave", setOf("https://chatgpt.com", "https://*.chatgpt.com"),
            object : WebViewCompat.WebMessageListener {
                override fun onPostMessage(
                    view: WebView, message: WebMessageCompat, origin: Uri,
                    isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy
                ) {
                    if (view !== web || !isMainFrame ||
                        !UrlPolicy.isTrustedChatGptOrigin(origin.toString())) return
                    val data = try { JSONObject(message.data ?: return) } catch (_: Exception) { return }
                    if (data.optString("id") != activeId) return
                    val name = activeName ?: return
                    activeId = null
                    activeName = null
                    when (data.optString("status")) {
                        "ok" -> saveImage(name, data.optString("data"))
                        "size" -> note("File larger than 25 MB. Try Chrome.")
                        else -> note("File download failed. Try Chrome.")
                    }
                }
            }
        )
    }

    /** Returns true if the URL was accepted and handled, even if saving later fails. */
    fun handle(url: String, filename: String): Boolean {
        if (!UrlPolicy.isTrustedChatGptOrigin(web.url ?: "")) return false
        if (url.startsWith("data:", ignoreCase = true)) {
            saveImage(filename, url)
            return true
        }
        val allowed = url.startsWith("blob:https://chatgpt.com/", ignoreCase = true) ||
            Regex("^blob:https://[a-z0-9.-]+\\.chatgpt\\.com/", RegexOption.IGNORE_CASE)
                .containsMatchIn(url)
        if (!allowed) return false
        if (activeId != null) {
            note("A download is already in progress.")
            return true
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            note("Update Android System WebView to save images inside Tong GPT.")
            return true
        }
        val id = UUID.randomUUID().toString()
        activeId = id
        activeName = filename
        handler.postDelayed({
            if (activeId == id) {
                activeId = null
                activeName = null
                note("Image download timed out. Try again or use Chrome.")
            }
        }, 30_000L)
        // JSON.quote escapes the URL: no script text is taken unescaped from the webpage.
        val script = """
            (() => {
              const id = __ID__;
              const url = __URL__;
              const report = (status, data) => {
                if (window.TongImageSave)
                  window.TongImageSave.postMessage(JSON.stringify({id, status, data: data || ""}));
              };
              fetch(url).then(resp => {
                if (!resp.ok) throw Error("fetch");
                return resp.blob();
              }).then(blob => {
                if (blob.size > __MAX__) { report("size"); return; }
                const reader = new FileReader();
                reader.onerror = () => report("error");
                reader.onload = () => report("ok", reader.result);
                reader.readAsDataURL(blob);
              }).catch(() => report("error"));
            })();
        """.trimIndent()
            .replace("__ID__", JSONObject.quote(id))
            .replace("__URL__", JSONObject.quote(url))
            .replace("__MAX__", MAX.toString())
        web.evaluateJavascript(script, null)
        note("Saving file to Downloads…")
        return true
    }

    private fun saveImage(originalName: String, dataUrl: String) {
        val comma = dataUrl.indexOf(',')
        if (comma < 0 || comma > 80 || dataUrl.length > MAX * 4 / 3 + 256) {
            note("Unsupported or oversized file. Try Chrome.")
            return
        }
        val mime = when (dataUrl.substring(0, comma).lowercase()) {
            "data:image/png;base64" -> "image/png"
            "data:image/jpeg;base64" -> "image/jpeg"
            "data:image/webp;base64" -> "image/webp"
            "data:image/gif;base64" -> "image/gif"
            "data:image/avif;base64" -> "image/avif"
            "data:application/pdf;base64" -> "application/pdf"
            "data:application/zip;base64", "data:application/x-zip-compressed;base64" -> "application/zip"
            "data:application/octet-stream;base64" -> "application/octet-stream"
            "data:application/vnd.openxmlformats-officedocument.wordprocessingml.document;base64" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "data:text/plain;base64" -> "text/plain"
            else -> null
        }
        if (mime == null) { note("Unsupported file format. Try Chrome."); return }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            note("Use Chrome to save files on Android 9 or older.")
            return
        }
        val extension = when (mime) {
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            "image/avif" -> ".avif"
            "application/pdf" -> ".pdf"
            "application/zip" -> ".zip"
            "text/plain" -> ".txt"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
            else -> ".bin"
        }
        val name = originalName.substringBeforeLast('.').ifBlank { "ChatGPT-image" }
            .replace(Regex("[^a-zA-Z0-9._() -]"), "_").take(55)
        val finalName = name + "-" + System.currentTimeMillis() + extension
        Thread {
            var destination: Uri? = null
            try {
                val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
                require(bytes.size <= MAX)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Tong GPT")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                destination = activity.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: error("Insert failed")
                activity.contentResolver.openOutputStream(destination!!)?.use { it.write(bytes) }
                    ?: error("Write failed")
                activity.contentResolver.update(destination!!, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
                notifyUi("Saved to Downloads / Tong GPT")
            } catch (_: Exception) {
                destination?.let {
                    try { activity.contentResolver.delete(it, null, null) } catch (_: Exception) {}
                }
                notifyUi("Could not save the file. Try Chrome.")
            }
        }.start()
    }

    private fun notifyUi(message: String) {
        handler.post { note(message) }
    }
    private fun note(message: String) {
        if (!activity.isDestroyed && !activity.isFinishing)
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }
}
