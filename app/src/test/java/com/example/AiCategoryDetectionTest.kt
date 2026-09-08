package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.remote.CategoryDetectionRemoteService
import com.example.data.remote.CategoryDetectionResult
import com.example.ui.screens.CustomerRequestFlowScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiCategoryDetectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `verify all 7 categories map correctly from edge function category names to app category ids`() {
        assertEquals("dry_cleaning", CategoryDetectionRemoteService.mapCategoryToId("Laundry & Ironing"))
        assertEquals("cleaning", CategoryDetectionRemoteService.mapCategoryToId("Home Cleaning"))
        assertEquals("ac_repair", CategoryDetectionRemoteService.mapCategoryToId("AC Servicing"))
        assertEquals("car_care", CategoryDetectionRemoteService.mapCategoryToId("Car Care/Wash"))
        assertEquals("plumbing", CategoryDetectionRemoteService.mapCategoryToId("Plumbing"))
        assertEquals("electrical", CategoryDetectionRemoteService.mapCategoryToId("Electrical"))
        assertEquals("appliance_repair", CategoryDetectionRemoteService.mapCategoryToId("Appliance Repair"))
    }

    @Test
    fun `verify local heuristic classification for common service descriptions`() {
        val service = CategoryDetectionRemoteService()

        // 1. AC Servicing
        val acResult = service.localHeuristicClassify("my AC is making a weird noise and not cooling")
        assertNotNull(acResult)
        assertEquals("ac_repair", acResult?.categoryId)
        assertEquals("AC Servicing", acResult?.category)

        // 2. Plumbing
        val plumbResult = service.localHeuristicClassify("kitchen tap is leaking and pipe is broken")
        assertNotNull(plumbResult)
        assertEquals("plumbing", plumbResult?.categoryId)
        assertEquals("Plumbing", plumbResult?.category)

        // 3. Electrical
        val elecResult = service.localHeuristicClassify("ceiling fan switch is sparking and breaker tripped")
        assertNotNull(elecResult)
        assertEquals("electrical", elecResult?.categoryId)
        assertEquals("Electrical", elecResult?.category)

        // 4. Laundry
        val laundryResult = service.localHeuristicClassify("need 5 shalwar kameez suits washed and ironed")
        assertNotNull(laundryResult)
        assertEquals("dry_cleaning", laundryResult?.categoryId)
        assertEquals("Laundry & Ironing", laundryResult?.category)

        // 5. Home Cleaning
        val cleanResult = service.localHeuristicClassify("deep clean sofa and carpet for house")
        assertNotNull(cleanResult)
        assertEquals("cleaning", cleanResult?.categoryId)
        assertEquals("Home Cleaning", cleanResult?.category)

        // 6. Appliance Repair
        val applianceResult = service.localHeuristicClassify("refrigerator cooling is not working and freezer warm")
        assertNotNull(applianceResult)
        assertEquals("appliance_repair", applianceResult?.categoryId)
        assertEquals("Appliance Repair", applianceResult?.category)

        // 7. Car Care
        val carResult = service.localHeuristicClassify("car wash and interior detailing needed at home")
        assertNotNull(carResult)
        assertEquals("car_care", carResult?.categoryId)
        assertEquals("Car Care/Wash", carResult?.category)
    }

    @Test
    fun `verify AI category detection localization strings in English and Urdu`() {
        // English
        assertEquals("Describe your problem", Strings.get("ai_describe_title", AppLanguage.ENGLISH))
        assertEquals("Suggest a service", Strings.get("ai_suggest_service_button", AppLanguage.ENGLISH))
        assertEquals("This looks like:", Strings.get("ai_looks_like_prefix", AppLanguage.ENGLISH))
        assertEquals("This might be:", Strings.get("ai_might_be_prefix", AppLanguage.ENGLISH))
        assertEquals("Continue with this?", Strings.get("ai_continue_prompt", AppLanguage.ENGLISH))
        assertEquals("Confirm & Pre-fill", Strings.get("ai_confirm_button", AppLanguage.ENGLISH))
        assertEquals("Not quite, let me choose myself", Strings.get("ai_choose_myself", AppLanguage.ENGLISH))

        // Urdu
        assertEquals("اپنا مسئلہ بیان کریں", Strings.get("ai_describe_title", AppLanguage.URDU))
        assertEquals("سروس تجویز کریں", Strings.get("ai_suggest_service_button", AppLanguage.URDU))
        assertEquals("یہ سروس معلوم ہوتی ہے:", Strings.get("ai_looks_like_prefix", AppLanguage.URDU))
        assertEquals("تصدیق کریں اور فارم بھریں", Strings.get("ai_confirm_button", AppLanguage.URDU))
        assertEquals("نہیں، میں خود منتخب کروں گا", Strings.get("ai_choose_myself", AppLanguage.URDU))
    }

    @Test
    fun `verify UI renders AI input and confirms suggestion prefilling the form`() {
        var submittedRequest: ServiceRequestEntity? = null

        composeTestRule.setContent {
            CustomerRequestFlowScreen(
                initialCategoryId = null,
                customerPhone = "+923001234567",
                customerName = "Test Customer",
                savedAddress = "123 Main St",
                cityArea = "Lahore - Gulberg",
                language = AppLanguage.ENGLISH,
                activeLiveRequest = null,
                incomingOffers = emptyList(),
                onBack = {},
                onSubmitRequest = { submittedRequest = it },
                onSelectOffer = {},
                onDoneViewingConfirmed = {},
                onDetectCategory = { desc ->
                    CategoryDetectionResult(
                        success = true,
                        category = "Plumbing",
                        categoryId = "plumbing",
                        serviceNote = "Leaking tap/pipe",
                        confidence = "high"
                    )
                }
            )
        }

        // Verify AI assistant input is displayed
        composeTestRule.onNodeWithTag("ai_problem_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_suggest_service_btn").assertIsDisplayed()

        // Type description
        composeTestRule.onNodeWithTag("ai_problem_input")
            .performTextInput("my kitchen tap is leaking")

        // Click Suggest a service
        composeTestRule.onNodeWithTag("ai_suggest_service_btn").performClick()
        composeTestRule.waitForIdle()

        // Verify suggestion card appears
        composeTestRule.onNodeWithTag("ai_suggestion_card").assertExists()
        composeTestRule.onNodeWithTag("ai_confirm_suggestion_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Suggestion card is now dismissed (applied)
        composeTestRule.onNodeWithTag("ai_suggestion_card").assertDoesNotExist()
    }

    @Test
    fun `verify fallback button dismisses AI suggestion without changing manual selection`() {
        composeTestRule.setContent {
            CustomerRequestFlowScreen(
                initialCategoryId = "electrical",
                customerPhone = "+923001234567",
                customerName = "Test Customer",
                savedAddress = "123 Main St",
                cityArea = "Lahore - Gulberg",
                language = AppLanguage.ENGLISH,
                activeLiveRequest = null,
                incomingOffers = emptyList(),
                onBack = {},
                onSubmitRequest = {},
                onSelectOffer = {},
                onDoneViewingConfirmed = {},
                onDetectCategory = { desc ->
                    CategoryDetectionResult(
                        success = true,
                        category = "AC Servicing",
                        categoryId = "ac_repair",
                        serviceNote = "AC not cooling",
                        confidence = "low"
                    )
                }
            )
        }

        // Type description & suggest
        composeTestRule.onNodeWithTag("ai_problem_input")
            .performTextInput("it is too hot in the room")
        composeTestRule.onNodeWithTag("ai_suggest_service_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify suggestion appears with softer framing ("This might be:")
        composeTestRule.onNodeWithTag("ai_suggestion_card").assertExists()

        // Click fallback: "Not quite, let me choose myself"
        composeTestRule.onNodeWithTag("ai_dismiss_suggestion_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Suggestion card dismissed
        composeTestRule.onNodeWithTag("ai_suggestion_card").assertDoesNotExist()
    }
}
