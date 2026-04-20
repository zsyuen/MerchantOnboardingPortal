package com.merchant.portal.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for security layer.
 * Verifies JWT authentication, public vs protected endpoints, and invalid token handling.
 */
class SecurityIntegrationTest extends BaseIntegrationTest {

    // ─── PUBLIC ENDPOINTS (no token required) ───────────────────────

    @Test
    void publicEndpoint_loginShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"x\",\"password\":\"y\"}"))
                .andExpect(status().isUnauthorized()); // 401 from bad creds, NOT from security filter
    }

    @Test
    void publicEndpoint_setupTotpShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/setup-totp")
                        .contentType("application/json")
                        .content("{\"username\":\"nonexistent\"}"))
                .andExpect(status().isBadRequest()); // 400 user not found, NOT 401
    }

    @Test
    void publicEndpoint_verifyTotpShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/verify-totp")
                        .contentType("application/json")
                        .content("{\"username\":\"nonexistent\",\"code\":123456}"))
                .andExpect(status().isBadRequest()); // 400 user not found, NOT 401
    }

    @Test
    void publicEndpoint_getApplicationByRefShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/applications/ref/SOME-REF"))
                .andExpect(status().isNotFound()); // 404 not found, NOT 401
    }

    // ─── PROTECTED ENDPOINTS (token required) ───────────────────────

    @Test
    void protectedEndpoint_getAllApplicationsShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_getAdminsShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/admins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_getDocumentShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/documents/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_getPermissionsShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/my-permissions"))
                .andExpect(status().isUnauthorized());
    }

    // ─── VALID TOKEN ────────────────────────────────────────────────

    @Test
    void validToken_shouldGrantAccessToProtectedEndpoint() throws Exception {
        String token = generateToken("reviewer", "reviewer");

        mockMvc.perform(get("/api/admins")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ─── INVALID / MALFORMED TOKEN ──────────────────────────────────

    @Test
    void invalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admins")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedAuthHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admins")
                        .header("Authorization", "NotBearer sometoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_shouldReturn401() throws Exception {
        // Generate a token with the utility but manually tamper with it
        String token = generateToken("reviewer", "reviewer");
        // Corrupt the token signature
        String corrupted = token.substring(0, token.length() - 5) + "XXXXX";

        mockMvc.perform(get("/api/admins")
                        .header("Authorization", "Bearer " + corrupted))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyAuthorizationHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admins")
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }
}

