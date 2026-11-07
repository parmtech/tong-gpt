package au.parmtech.tonggpt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {
    @Test fun permitsChatGpt() {
        assertTrue(UrlPolicy.isInternal("https://chatgpt.com/"))
        assertTrue(UrlPolicy.isInternal("https://chatgpt.com/c/123"))
        assertTrue(UrlPolicy.isInternal("https://auth.openai.com/authorize"))
        assertTrue(UrlPolicy.isInternal("https://chat.openai.com/"))
        assertTrue(UrlPolicy.isInternal("https://login.openai.com/"))
    }
    @Test fun rejectsLookalikeAndUnsafeDestinations() {
        assertFalse(UrlPolicy.isInternal("https://chatgpt.com.evil.example/"))
        assertFalse(UrlPolicy.isInternal("https://fakechatgpt.com/"))
        assertFalse(UrlPolicy.isInternal("http://chatgpt.com/"))
        assertFalse(UrlPolicy.isInternal("https://chatgpt.com:444/"))
        assertFalse(UrlPolicy.isInternal("https://chatgpt.com@evil.example/"))
        assertFalse(UrlPolicy.isInternal("javascript:alert(1)"))
        assertFalse(UrlPolicy.isInternal("https://accounts.google.com/"))
    }
    @Test fun micForChatGptOnly() {
        assertTrue(UrlPolicy.isTrustedChatGptOrigin("https://chatgpt.com/"))
        assertTrue(UrlPolicy.isTrustedChatGptOrigin("https://voice.chatgpt.com/"))
        assertFalse(UrlPolicy.isTrustedChatGptOrigin("https://chatgpt.com.evil.example/"))
        assertFalse(UrlPolicy.isTrustedChatGptOrigin("https://auth.openai.com/"))
    }
    @Test fun supportedExternalSchemesOnly() {
        assertTrue(UrlPolicy.canLaunchExternally("https://example.com"))
        assertTrue(UrlPolicy.canLaunchExternally("mailto:user@example.com"))
        assertFalse(UrlPolicy.canLaunchExternally("intent://pay"))
        assertFalse(UrlPolicy.canLaunchExternally("file:///etc/passwd"))
    }
}
