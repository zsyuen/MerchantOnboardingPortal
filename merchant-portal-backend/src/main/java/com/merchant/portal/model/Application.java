package com.merchant.portal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "application")
@Getter
@Setter
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", unique = true, nullable = false, length = 30)
    private String referenceId;
    private String status;
    private LocalDateTime submissionDate;

    // ================================
    // Merchant Information
    // ================================
    private String businessRegNo;
    private String companyName;
    private String dateOfIncorporation;
    private String countryOfCorporation;
    private String merchantNameEn;
    private String merchantNameLocal;
    private String taxId;
    private String classificationOfEntity;

    // ================================
    // Merchant Address
    // ================================
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String city;
    private String state;
    private String postal;
    private String country;
    private String telephone1;
    private String telephone2;

    // ================================
    // Owner Profile
    // ================================
    private String email;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String icPassport;
    private String nationality;
    private String idUploadFront;
    private String idUploadBack;
    private String passportPhoto;

    // ================================
    // Merchant Business
    // ================================
    private String industry;
    private String businessType;
    private int numberOfEmployees;
    private String schemeRequired;
    private String facilityRequired;
    private String proofOfBusiness;

    // ================================
    // Facial Verification
    // ================================
    private String selfieImage;
    private Double facialSimilarityScore;
    private String confidenceLevel;
    private String verificationStatus;
}