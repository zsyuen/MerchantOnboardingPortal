package com.merchant.portal.controller;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import com.merchant.portal.service.UserRoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder, UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRoleService = userRoleService;
    }

    // 1. Create a new Admin (POST /api/admins)
    @PostMapping
    public ResponseEntity<?> createAdmin(@RequestBody User user) {
        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username \"" + user.getUsername() + "\" is already taken. Please use a different username."));
        }

        // Set role
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("admin");
        }

        // New admins are granted access by default
        user.setStatus("Granted");

        // Encode password before saving
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Save to database
        User savedUser = userRepository.save(user);

        // Auto-assign role in userrole table
        userRoleService.assignRoleToUser(savedUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // 1b. Check if a username is already taken (GET /api/admins/check-username?username=xxx)
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean exists = userRepository.findByUsername(username).isPresent();
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // 2. Get all Admins (GET /api/admins)
    @GetMapping
    public ResponseEntity<List<User>> getAllAdmins() {

        // Fetch only users with the 'admin' role, so Reviewers don't revoke themselves
        List<User> admins = userRepository.findByRole("admin");
        return ResponseEntity.ok(admins);
    }

    // 3. Revoke/ Grant Access
    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokeAdmin(@PathVariable Long id) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = optionalUser.get();
        user.setStatus("Revoked");
        userRepository.save(user);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/{id}/grant")
    public ResponseEntity<?> grantAdmin(@PathVariable Long id) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = optionalUser.get();
        user.setStatus("Granted");
        userRepository.save(user);

        return ResponseEntity.ok(user);
    }
}