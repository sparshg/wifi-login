package dev.sparshg.bitslogin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Parsers against pages captured from the real BITS Pilani FortiGate. */
class PortalTest {
    private val stub = """<html><body><script language="JavaScript">window.location="https://fw.bits-pilani.ac.in:8090/fgtauth?03332d857d3fe308";</script></body></html>"""
    private val form = """
        <form action="/" method="post">
        <input type="hidden" name="4Tredir" value="http://connectivitycheck.gstatic.com/generate_204">
        <input type="hidden" name="magic" value="07575f2a083d9e7b">
        <input type="hidden" name="" value="">
        <input name="username" id="ft_un" type="text" autocorrect="off" autocapitalize="off">
    """.trimIndent()
    private val loggedIn = """<html><body><script language="JavaScript">window.location="https://fw.bits-pilani.ac.in:8090/keepalive?0a1b2c3d4e5f6a7b";</script></body></html>"""

    @Test
    fun portalUrlOnlyForBitsHosts() {
        assertEquals("https://fw.bits-pilani.ac.in:8090/fgtauth?03332d857d3fe308", Portal.fortiGateUrl(stub))
        assertNull(Portal.fortiGateUrl("""location="https://anyclub.bits-pilani.ac.in:443/fgtauth?ab12"""")) // other BITS servers too
        assertNull(Portal.fortiGateUrl("""location="https://evil.example:8090/fgtauth?03332d857d3fe308""""))
        assertNull(Portal.fortiGateUrl("""location="https://fw.bits-pilani.ac.in.evil.example:8090/fgtauth?ab12""""))
    }

    @Test
    fun sophosPortalOnlyForCampusOrigins() {
        assertEquals("http://172.16.0.30:8090", Portal.sophosOrigin("""location.href="http://172.16.0.30:8090/httpclient.html";""")) // Hyderabad
        assertEquals("https://campnet.bits-goa.ac.in:8090", Portal.sophosOrigin("Location: https://campnet.bits-goa.ac.in:8090/httpclient.html"))
        assertNull(Portal.sophosOrigin("""location.href="http://172.16.0.31:8090/httpclient.html";"""))
        assertNull(Portal.sophosOrigin("""location.href="http://evil.example/httpclient.html";"""))
    }

    @Test
    fun sophosReplies() {
        val xml = { status: String, msg: String -> "<?xml version='1.0' ?><requestresponse><status><![CDATA[$status]]></status><message><![CDATA[$msg]]></message></requestresponse>" }
        assertEquals(Portal.Answered(null, accepted = true), Portal.sophosOutcome(xml("LIVE", "You have successfully logged in")))
        assertEquals(Portal.Answered(null), Portal.sophosOutcome(xml("LOGIN", "Login failed. Invalid user name/password."))) // counts toward the halt
        assertEquals(Portal.NoAnswer, Portal.sophosOutcome(xml("LOGIN", "You have reached Maximum Login Limit."))) // retried later
        assertEquals(Portal.NoAnswer, Portal.sophosOutcome("<html>503 Service Unavailable</html>"))
    }

    @Test
    fun formFields() {
        assertEquals("07575f2a083d9e7b", Portal.field("magic", form))
        assertEquals("http://connectivitycheck.gstatic.com/generate_204", Portal.field("4Tredir", form))
    }

    @Test
    fun keepaliveNeverLogout() {
        assertEquals("https://fw.bits-pilani.ac.in:8090/keepalive?0a1b2c3d4e5f6a7b", Portal.keepaliveUrl(loggedIn))
        assertNull(Portal.keepaliveUrl("""<a href="https://fw.bits-pilani.ac.in:8090/logout?0a1b2c3d4e5f6a7b">"""))
    }
}
