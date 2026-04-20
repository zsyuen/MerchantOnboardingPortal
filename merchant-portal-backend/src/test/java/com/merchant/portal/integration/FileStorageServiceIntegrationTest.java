package com.merchant.portal.integration;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.MerchantDocumentRepository;
import com.merchant.portal.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for FileStorageService.
 * Verifies save and retrieval actually persist to and read from H2 DB.
 */
class FileStorageServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MerchantDocumentRepository merchantDocumentRepository;

    @Test
    void save_shouldPersistFileToDatabase() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{1, 2, 3, 4});

        UUID id = fileStorageService.save(file);

        assertNotNull(id);
        assertTrue(merchantDocumentRepository.findById(id).isPresent());

        MerchantDocument doc = merchantDocumentRepository.findById(id).get();
        assertEquals("test.png", doc.getFileName());
        assertEquals("image/png", doc.getContentType());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, doc.getData());
    }

    @Test
    void save_withTypeAndExpiry_shouldPersistMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "selfie.jpg", "image/jpeg", new byte[]{5, 6, 7});

        LocalDateTime expiry = LocalDateTime.now().plusDays(5);
        UUID id = fileStorageService.save(file, "SELFIE", expiry);

        assertNotNull(id);
        MerchantDocument doc = merchantDocumentRepository.findById(id).orElseThrow();
        assertEquals("SELFIE", doc.getDocumentType());
        assertNotNull(doc.getExpiresAt());
    }

    @Test
    void save_shouldReturnNullForEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[]{});

        UUID id = fileStorageService.save(emptyFile);

        assertNull(id);
    }

    @Test
    void getFile_shouldReturnSavedDocument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "retrieve.pdf", "application/pdf", new byte[]{10, 20, 30});

        UUID id = fileStorageService.save(file);
        MerchantDocument retrieved = fileStorageService.getFile(id);

        assertEquals("retrieve.pdf", retrieved.getFileName());
        assertEquals("application/pdf", retrieved.getContentType());
        assertArrayEquals(new byte[]{10, 20, 30}, retrieved.getData());
    }

    @Test
    void getFile_shouldThrowForNonExistentId() {
        UUID fakeId = UUID.randomUUID();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileStorageService.getFile(fakeId));
        assertTrue(ex.getMessage().contains("File not found"));
    }
}

