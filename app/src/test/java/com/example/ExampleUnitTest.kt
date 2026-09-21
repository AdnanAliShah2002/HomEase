package com.example

import com.example.data.remote.OtpRemoteService
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testNormalizePhoneNumber_allVariants() {
    // 11-digit local format with leading zero
    assertEquals("+923325973916", OtpRemoteService.normalizePhone("03325973916"))

    // 10-digit format without leading zero
    assertEquals("+923325973916", OtpRemoteService.normalizePhone("3325973916"))

    // E.164 standard international format
    assertEquals("+923325973916", OtpRemoteService.normalizePhone("+923325973916"))

    // International format without plus
    assertEquals("+923325973916", OtpRemoteService.normalizePhone("923325973916"))

    // Malformed input with redundant zero after country code +920...
    assertEquals("+923325973916", OtpRemoteService.normalizePhone("+9203325973916"))

    // Input with spaces or dashes
    assertEquals("+923325973916", OtpRemoteService.normalizePhone("+92 332-5973916"))
  }
}
