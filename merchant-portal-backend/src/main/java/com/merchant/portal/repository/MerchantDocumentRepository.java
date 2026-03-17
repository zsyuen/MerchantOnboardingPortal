package com.merchant.portal.repository;

import com.merchant.portal.model.MerchantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantDocumentRepository extends JpaRepository<MerchantDocument, UUID> {

    /** Returns all documents whose expiresAt is in the past and match the given type. */
    List<MerchantDocument> findByDocumentTypeAndExpiresAtBefore(String documentType, LocalDateTime now);
}

