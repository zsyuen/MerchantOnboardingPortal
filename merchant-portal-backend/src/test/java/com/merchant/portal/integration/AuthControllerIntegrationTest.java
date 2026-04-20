package com.merchant.portal.integration;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests login, TOTP setup/verify, and permissions endpoints against real H2 DB.
 */
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Create a test user with MFA disabled for login tests (if not already exists)
        if (userRepository.findByUsername("authTestUser").isEmpty()) {
            User user = new User();
            user.setUsername("authTestUser");
            user.setPassword(passwordEncoder.encode("testpass123"));
            user.setEmail("authtest@test.com");
            user.setRole("admin");
            user.setMfaEnabled(false);
            user.setStatus("Granted");
            userRepository.save(user);
        }

        // Create a user with MFA enabled and a known TOTP secret
        if (userRepository.findByUsername("mfaUser").isEmpty()) {
            User mfaUser = new User();
            mfaUser.setUsername("mfaUser");
            mfaUser.setPassword(passwordEncoder.encode("mfapass123"));
            mfaUser.setEmail("mfa@test.com");
            mfaUser.setRole("admin");
            mfaUser.setMfaEnabled(true);
            mfaUser.setTotpSecret("JBSWY3DPEHPK3PXP"); // known test secret
            mfaUser.setStatus("Granted");
            userRepository.save(mfaUser);
        }

        // Create a revoked user
        if (userRepository.findByUsername("revokedUser").isEmpty()) {
            User revoked = new User();
            revoked.setUsername("revokedUser");
            revoked.setPassword(passwordEncoder.encode("revoked123"));
            revoked.setEmail("revoked@test.com");
            revoked.setRole("admin");
            revoked.setMfaEnabled(true);
            revoked.setTotpSecret("JBSWY3DPEHPK3PXP");
            revoked.setStatus("Revoked");
            userRepository.save(revoked);
        }
    }

    // ─── LOGIN ──────────────────────────────────────────────────────

    @Test
    void login_shouldReturn202WhenMfaNotSetUp() throws Exception {
        String json = """
                {
                    "username": "authTestUser",
                    "password": "testpass123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.action").value("SETUP_REQUIRED"))
                .andExpect(jsonPath("$.username").value("authTestUser"));
    }

    @Test
    void login_shouldReturn401ForInvalidCredentials() throws Exception {
        String json = """
                {
                    "username": "authTestUser",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_shouldReturn401ForNonExistentUser() throws Exception {
        String json = """
                {
                    "username": "noSuchUser",
                    "password": "anypassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_shouldReturn401WhenMfaEnabledButNoCodeProvided() throws Exception {
        String json = """
                {
                    "username": "mfaUser",
                    "password": "mfapass123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.action").value("CODE_REQUIRED"));
    }

    @Test
    void login_shouldReturn401ForInvalid2faCode() throws Exception {
        // Use code=1 which is almost certainly invalid for any TOTP secret
        String json = """
                {
                    "username": "mfaUser",
                    "password": "mfapass123",
                    "code": 1
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid 2FA Code"));
    }

    @Test
    void login_shouldReturn403ForRevokedUser() throws Exception {
        String json = """
                {
                    "username": "revokedUser",
                    "password": "revoked123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("revoked")));
    }

    // ─── SETUP TOTP ─────────────────────────────────────────────────

    @Test
    void setupTotp_shouldReturnSecretAndOtpUrl() throws Exception {
        String json = """
                { "username": "authTestUser" }
                """;

        mockMvc.perform(post("/api/auth/setup-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpAuthUrl", containsString("otpauth://totp/MerchantPortal:authTestUser")));
    }

    @Test
    void setupTotp_shouldReturn400ForMissingUsername() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/auth/setup-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username required"));
    }

    @Test
    void setupTotp_shouldReturn400ForUnknownUser() throws Exception {
        String json = """
                { "username": "unknownUser999" }
                """;

        mockMvc.perform(post("/api/auth/setup-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    // ─── VERIFY TOTP ────────────────────────────────────────────────

    @Test
    void verifyTotp_shouldReturn401ForInvalidCode() throws Exception {
        String json = """
                {
                    "username": "authTestUser",
                    "code": 999999
                }
                """;

        mockMvc.perform(post("/api/auth/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid Code"));
    }

    @Test
    void verifyTotp_shouldReturn400ForMissingCode() throws Exception {
        String json = """
                { "username": "authTestUser" }
                """;

        mockMvc.perform(post("/api/auth/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Code is required"));
    }

    @Test
    void verifyTotp_shouldReturn400ForInvalidCodeFormat() throws Exception {
        String json = """
                {
                    "username": "authTestUser",
                    "code": "notanumber"
                }
                """;

        mockMvc.perform(post("/api/auth/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid code format"));
    }

    @Test
    void verifyTotp_shouldReturn400ForUnknownUser() throws Exception {
        String json = """
                {
                    "username": "noSuchUser",
                    "code": 123456
                }
                """;

        mockMvc.perform(post("/api/auth/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    void verifyTotp_shouldReturn403ForRevokedUser() throws Exception {
        String json = """
                {
                    "username": "revokedUser",
                    "code": 123456
                }
                """;

        mockMvc.perform(post("/api/auth/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("revoked")));
    }

    // ─── MY PERMISSIONS ─────────────────────────────────────────────

    @Test
    void getMyPermissions_shouldReturnPermissionsForAuthenticatedUser() throws Exception {
        // The "reviewer" user is seeded by DataSeeder with APPROVE_REJECT_APPLICATION and MANAGE_ADMIN_ACCESS
        String token = generateToken("reviewer", "reviewer");

        mockMvc.perform(get("/api/auth/my-permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reviewer"))
                .andExpect(jsonPath("$.role").value("reviewer"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions", hasItem("APPROVE_REJECT_APPLICATION")));
    }

    @Test
    void getMyPermissions_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/my-permissions"))
                .andExpect(status().isUnauthorized());
    }
}
