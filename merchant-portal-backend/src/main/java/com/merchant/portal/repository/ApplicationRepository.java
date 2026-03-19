package com.merchant.portal.repository;

import com.merchant.portal.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByReferenceId(String referenceId);

    List<Application> findByStatusIgnoreCase(String status);

    // Nulls out selfieImage on any Application that references the given document UUID
    @Modifying
    @Query("UPDATE Application a SET a.selfieImage = NULL WHERE a.selfieImage = :documentId")
    void clearSelfieImageByDocumentId(@Param("documentId") String documentId);
}
