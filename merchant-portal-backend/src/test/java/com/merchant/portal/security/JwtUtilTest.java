package com.merchant.portal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-that-is-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtUtil.generateToken("testuser", "admin");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("testuser", "admin");

        assertEquals("testuser", jwtUtil.extractUsername(token));
    }

    @Test
    void extractExpiration_shouldReturnFutureDate() {
        String token = jwtUtil.generateToken("testuser", "admin");

        Date expiration = jwtUtil.extractExpiration(token);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("testuser", "admin");

        assertTrue(jwtUtil.validateToken(token, "testuser"));
    }

    @Test
    void validateToken_shouldReturnFalseForWrongUsername() {
        String token = jwtUtil.generateToken("testuser", "admin");

        assertFalse(jwtUtil.validateToken(token, "wronguser"));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Set expiration to -1 to create an already-expired token
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken("testuser", "admin");

        // Reset expiration
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        assertThrows(Exception.class, () -> jwtUtil.validateToken(token, "testuser"));
    }

    @Test
    void extractClaim_shouldExtractRole() {
        String token = jwtUtil.generateToken("testuser", "admin");

        String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("admin", role);
    }
}

