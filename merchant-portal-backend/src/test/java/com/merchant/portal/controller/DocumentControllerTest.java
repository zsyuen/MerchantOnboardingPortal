package com.merchant.portal.controller;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DocumentController controller;

    @Test
    void getDocument_shouldReturnFileBytes() {
        UUID id = UUID.randomUUID();
        MerchantDocument doc = new MerchantDocument();
        doc.setId(id);
        doc.setFileName("test.png");
        doc.setContentType("image/png");
        doc.setData(new byte[]{1, 2, 3});
        when(fileStorageService.getFile(id)).thenReturn(doc);

        ResponseEntity<byte[]> response = controller.getDocument(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
    }

    @Test
    void getDocument_shouldReturn404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(fileStorageService.getFile(id)).thenThrow(new RuntimeException("Not found"));

        ResponseEntity<byte[]> response = controller.getDocument(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

