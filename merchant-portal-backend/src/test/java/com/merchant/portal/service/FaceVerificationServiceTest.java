package com.merchant.portal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FaceVerificationServiceTest {

    private final FaceVerificationService service = new FaceVerificationService();

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

