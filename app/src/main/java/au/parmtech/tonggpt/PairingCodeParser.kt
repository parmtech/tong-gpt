package au.parmtech.tonggpt

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * No secrets or full QR payloads are logged.
 *
 * Official ChatGPT Desktop codes are NOT Tong GPT companion codes. Their pairing
 * authentication belongs to the official ChatGPT mobile app.
 */
internal object PairingCodeParser {
    sealed class Parsed {
        data class Companion(val endpoint: String, val token: String, val desktopUrl: String?) : Parsed()
        data class Official(val link: String) : Parsed()
        data object Unsupported : Parsed()
    }

    fun parse(content: String): Parsed {
        if (content.length !in 5..8192) return Parsed.Unsupported
        val code = content.trim()
        val uri = try { URI(code) } catch (_: Exception) { return Parsed.Unsupported }
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return Parsed.Unsupported
        val host = uri.host?.lowercase(Locale.ROOT)

        if (scheme == "tonggpt" && host == "pair" && uri.rawUserInfo == null &&
            uri.rawFragment == null && uri.rawQuery != null && uri.rawQuery.length <= 4096) {
            val pairs = mutableMapOf<String, String>()
            for (part in uri.rawQuery.split("&")) {
                val split = part.indexOf('=')
                if (split < 1) return Parsed.Unsupported
                val key = decode(part.substring(0, split)) ?: return Parsed.Unsupported
                val value = decode(part.substring(split + 1)) ?: return Parsed.Unsupported
                if (pairs.containsKey(key)) return Parsed.Unsupported
                pairs[key] = value
            }
            if (pairs["v"] != "1") return Parsed.Unsupported
            val endpoint = pairs["endpoint"] ?: return Parsed.Unsupported
            val token = pairs["token"] ?: return Parsed.Unsupported
            if (!validWssEndpoint(endpoint) || token.length !in 32..512 ||
                token.any { it.isWhitespace() || it.isISOControl() }) return Parsed.Unsupported
            val desktop = pairs["desktop"]?.takeIf { it.isNotEmpty() }
            if (desktop != null && !validHttpsEndpoint(desktop)) return Parsed.Unsupported
            return Parsed.Companion(endpoint, token, desktop)
        }

        // Keep the original official link in memory only; a separate confirmation
        // screen must precede opening it in the official ChatGPT mobile client.
        if ((scheme == "https" && (host == "chatgpt.com" ||
                host?.endsWith(".chatgpt.com") == true ||
                host == "openai.com" ||
                host?.endsWith(".openai.com") == true) &&
                uri.rawUserInfo == null && uri.port in listOf(-1, 443)) ||
                scheme == "chatgpt") {
            return Parsed.Official(code)
        }
        return Parsed.Unsupported
    }

    private fun validWssEndpoint(raw: String): Boolean {
        val uri = try { URI(raw) } catch (_: Exception) { return false }
        return uri.scheme.equals("wss", ignoreCase = true) &&
            validHost(uri.host) && uri.rawUserInfo == null && uri.rawFragment == null &&
            uri.rawQuery == null && uri.port in -1..65535 && uri.port != 0
    }

    private fun validHttpsEndpoint(raw: String): Boolean {
        val uri = try { URI(raw) } catch (_: Exception) { return false }
        return uri.scheme.equals("https", ignoreCase = true) &&
            validHost(uri.host) && uri.rawUserInfo == null && uri.rawFragment == null &&
            uri.rawQuery == null && uri.port in -1..65535 && uri.port != 0
    }

    private fun validHost(raw: String?): Boolean {
        val host = raw?.lowercase(Locale.ROOT) ?: return false
        if (!host.contains('.') || host.length > 253) return false
        return host != "localhost" && !host.endsWith(".local") && !host.endsWith(".internal") &&
            !host.startsWith("127.") && !host.startsWith("10.") &&
            !host.startsWith("192.168.") && !host.startsWith("169.254.")
    }

    private fun decode(raw: String): String? = try {
        URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    } catch (_: Exception) { null }
}
