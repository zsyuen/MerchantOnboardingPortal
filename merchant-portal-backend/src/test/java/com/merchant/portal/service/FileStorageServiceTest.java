package com.merchant.portal.service;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.MerchantDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MerchantDocumentRepository merchantDocumentRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    @Test
    void save_shouldReturnUuid() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});

        UUID expectedId = UUID.randomUUID();
        MerchantDocument saved = new MerchantDocument();
        saved.setId(expectedId);
        when(merchantDocumentRepository.save(any(MerchantDocument.class))).thenReturn(saved);

        UUID result = fileStorageService.save(file);

        assertEquals(expectedId, result);
    }

    @Test
    void save_shouldReturnNullForEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        UUID result = fileStorageService.save(file);

        assertNull(result);
    }

    @Test
    void save_withTypeAndExpiry_shouldSetFields() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("selfie.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(new byte[]{1});

        UUID expectedId = UUID.randomUUID();
        MerchantDocument saved = new MerchantDocument();
        saved.setId(expectedId);
        when(merchantDocumentRepository.save(any(MerchantDocument.class))).thenAnswer(inv -> {
            MerchantDocument doc = inv.getArgument(0);
            assertEquals("SELFIE", doc.getDocumentType());
            assertNotNull(doc.getExpiresAt());
            doc.setId(expectedId);
            return doc;
        });

        LocalDateTime expiry = LocalDateTime.now().plusDays(5);
        UUID result = fileStorageService.save(file, "SELFIE", expiry);

        assertEquals(expectedId, result);
    }

    @Test
    void save_shouldThrowOnIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getBytes()).thenThrow(new IOException("disk error"));

        assertThrows(RuntimeException.class, () -> fileStorageService.save(file));
    }

    @Test
    void getFile_shouldReturnDocument() {
        UUID id = UUID.randomUUID();
        MerchantDocument doc = new MerchantDocument();
        doc.setId(id);
        doc.setFileName("test.png");
        when(merchantDocumentRepository.findById(id)).thenReturn(Optional.of(doc));

        MerchantDocument result = fileStorageService.getFile(id);

        assertEquals("test.png", result.getFileName());
    }

    @Test
    void getFile_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(merchantDocumentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileStorageService.getFile(id));
    }
}

