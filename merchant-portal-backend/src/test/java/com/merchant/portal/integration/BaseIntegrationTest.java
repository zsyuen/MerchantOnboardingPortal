package com.merchant.portal.integration;

import com.merchant.portal.security.JwtUtil;
import com.merchant.portal.service.EmailService;
import com.merchant.portal.service.FaceVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all integration tests.
 * Boots the full Spring context with H2, provides MockMvc, and mocks external services.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtUtil jwtUtil;

    // Mock external services that need infrastructure not available in tests
    @MockitoBean
    protected EmailService emailService;

    @MockitoBean
    protected FaceVerificationService faceVerificationService;

    /**
     * Generates a valid JWT token for use in authenticated requests.
     */
    protected String generateToken(String username, String role) {
        return jwtUtil.generateToken(username, role);
    }
}

