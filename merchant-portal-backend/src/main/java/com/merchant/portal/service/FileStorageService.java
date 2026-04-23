package com.merchant.portal.service;

import com.merchant.portal.model.Application;
import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.MerchantDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileStorageService {

    private final MerchantDocumentRepository merchantDocumentRepository;

    public FileStorageService(MerchantDocumentRepository merchantDocumentRepository) {
        this.merchantDocumentRepository = merchantDocumentRepository;
    }

    /** Saves a file without linking to any application. */
    public UUID save(MultipartFile file) {
        return save(file, null, null, null);
    }

    /** Saves a file with document type and expiry, without linking to an application. */
    public UUID save(MultipartFile file, String documentType, LocalDateTime expiresAt) {
        return save(file, documentType, expiresAt, null);
    }

    /** Saves a file and links it to the given Application (sets the FK). */
    public UUID save(MultipartFile file, String documentType, LocalDateTime expiresAt, Application application) {
        if (file.isEmpty()) {
            return null;
        }
        try {
            MerchantDocument document = new MerchantDocument();
            document.setFileName(file.getOriginalFilename());
            document.setContentType(file.getContentType());
            document.setData(file.getBytes());
            document.setDocumentType(documentType);
            document.setExpiresAt(expiresAt);
            document.setApplication(application);

            MerchantDocument saved = merchantDocumentRepository.save(document);
            return saved.getId();
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file in the database. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Persists an existing MerchantDocument entity (e.g. to update its application link).
     */
    public void saveDocument(MerchantDocument document) {
        merchantDocumentRepository.save(document);
    }

    /**
     * Retrieves a stored document by its UUID.
     */
    public MerchantDocument getFile(UUID id) {
        return merchantDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
    }
}