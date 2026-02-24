package com.merchant.portal.controller;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import com.merchant.portal.security.JwtUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final GoogleAuthenticator gAuth;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;

        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(30000)
                .setWindowSize(5)
                .build();

        this.gAuth = new GoogleAuthenticator(config);
    }

    public static class LoginRequest {
        public String username;
        public String password;
        public Integer code;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Use BCrypt to verify password
            if (passwordEncoder.matches(loginRequest.password, user.getPassword())) {

                // 1. MANDATORY 2FA CHECK
                if (!user.isMfaEnabled()) {
                    return ResponseEntity.status(HttpStatus.ACCEPTED)
                            .body(Map.of(
                                    "message", "2FA setup is mandatory.",
                                    "action", "SETUP_REQUIRED",
                                    "username", user.getUsername()
                            ));
                }

                // 2. VERIFY CODE
                if (loginRequest.code == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of(
                                    "message", "Please enter your 2FA code.",
                                    "action", "CODE_REQUIRED"
                            ));
                }

                boolean isCodeValid = gAuth.authorize(user.getTotpSecret(), loginRequest.code);
                if (!isCodeValid) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Invalid 2FA Code"));
                }

                // 3. SUCCESS - Generate JWT token
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", user.getRole() != null ? user.getRole() : "admin"
                ));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }

    @PostMapping("/setup-totp")
    public ResponseEntity<?> setupTotp(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username required"));
        }

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }

        User user = userOptional.get();

        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        user.setTotpSecret(secret);
        userRepository.save(user);

        String otpAuthUrl = String.format("otpauth://totp/MerchantPortal:%s?secret=%s&issuer=MerchantPortal",
                username, secret);

        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "otpAuthUrl", otpAuthUrl
        ));
    }

    @PostMapping("/verify-totp")
    public ResponseEntity<?> verifyTotp(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        Object codeObj = payload.get("code");

        if (codeObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code is required"));
        }

        int code;
        try {
            code = Integer.parseInt(String.valueOf(codeObj));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid code format"));
        }

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOptional.get();

        boolean isCodeValid = gAuth.authorize(user.getTotpSecret(), code);

        if (isCodeValid) {
            user.setMfaEnabled(true);
            userRepository.save(user);

            // Generate JWT token for login
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "2FA Enabled Successfully",
                    "token", token,
                    "role", user.getRole() != null ? user.getRole() : "admin"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Code"));
        }
    }
}
