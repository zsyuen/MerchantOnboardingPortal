package com.merchant.portal.integration;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.MerchantDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DocumentController.
 * Tests document retrieval endpoint (GET /api/documents/{id}) with real H2 DB.
 */
class DocumentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MerchantDocumentRepository merchantDocumentRepository;

    private String reviewerToken;
    private MerchantDocument testDocument;

    @BeforeEach
    void setUp() {
        reviewerToken = generateToken("reviewer", "reviewer");

        // Seed a document directly into H2
        testDocument = new MerchantDocument();
        testDocument.setFileName("test-image.png");
        testDocument.setContentType("image/png");
        testDocument.setData(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}); // PNG magic bytes
        testDocument = merchantDocumentRepository.save(testDocument);
    }

    // ─── GET DOCUMENT ───────────────────────────────────────────────

    @Test
    void getDocument_shouldReturnBinaryContentWithCorrectHeaders() throws Exception {
        mockMvc.perform(get("/api/documents/" + testDocument.getId())
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(testDocument.getData()));
    }

    @Test
    void getDocument_shouldReturn404ForNonExistentId() throws Exception {
        UUID fakeId = UUID.randomUUID();

        mockMvc.perform(get("/api/documents/" + fakeId)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDocument_shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/documents/" + testDocument.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDocument_shouldServePdfWithCorrectContentType() throws Exception {
        // Seed a PDF document
        MerchantDocument pdfDoc = new MerchantDocument();
        pdfDoc.setFileName("test-file.pdf");
        pdfDoc.setContentType("application/pdf");
        pdfDoc.setData(new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF magic bytes
        pdfDoc = merchantDocumentRepository.save(pdfDoc);

        mockMvc.perform(get("/api/documents/" + pdfDoc.getId())
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(pdfDoc.getData()));
    }
}

