package QRAuth.discordqr

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class DiscordQrAuthClientLiveTest {
    @Test
    fun discordIssuesQrFingerprint() {
        assumeTrue(
            "Set WEARCORD_LIVE_QR_TEST=1 to run the live Discord handshake",
            System.getenv("WEARCORD_LIVE_QR_TEST") == "1",
        )
        val errors = CopyOnWriteArrayList<Throwable>()
        val client = DiscordQrAuthClient { error -> errors += error }
        try {
            client.start()
            val fingerprint = client.codeFuture.get(30, TimeUnit.SECONDS)
            assertTrue("QR fingerprint was blank", fingerprint.isNotBlank())
            assertTrue("Handshake reported: $errors", errors.isEmpty())
        } finally {
            client.close()
        }
    }
}
