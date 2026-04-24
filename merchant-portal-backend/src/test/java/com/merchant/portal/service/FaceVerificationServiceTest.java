package com.merchant.portal.service;

import com.merchant.portal.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FaceVerificationServiceTest {

    private final SystemSettingRepository settingRepository = mock(SystemSettingRepository.class);
    private final FaceVerificationService service = new FaceVerificationService(settingRepository);

    // Default thresholds used when no DB settings exist (HIGH=0.70, MEDIUM=0.55)
    {
        when(settingRepository.findBySettingKey(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void getConfidenceLevel_shouldReturnHigh() {
        assertEquals("High", service.getConfidenceLevel(0.85));
        assertEquals("High", service.getConfidenceLevel(0.70));
    }

    @Test
    void getConfidenceLevel_shouldReturnMedium() {
        assertEquals("Medium", service.getConfidenceLevel(0.60));
        assertEquals("Medium", service.getConfidenceLevel(0.55));
    }

    @Test
    void getConfidenceLevel_shouldReturnLow() {
        assertEquals("Low", service.getConfidenceLevel(0.40));
        assertEquals("Low", service.getConfidenceLevel(0.0));
    }

    @Test
    void getConfidenceLevel_boundaryValues() {
        assertEquals("High", service.getConfidenceLevel(0.70));
        assertEquals("Medium", service.getConfidenceLevel(0.55));
        assertEquals("Low", service.getConfidenceLevel(0.54));
    }
}

