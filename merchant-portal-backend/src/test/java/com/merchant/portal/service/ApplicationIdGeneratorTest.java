package com.merchant.portal.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationIdGeneratorTest {

    @Test
    void generateId_shouldReturnFormattedId() {
        String id = ApplicationIdGenerator.generateId();

        assertNotNull(id);
        // Format: yyyyMMddHHmmss-001
        assertTrue(id.matches("\\d{14}-\\d{3}"), "ID should match pattern yyyyMMddHHmmss-NNN but was: " + id);
    }

    @Test
    void generateId_shouldIncrementSequence() {
        String id1 = ApplicationIdGenerator.generateId();
        String id2 = ApplicationIdGenerator.generateId();

        // Extract sequence numbers
        int seq1 = Integer.parseInt(id1.substring(id1.lastIndexOf('-') + 1));
        int seq2 = Integer.parseInt(id2.substring(id2.lastIndexOf('-') + 1));

        assertEquals(seq1 + 1, seq2);
    }
}

