package com.merchant.portal.integration;

import com.merchant.portal.model.Application;
import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.ApplicationRepository;
import com.merchant.portal.repository.MerchantDocumentRepository;
import com.merchant.portal.service.SelfieRetentionScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SelfieRetentionScheduler.
 * Verifies that expired selfie documents are purged from DB and
 * the selfieImage reference in Application is nulled out.
 */
class SelfieRetentionSchedulerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SelfieRetentionScheduler scheduler;

    @Autowired
    private MerchantDocumentRepository merchantDocumentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void purgeExpiredSelfies_shouldDeleteExpiredDocAndClearAppReference() {
        // 1. Create an expired selfie document
        MerchantDocument selfieDoc = new MerchantDocument();
        selfieDoc.setFileName("selfie.jpg");
        selfieDoc.setContentType("image/jpeg");
        selfieDoc.setData(new byte[]{1, 2, 3});
        selfieDoc.setDocumentType("SELFIE");
        selfieDoc.setExpiresAt(LocalDateTime.now().minusDays(1)); // already expired
        selfieDoc = merchantDocumentRepository.save(selfieDoc);

        String docId = selfieDoc.getId().toString();

        // 2. Create an Application that references this selfie
        Application app = createTestApplication();
        app.setSelfieImage(docId);
        app = applicationRepository.save(app);

        // 3. Run the scheduler
        scheduler.purgeExpiredSelfies();

        // 4. Verify the selfie document was deleted
        assertTrue(merchantDocumentRepository.findById(selfieDoc.getId()).isEmpty(),
                "Expired selfie document should be deleted from DB");

        // 5. Verify the application's selfieImage reference was nulled out
        Application updatedApp = applicationRepository.findById(app.getId()).orElseThrow();
        assertNull(updatedApp.getSelfieImage(),
                "Application's selfieImage reference should be null after purge");
    }

    @Test
    void purgeExpiredSelfies_shouldNotDeleteNonExpiredSelfies() {
        // Create a selfie that expires in the future
        MerchantDocument futureDoc = new MerchantDocument();
        futureDoc.setFileName("future-selfie.jpg");
        futureDoc.setContentType("image/jpeg");
        futureDoc.setData(new byte[]{4, 5, 6});
        futureDoc.setDocumentType("SELFIE");
        futureDoc.setExpiresAt(LocalDateTime.now().plusDays(5)); // not yet expired
        futureDoc = merchantDocumentRepository.save(futureDoc);

        // Run the scheduler
        scheduler.purgeExpiredSelfies();

        // Verify it was NOT deleted
        assertTrue(merchantDocumentRepository.findById(futureDoc.getId()).isPresent(),
                "Non-expired selfie should NOT be deleted");
    }

    @Test
    void purgeExpiredSelfies_shouldNotAffectNonSelfieDocuments() {
        // Create an expired document that is NOT a SELFIE type
        MerchantDocument idDoc = new MerchantDocument();
        idDoc.setFileName("id-card.png");
        idDoc.setContentType("image/png");
        idDoc.setData(new byte[]{7, 8, 9});
        idDoc.setDocumentType("ID_FRONT");
        idDoc.setExpiresAt(LocalDateTime.now().minusDays(1)); // expired but not SELFIE
        idDoc = merchantDocumentRepository.save(idDoc);

        // Run the scheduler
        scheduler.purgeExpiredSelfies();

        // Verify it was NOT deleted (scheduler only targets SELFIE type)
        assertTrue(merchantDocumentRepository.findById(idDoc.getId()).isPresent(),
                "Non-SELFIE documents should NOT be deleted even if expired");
    }

    /**
     * Helper to create a minimal valid Application for DB persistence.
     */
    private Application createTestApplication() {
        Application app = new Application();
        app.setReferenceId("SELFIE-TEST-" + System.nanoTime());
        app.setStatus("Pending");
        app.setSubmissionDate(LocalDateTime.now());
        app.setCompanyName("Selfie Test Corp");
        app.setBusinessRegNo("BRN-SELFIE");
        app.setDateOfIncorporation("2020-01-01");
        app.setCountryOfCorporation("MY");
        app.setMerchantNameEn("Selfie EN");
        app.setMerchantNameLocal("Selfie Local");
        app.setTaxId("TAX-SELFIE");
        app.setClassificationOfEntity("LLC");
        app.setAddressLine1("123 Selfie St");
        app.setCity("KL");
        app.setState("KL");
        app.setPostal("50000");
        app.setCountry("MY");
        app.setTelephone1("0123456789");
        app.setEmail("selfie@test.com");
        app.setFirstName("Selfie");
        app.setLastName("Test");
        app.setDateOfBirth("1990-01-01");
        app.setIcPassport("S12345");
        app.setNationality("MY");
        app.setIdUploadFront("uuid-front");
        app.setIdUploadBack("uuid-back");
        app.setPassportPhoto("uuid-passport");
        app.setIndustry("Tech");
        app.setBusinessType("Online");
        app.setNumberOfEmployees(10);
        app.setSchemeRequired("Visa");
        app.setFacilityRequired("Terminal");
        app.setProofOfBusiness("uuid-proof");
        return app;
    }
}

