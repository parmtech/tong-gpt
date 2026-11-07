package au.parmtech.tonggpt

import java.net.URI
import java.util.Locale

/** Prevent embedded WebView from becoming an unrestricted general-purpose browser. */
internal object UrlPolicy {
    fun isInternal(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        return host == "chatgpt.com" ||
            host.endsWith(".chatgpt.com") ||
            host == "chat.openai.com" ||
            host == "auth.openai.com" ||
            host == "auth0.openai.com" ||
            host == "login.openai.com"
    }

    fun isTrustedChatGptOrigin(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        return host == "chatgpt.com" || host.endsWith(".chatgpt.com")
    }

    /**
     * In-app authenticated downloads are limited to OpenAI and ChatGPT-hosted
     * resources. Other HTTPS links remain ordinary external downloads/links.
     */
    fun isTrustedDownloadOrigin(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            uri.rawUserInfo != null || uri.port !in listOf(-1, 443)) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        return host == "chatgpt.com" || host.endsWith(".chatgpt.com") ||
            host == "openai.com" || host.endsWith(".openai.com") ||
            host == "oaiusercontent.com" || host.endsWith(".oaiusercontent.com")
    }

    fun canLaunchExternally(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        return when (uri.scheme?.lowercase(Locale.ROOT)) {
            "https", "http", "mailto", "tel" -> true
            else -> false
        }
    }
}
