package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.db.ServiceRequestEntity
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.ServiceCatalog
import com.example.data.repository.HomeaseRepository
import com.example.util.LocationHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: HomeaseRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = HomeaseRepository(db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HomEase", appName)
  }

  @Test
  fun `verify bilingual strings dictionary`() {
    val enTitle = Strings.get("role_title", AppLanguage.ENGLISH)
    val urTitle = Strings.get("role_title", AppLanguage.URDU)
    assertEquals("How do you want to use HomEase?", enTitle)
    assertTrue(urTitle.isNotBlank())
  }

  @Test
  fun `verify service catalog categories exist`() {
    val categories = ServiceCatalog.categories
    assertTrue(categories.isNotEmpty())
    assertNotNull(categories.find { it.id == "plumbing" })
    assertNotNull(categories.find { it.id == "electrical" })
    assertNotNull(categories.find { it.id == "carpentry" })
  }

  @Test
  fun `verify end-to-end job completion and rating recalculation`() = runBlocking {
    val providerPhone = "+923001234567"
    val customerPhone = "+923007654321"

    val provider = UserEntity(
        phone = providerPhone,
        role = "PROVIDER",
        name = "Ustad Tariq",
        cityArea = "Lahore - DHA",
        status = "APPROVED",
        avgRating = 0.0,
        totalJobs = 0
    )
    repository.saveUser(provider)

    val request = ServiceRequestEntity(
        id = 101L,
        customerPhone = customerPhone,
        customerName = "Ali Raza",
        categoryId = "plumbing",
        categoryTitle = "Plumbing",
        serviceTitle = "Kitchen Tap Leaking",
        description = "Leaking fixture",
        cityArea = "Lahore - DHA",
        fullAddress = "Sector Y, Phase 3, DHA",
        budgetRs = 1500,
        status = "ACCEPTED",
        selectedProviderPhone = providerPhone,
        selectedProviderName = "Ustad Tariq",
        agreedPriceRs = 1500
    )
    db.serviceRequestDao().insertRequest(request)

    // Step 1: Provider marks job completed -> enters AWAITING_CUSTOMER_CONFIRMATION
    repository.markJobAwaitingConfirmation(101L)
    val afterProviderDone = db.serviceRequestDao().getRequestById(101L)
    assertEquals("AWAITING_CUSTOMER_CONFIRMATION", afterProviderDone?.status)

    // Step 2: Customer rates with 5 stars
    repository.completeJobWithRating(101L, rating = 5, comment = "Excellent work on time!")
    val completedReq = db.serviceRequestDao().getRequestById(101L)
    assertEquals("COMPLETED", completedReq?.status)
    assertEquals(5, completedReq?.ratingGiven)
    assertEquals("Excellent work on time!", completedReq?.ratingComment)

    // Step 3: Verify provider aggregates were automatically recalculated
    val updatedProvider = repository.getUser(providerPhone)
    assertNotNull(updatedProvider)
    assertEquals(5.0, updatedProvider!!.avgRating, 0.01)
    assertEquals(1, updatedProvider.totalJobs)

    // Step 4: Second job with 4 stars -> verify running average becomes 4.5 and jobs = 2
    val request2 = ServiceRequestEntity(
        id = 102L,
        customerPhone = customerPhone,
        customerName = "Ali Raza",
        categoryId = "electrical",
        categoryTitle = "Electrical",
        serviceTitle = "Switchboard Wiring",
        description = "Sparking board",
        cityArea = "Lahore - DHA",
        fullAddress = "Sector Y, Phase 3, DHA",
        budgetRs = 2500,
        status = "ACCEPTED",
        selectedProviderPhone = providerPhone,
        selectedProviderName = "Ustad Tariq",
        agreedPriceRs = 2500
    )
    db.serviceRequestDao().insertRequest(request2)
    repository.completeJobWithRating(102L, rating = 4, comment = "Good job")

    val providerAfterJob2 = repository.getUser(providerPhone)
    assertEquals(4.5, providerAfterJob2!!.avgRating, 0.01)
    assertEquals(2, providerAfterJob2.totalJobs)
  }

  @Test
  fun `verify auto-completion without rating`() = runBlocking {
    val providerPhone = "+923009998888"
    val request = ServiceRequestEntity(
        id = 201L,
        customerPhone = "+923001112222",
        customerName = "Bilal",
        categoryId = "cleaning",
        categoryTitle = "Cleaning",
        serviceTitle = "Carpet Deep Clean",
        description = "Living room carpet",
        cityArea = "Lahore - Gulberg",
        fullAddress = "House 10, Gulberg 2",
        budgetRs = 3000,
        status = "AWAITING_CUSTOMER_CONFIRMATION",
        selectedProviderPhone = providerPhone,
        selectedProviderName = "CleanPro",
        agreedPriceRs = 3000
    )
    db.serviceRequestDao().insertRequest(request)

    repository.autoCompleteJobWithoutRating(201L)
    val completedReq = db.serviceRequestDao().getRequestById(201L)
    assertEquals("COMPLETED", completedReq?.status)
    assertNull(completedReq?.ratingGiven)
  }

  @Test
  fun `verify report issue saves category and description`() = runBlocking {
    val request = ServiceRequestEntity(
        id = 301L,
        customerPhone = "+923001112222",
        customerName = "Bilal",
        categoryId = "carpentry",
        categoryTitle = "Carpentry",
        serviceTitle = "Door Lock Fix",
        description = "Main door lock",
        cityArea = "Lahore - Gulberg",
        fullAddress = "House 10, Gulberg 2",
        budgetRs = 1000,
        status = "AWAITING_CUSTOMER_CONFIRMATION"
    )
    db.serviceRequestDao().insertRequest(request)

    repository.reportJobIssue(301L, "POOR_QUALITY", "Door handle is loose and still wobbling")
    val updated = db.serviceRequestDao().getRequestById(301L)
    assertEquals("POOR_QUALITY", updated?.issueCategory)
    assertEquals("Door handle is loose and still wobbling", updated?.issueDescription)
  }

  @Test
  fun `verify service_categories table seeded with unified pricing model`() = runBlocking {
    repository.seedCategoriesIfEmpty()
    val allCategories = repository.getAllServiceCategories()
    assertTrue(allCategories.isNotEmpty())

    val plumbing = allCategories.find { it.id == "plumbing" }
    assertNotNull(plumbing)
    assertTrue(plumbing!!.isEstimateOnly)
    assertEquals(300, plumbing.priceMin)
    assertEquals(2000, plumbing.priceMax)

    val laundry = allCategories.find { it.id == "dry_cleaning" }
    assertNotNull(laundry)
    assertEquals(false, laundry!!.isEstimateOnly)
    assertEquals(40, laundry.priceMin)
    assertEquals(150, laundry.priceMax)

    val electrical = allCategories.find { it.id == "electrical" }
    assertNotNull(electrical)
    assertTrue(electrical!!.isEstimateOnly)
    assertEquals(300, electrical.priceMin)
    assertEquals(2000, electrical.priceMax)

    val appliance = allCategories.find { it.id == "appliance_repair" }
    assertNotNull(appliance)
    assertTrue(appliance!!.isEstimateOnly)
    assertEquals(400, appliance.priceMin)
    assertEquals(2500, appliance.priceMax)
  }

  @Test
  fun `verify unified customer asking price and request submission`() = runBlocking {
    val req = ServiceRequestEntity(
        id = 401L,
        customerPhone = "+923001239999",
        customerName = "Fatima Noor",
        categoryId = "plumbing",
        categoryTitle = "Plumbing",
        serviceTitle = "Kitchen Pipe Leak Repair",
        description = "Pipe under the kitchen sink is leaking heavily",
        cityArea = "Lahore - Gulberg",
        fullAddress = "House 12, Block J, Gulberg III",
        budgetRs = 800,
        customerAskingPrice = 800,
        status = "SEARCHING"
    )
    val savedId = repository.createServiceRequest(req)
    val fetched = db.serviceRequestDao().getRequestById(savedId)
    assertNotNull(fetched)
    assertEquals(800, fetched?.customerAskingPrice)
    assertEquals(800, fetched?.budgetRs)
    assertEquals("SEARCHING", fetched?.status)
  }

  @Test
  fun `verify customer selects offer and expires other competing offers`() = runBlocking {
    val req = ServiceRequestEntity(
        id = 501L,
        customerPhone = "+923001239999",
        customerName = "Fatima Noor",
        categoryId = "electrical",
        categoryTitle = "Electrical",
        serviceTitle = "Ceiling Fan Repair",
        description = "Fan regulator and capacitor replacement",
        cityArea = "Lahore - Gulberg",
        fullAddress = "House 12, Block J, Gulberg III",
        budgetRs = 1000,
        customerAskingPrice = 1000,
        status = "SEARCHING"
    )
    db.serviceRequestDao().insertRequest(req)

    val offer1 = com.example.data.db.JobOfferEntity(
        id = 601L,
        requestId = 501L,
        providerPhone = "+923001111111",
        providerName = "Electrician Hamza",
        providerRating = 4.8,
        distanceKm = 1.2,
        counterPriceRs = 1000,
        offerPriceRs = 1000,
        offerNote = "Accepted at your asking price",
        status = "pending"
    )
    val offer2 = com.example.data.db.JobOfferEntity(
        id = 602L,
        requestId = 501L,
        providerPhone = "+923002222222",
        providerName = "Master Bilal",
        providerRating = 4.9,
        distanceKm = 2.0,
        counterPriceRs = 1200,
        offerPriceRs = 1200,
        offerNote = "Includes spare capacitor check",
        status = "pending"
    )
    db.jobOfferDao().insertOffer(offer1)
    db.jobOfferDao().insertOffer(offer2)

    // Customer accepts offer 1
    repository.customerSelectOffer(offer1)

    // Verify offer 1 is accepted
    val updatedOffer1 = db.jobOfferDao().getOfferById(601L)
    assertEquals("accepted", updatedOffer1?.status)

    // Verify offer 2 is automatically expired
    val updatedOffer2 = db.jobOfferDao().getOfferById(602L)
    assertEquals("expired", updatedOffer2?.status)

    // Verify request is updated to ACCEPTED with agreed price
    val updatedReq = db.serviceRequestDao().getRequestById(501L)
    assertEquals("ACCEPTED", updatedReq?.status)
    assertEquals(1000, updatedReq?.agreedPriceRs)
    assertEquals("Electrician Hamza", updatedReq?.selectedProviderName)
    assertEquals("+923001111111", updatedReq?.selectedProviderPhone)
  }

  @Test
  fun `verify provider counter offer with inspection quote note`() = runBlocking {
    val req = ServiceRequestEntity(
        id = 701L,
        customerPhone = "+923001239999",
        customerName = "Zainab",
        categoryId = "appliance",
        categoryTitle = "Appliance Repair",
        serviceTitle = "Refrigerator Cooling Issue",
        description = "Compressor running but not cooling",
        cityArea = "Lahore - Gulberg",
        fullAddress = "House 5, Gulberg",
        budgetRs = 1500,
        customerAskingPrice = 1500,
        status = "SEARCHING"
    )
    db.serviceRequestDao().insertRequest(req)

    // Provider submits inspection counter offer
    repository.submitProviderCounter(
        requestId = 701L,
        providerPhone = "+923008887777",
        providerName = "Cooling Expert Aslam",
        counterPrice = 300,
        note = "Rs 300 to inspect and quote"
    )

    val offers = db.jobOfferDao().getOffersForRequest(701L)
    assertEquals(1, offers.size)
    val offer = offers.first()
    assertEquals(300, offer.counterPriceRs)
    assertEquals(300, offer.offerPriceRs)
    assertEquals("Rs 300 to inspect and quote", offer.offerNote)
    assertEquals("pending", offer.status)
  }

  @Test
  fun `verify laundry and request flow localization keys resolve to human readable text`() {
    // 1. Laundry service type labels
    assertEquals("Wash & Iron", Strings.get("laundry_wash_and_iron", AppLanguage.ENGLISH))
    assertEquals("دھلائی اور استری", Strings.get("laundry_wash_and_iron", AppLanguage.URDU))

    assertEquals("Ironing Only", Strings.get("laundry_iron_only", AppLanguage.ENGLISH))
    assertEquals("صرف استری", Strings.get("laundry_iron_only", AppLanguage.URDU))

    assertEquals("Wash Only", Strings.get("laundry_wash_only", AppLanguage.ENGLISH))
    assertEquals("صرف دھلائی", Strings.get("laundry_wash_only", AppLanguage.URDU))

    // 2. Suit count & hint
    assertEquals("Number of Suits", Strings.get("laundry_suit_count", AppLanguage.ENGLISH))
    assertEquals("سوٹ کی تعداد", Strings.get("laundry_suit_count", AppLanguage.URDU))

    assertEquals("Number of Shalwar Kameez / Pants & Shirts", Strings.get("laundry_items_hint", AppLanguage.ENGLISH))
    assertEquals("شلوار قمیض / پینٹ شرٹ کی تعداد", Strings.get("laundry_items_hint", AppLanguage.URDU))

    // 3. Pricing & estimate labels
    assertEquals("Typical Price Range", Strings.get("reference_price_title", AppLanguage.ENGLISH))
    assertEquals("Suggested", Strings.get("suggested_price_chip", AppLanguage.ENGLISH))
    assertTrue(Strings.get("customer_asking_price_desc", AppLanguage.ENGLISH).contains("willing to pay"))
    assertTrue(Strings.get("estimate_only_explanation", AppLanguage.ENGLISH).contains("inspection"))

    // 4. All ServiceCatalog categories have valid localized display names in both languages
    ServiceCatalog.categories.forEach { cat ->
      val enName = Strings.get(cat.nameKey, AppLanguage.ENGLISH)
      val urName = Strings.get(cat.nameKey, AppLanguage.URDU)
      assertTrue(enName.isNotBlank())
      assertTrue(!enName.contains("_")) // No raw snake_case
      assertTrue(urName.isNotBlank())
    }

    // 5. Fallback formatting converts any unknown key to formatted Title Case, never raw snake_case
    val fallback = Strings.get("unknown_sample_service_key", AppLanguage.ENGLISH)
    assertEquals("Unknown Sample Service Key", fallback)
  }
}
