package com.merchant.portal.integration;

import com.merchant.portal.model.Application;
import com.merchant.portal.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ApplicationController.
 * Tests GET/PUT/DELETE endpoints with real H2 DB.
 * @Transactional ensures each test rolls back, preventing unique constraint clashes.
 */
@Transactional
class ApplicationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    private String reviewerToken;
    private Application testApp;

    @BeforeEach
    void setUp() {
        reviewerToken = generateToken("reviewer", "reviewer");

        // Seed a test application with a unique reference ID
        testApp = new Application();
        testApp.setReferenceId("IT-" + System.nanoTime());
        testApp.setStatus("Pending");
        testApp.setSubmissionDate(LocalDateTime.now());
        testApp.setCompanyName("Integration Corp");
        testApp.setBusinessRegNo("BRN-001");
        testApp.setDateOfIncorporation("2020-01-01");
        testApp.setCountryOfCorporation("MY");
        testApp.setMerchantNameEn("Integration Corp EN");
        testApp.setMerchantNameLocal("Integration Corp Local");
        testApp.setTaxId("TAX001");
        testApp.setClassificationOfEntity("LLC");
        testApp.setAddressLine1("123 Test Street");
        testApp.setCity("Kuala Lumpur");
        testApp.setState("KL");
        testApp.setPostal("50000");
        testApp.setCountry("MY");
        testApp.setTelephone1("0123456789");
        testApp.setEmail("integration@test.com");
        testApp.setFirstName("John");
        testApp.setLastName("Doe");
        testApp.setDateOfBirth("1990-01-01");
        testApp.setIcPassport("A12345678");
        testApp.setNationality("Malaysian");
        testApp.setIdUploadFront("uuid-front");
        testApp.setIdUploadBack("uuid-back");
        testApp.setPassportPhoto("uuid-passport");
        testApp.setIndustry("Technology");
        testApp.setBusinessType("E-Commerce");
        testApp.setNumberOfEmployees(50);
        testApp.setSchemeRequired("Visa");
        testApp.setFacilityRequired("POS Terminal");
        testApp.setProofOfBusiness("uuid-proof");
        testApp.setFacialSimilarityScore(0.85);
        testApp.setConfidenceLevel("High");
        testApp.setVerificationStatus("Pending");

        testApp = applicationRepository.saveAndFlush(testApp);
    }

    // ─── GET ALL APPLICATIONS ───────────────────────────────────────

    @Test
    void getAllApplications_shouldReturnListWithAuth() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].companyName", hasItem("Integration Corp")));
    }

    @Test
    void getAllApplications_shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET BY REFERENCE ID (PUBLIC) ───────────────────────────────

    @Test
    void getByRef_shouldReturnApplicationWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/applications/ref/" + testApp.getReferenceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Integration Corp"))
                .andExpect(jsonPath("$.status").value("Pending"));
    }

    @Test
    void getByRef_shouldReturn404ForInvalidRef() throws Exception {
        mockMvc.perform(get("/api/applications/ref/INVALID-REF"))
                .andExpect(status().isNotFound());
    }

    // ─── GET BY STATUS ──────────────────────────────────────────────

    @Test
    void getByStatus_shouldReturnFilteredList() throws Exception {
        mockMvc.perform(get("/api/applications/by-status")
                        .param("status", "Pending")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].status", everyItem(equalToIgnoringCase("Pending"))));
    }

    @Test
    void getByStatus_shouldReturnEmptyListForNoMatch() throws Exception {
        mockMvc.perform(get("/api/applications/by-status")
                        .param("status", "NonExistentStatus")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─── GET BY ID ──────────────────────────────────────────────────

    @Test
    void getById_shouldReturnApplicationWithAuth() throws Exception {
        mockMvc.perform(get("/api/applications/" + testApp.getId())
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testApp.getId()))
                .andExpect(jsonPath("$.companyName").value("Integration Corp"));
    }

    @Test
    void getById_shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(get("/api/applications/99999")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/applications/" + testApp.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ─── UPDATE STATUS ──────────────────────────────────────────────

    @Test
    void updateStatus_shouldUpdateAndPersist() throws Exception {
        String json = """
                { "status": "Approved" }
                """;

        mockMvc.perform(put("/api/applications/" + testApp.getId() + "/status")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Approved"));

        Application updated = applicationRepository.findById(testApp.getId()).orElseThrow();
        assertEquals("Approved", updated.getStatus());
    }

    @Test
    void updateStatus_shouldReturnBadRequestForMissingStatus() throws Exception {
        String json = "{}";

        mockMvc.perform(put("/api/applications/" + testApp.getId() + "/status")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturnBadRequestForEmptyStatus() throws Exception {
        String json = """
                { "status": "   " }
                """;

        mockMvc.perform(put("/api/applications/" + testApp.getId() + "/status")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturn404ForNonExistentApp() throws Exception {
        String json = """
                { "status": "Approved" }
                """;

        mockMvc.perform(put("/api/applications/99999/status")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE ─────────────────────────────────────────────────────

    @Test
    void deleteApplication_shouldRemoveFromDatabase() throws Exception {
        Long id = testApp.getId();

        mockMvc.perform(delete("/api/applications/" + id)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNoContent());

        assertTrue(applicationRepository.findById(id).isEmpty());
    }

    @Test
    void deleteApplication_shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(delete("/api/applications/99999")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteApplication_shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(delete("/api/applications/" + testApp.getId()))
                .andExpect(status().isUnauthorized());
    }
}
