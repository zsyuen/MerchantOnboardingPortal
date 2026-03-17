package com.merchant.portal.service;

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

    /**
     * Saves a multipart file to the database and returns the generated document UUID.
     *
     * @param file the uploaded file
     * @return the UUID of the saved MerchantDocument, or null if the file is empty
     */
    public UUID save(MultipartFile file) {
        return save(file, null, null);
    }

    /**
     * Saves a multipart file with an explicit document type and expiry timestamp.
     * Use this for files that must be purged after a retention period (e.g. selfies).
     *
     * @param file         the uploaded file
     * @param documentType a label such as "SELFIE", "ID_FRONT", etc.
     * @param expiresAt    when this document should be deleted; null means never expires
     * @return the UUID of the saved MerchantDocument, or null if the file is empty
     */
    public UUID save(MultipartFile file, String documentType, LocalDateTime expiresAt) {
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

            MerchantDocument saved = merchantDocumentRepository.save(document);
            return saved.getId();
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file in the database. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a stored document by its UUID.
     *
     * @param id the UUID of the document
     * @return the MerchantDocument entity
     * @throws RuntimeException if no document is found with the given ID
     */
    public MerchantDocument getFile(UUID id) {
        return merchantDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
    }
}