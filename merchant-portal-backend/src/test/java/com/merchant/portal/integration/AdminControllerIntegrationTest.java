package com.merchant.portal.integration;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import com.merchant.portal.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminController.
 * Tests the full HTTP → Controller → Service → Repository → H2 DB flow.
 */
class AdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String reviewerToken;

    @BeforeEach
    void setUp() {
        // DataSeeder creates a "reviewer" user on startup.
        // Generate a JWT for the reviewer to access protected endpoints.
        reviewerToken = generateToken("reviewer", "reviewer");
    }

    // ─── CREATE ADMIN ───────────────────────────────────────────────

    @Test
    void createAdmin_shouldReturn201AndPersistToDatabase() throws Exception {
        String json = """
                {
                    "username": "integrationAdmin1",
                    "password": "password123",
                    "email": "integration1@test.com",
                    "role": "admin"
                }
                """;

        mockMvc.perform(post("/api/admins")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integrationAdmin1"))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.status").value("Granted"));

        // Verify it actually exists in the database
        assert userRepository.findByUsername("integrationAdmin1").isPresent();
    }

    @Test
    void createAdmin_shouldReturn409ForDuplicateUsername() throws Exception {
        // "reviewer" is already seeded by DataSeeder
        String json = """
                {
                    "username": "reviewer",
                    "password": "password123",
                    "email": "dup@test.com",
                    "role": "admin"
                }
                """;

        mockMvc.perform(post("/api/admins")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already taken")));
    }

    @Test
    void createAdmin_shouldDefaultRoleToAdmin() throws Exception {
        String json = """
                {
                    "username": "noRoleAdmin",
                    "password": "password123",
                    "email": "norole@test.com"
                }
                """;

        mockMvc.perform(post("/api/admins")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    void createAdmin_shouldReturn401WithoutToken() throws Exception {
        String json = """
                {
                    "username": "noAuthAdmin",
                    "password": "password123",
                    "email": "noauth@test.com",
                    "role": "admin"
                }
                """;

        mockMvc.perform(post("/api/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    // ─── CHECK USERNAME ─────────────────────────────────────────────

    @Test
    void checkUsername_shouldReturnTrueForExistingUser() throws Exception {
        mockMvc.perform(get("/api/admins/check-username")
                        .param("username", "reviewer")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void checkUsername_shouldReturnFalseForNonExistingUser() throws Exception {
        mockMvc.perform(get("/api/admins/check-username")
                        .param("username", "nonexistentUser999")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    // ─── GET ALL ADMINS ─────────────────────────────────────────────

    @Test
    void getAllAdmins_shouldReturnOnlyAdminRoleUsers() throws Exception {
        // Create an admin first
        String json = """
                {
                    "username": "listTestAdmin",
                    "password": "password123",
                    "email": "listtest@test.com",
                    "role": "admin"
                }
                """;
        mockMvc.perform(post("/api/admins")
                .header("Authorization", "Bearer " + reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        mockMvc.perform(get("/api/admins")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                // Should NOT contain the "reviewer" user since getAllAdmins filters by role="admin"
                .andExpect(jsonPath("$[*].role", everyItem(is("admin"))));
    }

    // ─── REVOKE / GRANT ─────────────────────────────────────────────

    @Test
    void revokeAndGrantAdmin_shouldToggleStatus() throws Exception {
        // Create an admin to revoke
        String json = """
                {
                    "username": "revokeTestAdmin",
                    "password": "password123",
                    "email": "revoke@test.com",
                    "role": "admin"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/admins")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the created user's ID
        String responseBody = createResult.getResponse().getContentAsString();
        Long userId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(responseBody).get("id").asLong();

        // Revoke the admin
        mockMvc.perform(post("/api/admins/" + userId + "/revoke")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Revoked"));

        // Verify in DB
        User revoked = userRepository.findById(userId).orElseThrow();
        assert "Revoked".equals(revoked.getStatus());

        // Grant the admin back
        mockMvc.perform(post("/api/admins/" + userId + "/grant")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Granted"));

        // Verify in DB
        User granted = userRepository.findById(userId).orElseThrow();
        assert "Granted".equals(granted.getStatus());
    }

    @Test
    void revokeAdmin_shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(post("/api/admins/99999/revoke")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void grantAdmin_shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(post("/api/admins/99999/grant")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE ADMIN ───────────────────────────────────────────────

    @Test
    void deleteAdmin_shouldRemoveFromDatabase() throws Exception {
        // Create an admin to delete
        String json = """
                {
                    "username": "deleteTestAdmin",
                    "password": "password123",
                    "email": "delete@test.com",
                    "role": "admin"
                }
                """;
        MvcResult createResult = mockMvc.perform(post("/api/admins")
                        .header("Authorization", "Bearer " + reviewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Delete
        mockMvc.perform(delete("/api/admins/" + userId)
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin deleted successfully."));

        // Verify deleted from DB
        assert userRepository.findById(userId).isEmpty();
    }

    @Test
    void deleteAdmin_shouldReturn404ForNonExistentId() throws Exception {
        mockMvc.perform(delete("/api/admins/99999")
                        .header("Authorization", "Bearer " + reviewerToken))
                .andExpect(status().isNotFound());
    }
}

