package com.merchant.portal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_document")
@Getter
@Setter
public class MerchantDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Lob
    @Column(name = "data", nullable = false)
    private byte[] data;

    /** e.g. "SELFIE", "ID_FRONT", "ID_BACK", "PASSPORT_PHOTO", "PROOF_OF_BUSINESS" */
    @Column(name = "document_type")
    private String documentType;

    /** Null means no expiry. Selfies are set to uploadedAt + 5 days. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

