package au.parmtech.tonggpt

import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Native fallback for user-selected ChatGPT artifact download links.
 * Original implementation informed by WebGPT MIT project's HTTPS download approach.
 * No HTML/page-script scraping. Cookies are sent ONLY to trusted ChatGPT origins.
 * Redirects are manual to prevent accidentally forwarding session cookies.
 */
internal class NativeFileDownloader(private val activity: Activity) {
    companion object {
        private const val MAX_BYTES = 150L * 1024 * 1024
        private const val MAX_REDIRECTS = 5
    }

    fun download(url: String, suggested: String, agent: String): Boolean {
        if (!UrlPolicy.isTrustedDownloadOrigin(url)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        message("Downloading file…")
        thread(name = "tong-gpt-https-download") {
            var conn: HttpURLConnection? = null
            var destination: Uri? = null
            try {
                var current = URL(url)
                var hops = 0
                while (true) {
                    require(current.protocol == "https" && publicHost(current.host)) {
                        "Only secure HTTPS downloads are supported"
                    }
                    conn?.disconnect()
                    conn = (current.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 15_000
                        readTimeout = 90_000
                        setRequestProperty("User-Agent", agent)
                        setRequestProperty("Accept", "*/*")
                        setRequestProperty("Accept-Encoding", "identity")
                        setRequestProperty("Referer", "https://chatgpt.com/")
                        if (UrlPolicy.isTrustedChatGptOrigin(current.toString())) {
                            CookieManager.getInstance().getCookie(current.toString())?.let {
                                setRequestProperty("Cookie", it)
                            }
                        }
                    }
                    val response = conn!!.responseCode
                    if (response in listOf(301, 302, 303, 307, 308)) {
                        if (++hops > MAX_REDIRECTS) throw IOException("Too many redirects")
                        val location = conn!!.getHeaderField("Location")
                            ?: throw IOException("Missing redirect destination")
                        current = URL(current, location)
                        continue
                    }
                    if (response == 401 || response == 403) {
                        throw IOException("Download authorization expired. Sign in again or use Chrome.")
                    }
                    if (response !in 200..299) {
                        throw IOException("Server returned HTTP " + response)
                    }
                    break
                }

                val length = conn!!.contentLengthLong
                if (length > MAX_BYTES) throw IOException("File exceeds the 150 MB size limit")
                val mime = conn!!.contentType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
                    ?: "application/octet-stream"
                if (mime == "text/html" || mime == "application/xhtml+xml") {
                    throw IOException("Server returned a login/error page instead of a file")
                }
                val disposition = conn!!.getHeaderField("Content-Disposition")
                val guessed = URLUtil.guessFileName(current.toString(), disposition, mime)
                val fileName = fileName(guessed.takeIf { it.isNotBlank() } ?: suggested, mime)

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Tong GPT")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                destination = activity.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: throw IOException("Could not create file in Downloads")

                val stream = activity.contentResolver.openOutputStream(destination!!)
                    ?: throw IOException("Cannot open target file")
                var total = 0L
                stream.use { output ->
                    conn!!.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_BYTES) throw IOException("Download exceeds 150 MB")
                            output.write(buffer, 0, count)
                        }
                    }
                }
                require(total > 0) { "Empty download" }
                activity.contentResolver.update(destination!!, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
                message("Saved: Downloads / Tong GPT / " + fileName)
            } catch (e: Exception) {
                destination?.let {
                    try { activity.contentResolver.delete(it, null, null) } catch (_: Exception) { }
                }
                message("Download failed: " + (e.message ?: "Try Chrome").take(140))
            } finally {
                conn?.disconnect()
            }
        }
        return true
    }

    private fun fileName(input: String, mime: String): String {
        val clean = input.substringAfterLast('/').substringBefore('?')
            .replace(Regex("[^a-zA-Z0-9._() -]"), "_")
            .take(90).ifBlank { "ChatGPT-file" }
        val ext = when (mime) {
            "image/png" -> ".png"
            "image/jpeg" -> ".jpg"
            "image/webp" -> ".webp"
            "application/zip", "application/x-zip-compressed" -> ".zip"
            "application/pdf" -> ".pdf"
            else -> ""
        }
        val stem = if (ext.isNotEmpty() && clean.endsWith(ext, ignoreCase = true)) clean
            else if (ext.isNotEmpty() && clean.substringAfterLast('.', "").length in 1..4)
                clean.substringBeforeLast('.') + ext
            else clean + ext
        return System.currentTimeMillis().toString() + "-" + stem
    }

    private fun publicHost(host: String): Boolean {
        val h = host.lowercase(Locale.ROOT)
        if (h.isBlank() || h == "localhost" || h.endsWith(".local") || h.endsWith(".internal")) return false
        if (h.startsWith("127.") || h.startsWith("10.") || h.startsWith("192.168.") ||
            h.startsWith("169.254.") || h == "::1" || h == "0.0.0.0") return false
        if (h.startsWith("172.")) {
            val part = h.split('.').getOrNull(1)?.toIntOrNull()
            if (part != null && part in 16..31) return false
        }
        return h.contains('.')
    }

    private fun message(value: String) = activity.runOnUiThread {
        if (!activity.isFinishing && !activity.isDestroyed) {
            Toast.makeText(activity, value, Toast.LENGTH_LONG).show()
        }
    }
}
