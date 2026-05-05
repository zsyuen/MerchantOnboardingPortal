package com.merchant.portal.service;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.ApplicationRepository;
import com.merchant.portal.repository.MerchantDocumentRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SelfieRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SelfieRetentionScheduler.class);
    private static final String SELFIE_TYPE = "SELFIE";

    private final MerchantDocumentRepository merchantDocumentRepository;
    private final ApplicationRepository applicationRepository;

    public SelfieRetentionScheduler(MerchantDocumentRepository merchantDocumentRepository,
                                    ApplicationRepository applicationRepository) {
        this.merchantDocumentRepository = merchantDocumentRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Runs once immediately after Spring Boot fully starts (all proxies ready).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnStartup() {
        log.info("[SelfieRetention] Running purge on application startup...");
        purgeExpiredSelfies();
    }

    /**
     * Runs every day at midnight.
     * Finds all SELFIE documents whose expiresAt has passed, BUT only for
     * applications that are NOT in "Pending" status (admin must approve/reject first).
     * Then nulls the selfieImage reference in the parent Application and deletes the document row.
     *
     * Cron: "0 0 0 * * *" = second=0, minute=0, hour=0, every day
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeExpiredSelfies() {
        List<MerchantDocument> expired =
                merchantDocumentRepository.findExpiredSelfiesExcludingPending(SELFIE_TYPE, LocalDateTime.now());

        if (expired.isEmpty()) {
            log.info("[SelfieRetention] No expired selfies found.");
            return;
        }

        log.info("[SelfieRetention] Found {} expired selfie(s) to purge.", expired.size());

        for (MerchantDocument doc : expired) {
            String docId = doc.getId().toString();

            // 1. Null out the selfieImage reference in any Application pointing to this doc
            applicationRepository.clearSelfieImageByDocumentId(docId);
            log.info("[SelfieRetention] Cleared selfieImage reference for document ID: {}", docId);

            // 2. Delete the actual binary data from merchant_document
            merchantDocumentRepository.delete(doc);
            log.info("[SelfieRetention] Deleted selfie document ID: {}", docId);
        }

        log.info("[SelfieRetention] Purge complete. {} selfie(s) deleted.", expired.size());
    }
}

