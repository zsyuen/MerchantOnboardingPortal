package com.merchant.portal.config;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // Create default Reviewer/Super Admin
        if (userRepository.findByUsername("reviewer").isEmpty()) {
            User reviewer = new User();
            reviewer.setUsername("reviewer");
            reviewer.setPassword(passwordEncoder.encode("reviewer123"));
            reviewer.setEmail("reviewer@test.com");
            reviewer.setRole("reviewer");
            userRepository.save(reviewer);
            System.out.println("Default Reviewer is created.");
        }else{
            System.out.println("Reviewer already exists.");
        }
    }
}
