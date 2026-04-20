package com.merchant.portal.service;

import com.merchant.portal.model.MerchantDocument;
import com.merchant.portal.repository.ApplicationRepository;
import com.merchant.portal.repository.MerchantDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfieRetentionSchedulerTest {

    @Mock
    private MerchantDocumentRepository merchantDocumentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private SelfieRetentionScheduler scheduler;

    @Test
    void purgeExpiredSelfies_shouldDoNothingWhenNoExpired() {
        when(merchantDocumentRepository.findByDocumentTypeAndExpiresAtBefore(eq("SELFIE"), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.purgeExpiredSelfies();

        verify(applicationRepository, never()).clearSelfieImageByDocumentId(anyString());
        verify(merchantDocumentRepository, never()).delete(any(MerchantDocument.class));
    }

    @Test
    void purgeExpiredSelfies_shouldClearAndDeleteExpiredDocs() {
        UUID docId = UUID.randomUUID();
        MerchantDocument doc = new MerchantDocument();
        doc.setId(docId);
        doc.setDocumentType("SELFIE");
        doc.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(merchantDocumentRepository.findByDocumentTypeAndExpiresAtBefore(eq("SELFIE"), any(LocalDateTime.class)))
                .thenReturn(List.of(doc));

        scheduler.purgeExpiredSelfies();

        verify(applicationRepository).clearSelfieImageByDocumentId(docId.toString());
        verify(merchantDocumentRepository).delete(doc);
    }

    @Test
    void purgeExpiredSelfies_shouldHandleMultipleDocuments() {
        MerchantDocument doc1 = new MerchantDocument();
        doc1.setId(UUID.randomUUID());
        MerchantDocument doc2 = new MerchantDocument();
        doc2.setId(UUID.randomUUID());

        when(merchantDocumentRepository.findByDocumentTypeAndExpiresAtBefore(eq("SELFIE"), any(LocalDateTime.class)))
                .thenReturn(List.of(doc1, doc2));

        scheduler.purgeExpiredSelfies();

        verify(applicationRepository, times(2)).clearSelfieImageByDocumentId(anyString());
        verify(merchantDocumentRepository, times(2)).delete(any(MerchantDocument.class));
    }
}

