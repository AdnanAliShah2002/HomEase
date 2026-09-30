package com.example

import com.example.service.AgoraTokenBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgoraTokenBuilderTest {

    @Test
    fun testBuildTokenGeneratesValidFormat() {
        val appId = "3014ea1db0a84f5ca24477ad98c0f495"
        val certificate = "bbd579b06ed04b1a9e07fd2cdd694996"
        val channelName = "test_channel"

        val token = AgoraTokenBuilder.buildToken(
            appId = appId,
            appCertificate = certificate,
            channelName = channelName,
            uid = 0
        )

        // Version: 006 (3 chars) + AppID (32 chars) + Base64 content
        assertTrue("Token must start with 006", token.startsWith("006"))
        assertEquals("App ID in token must match", appId, token.substring(3, 35))
        assertTrue("Token length must be reasonable", token.length > 50)
    }
}
