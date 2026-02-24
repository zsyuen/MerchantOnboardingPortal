package com.merchant.portal.controller;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Create a new Admin (POST /api/admins)
    @PostMapping
    public ResponseEntity<?> createAdmin(@RequestBody User user) {
        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
        }

        // Ensure role is set
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("admin");
        }

        // Encode password before saving
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Save to database
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // 2. Get all Admins (GET /api/admins) - Used by ManageAdminsComponent
    @GetMapping
    public List<User> getAllAdmins() {
        return userRepository.findAll();
    }

    // 3. Revoke/Grant placeholder methods (Optional, prevents 404 errors if you click those buttons)
    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokeAdmin(@PathVariable Long id) {
        // Add logic here if needed, for now just return OK
        return ResponseEntity.ok("Revoked");
    }

    @PostMapping("/{id}/grant")
    public ResponseEntity<?> grantAdmin(@PathVariable Long id) {
        // Add logic here if needed
        return ResponseEntity.ok("Granted");
    }
}