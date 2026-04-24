package com.merchant.portal.repository;

import com.merchant.portal.model.MerchantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantDocumentRepository extends JpaRepository<MerchantDocument, UUID> {

    // Returns all documents whose expiresAt is in the past and match the given type
    List<MerchantDocument> findByDocumentTypeAndExpiresAtBefore(String documentType, LocalDateTime now);

    // Returns expired selfies only for applications that are NOT Pending (already reviewed by admin)
    @Query("SELECT d FROM MerchantDocument d WHERE d.documentType = :docType " +
           "AND d.expiresAt < :now " +
           "AND (d.application IS NULL OR d.application.status <> 'Pending')")
    List<MerchantDocument> findExpiredSelfiesExcludingPending(
            @Param("docType") String documentType,
            @Param("now") LocalDateTime now);
}

