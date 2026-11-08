package au.parmtech.tonggpt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCodeParserTest {
    @Test fun companionQrParsesSecureWssAndToken() {
        val token = "x".repeat(48)
        val payload = "tonggpt://pair?v=1&endpoint=wss%3A%2F%2Fpc.tailnet.ts.net%3A8443%2F&token=" + token
        val result = PairingCodeParser.parse(payload)
        assertTrue(result is PairingCodeParser.Parsed.Companion)
        val pair = result as PairingCodeParser.Parsed.Companion
        assertEquals("wss://pc.tailnet.ts.net:8443/", pair.endpoint)
        assertEquals(token, pair.token)
    }

    @Test fun rejectsUnencryptedAndWeakTokens() {
        val weak = "x".repeat(8)
        assertTrue(PairingCodeParser.parse(
            "tonggpt://pair?v=1&endpoint=ws%3A%2F%2Fpc.tailnet.ts.net&token=" + weak
        ) is PairingCodeParser.Parsed.Unsupported)
        assertTrue(PairingCodeParser.parse(
            "tonggpt://pair?v=1&endpoint=wss%3A%2F%2Fpc.tailnet.ts.net&token=" + weak
        ) is PairingCodeParser.Parsed.Unsupported)
    }

    @Test fun detectsOfficialCodeWithoutTreatingItAsCompanion() {
        assertTrue(PairingCodeParser.parse(
            "https://chatgpt.com/codex/remote/connect?token=test"
        ) is PairingCodeParser.Parsed.Official)
    }

    @Test fun rejectsUntrustedQrDestinations() {
        assertTrue(PairingCodeParser.parse(
            "https://chatgpt.com.evil.example/pair"
        ) is PairingCodeParser.Parsed.Unsupported)
        assertTrue(PairingCodeParser.parse("javascript:alert(1)") is PairingCodeParser.Parsed.Unsupported)
        assertTrue(PairingCodeParser.parse(
            "tonggpt://pair?v=1&endpoint=wss%3A%2F%2F127.0.0.1%3A4500&token=" + "y".repeat(48)
        ) is PairingCodeParser.Parsed.Unsupported)
    }
}
