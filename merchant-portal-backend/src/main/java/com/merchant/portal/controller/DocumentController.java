package com.merchant.portal.controller;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for serving stored documents (images, PDFs) from the database.
 * The admin frontend calls GET /api/documents/{id} to display or download a file.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final FileStorageService fileStorageService;

    public DocumentController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Streams a document's binary content back to the browser with the correct Content-Type.
     * Images (image/png, image/jpeg) will render inline in the browser.
     * PDFs (application/pdf) will also render inline.
     *
     * @param id the UUID of the document stored in merchant_document table
     * @return the raw file bytes with appropriate Content-Type and Content-Disposition headers
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getDocument(@PathVariable UUID id) {
        try {
            MerchantDocument document = fileStorageService.getFile(id);

            HttpHeaders headers = new HttpHeaders();

            // Set the correct MIME type so the browser knows how to render the file
            headers.setContentType(MediaType.parseMediaType(document.getContentType()));

            // "inline" tells the browser to display the file rather than force a download
            headers.setContentDispositionFormData("inline", document.getFileName());

            return new ResponseEntity<>(document.getData(), headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

