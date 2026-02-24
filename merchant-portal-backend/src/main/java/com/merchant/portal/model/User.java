package com.merchant.portal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    private String username;

    @Column(name = "password_hash")
    private String password;
    private String email;
    private String role;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "is_mfa_enabled")
    private boolean mfaEnabled = false;
}