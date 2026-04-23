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

    @Column(name = "file_name", nullable = false) //file name that user uploads
    private String fileName;

    @Column(name = "content_type", nullable = false) //jpeg, jpg, pdf
    private String contentType;

    @Lob
    @Column(name = "data", nullable = false) //convert files into byte array
    private byte[] data;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
}
