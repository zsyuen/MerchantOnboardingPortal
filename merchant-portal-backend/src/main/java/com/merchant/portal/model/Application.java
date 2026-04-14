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

    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate;

    // ================================
    // Merchant Information
    // ================================
    @Column(name = "business_reg_no", nullable = false)
    private String businessRegNo;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "date_of_incorporation", nullable = false)
    private String dateOfIncorporation;

    @Column(name = "country_of_corporation", nullable = false)
    private String countryOfCorporation;

    @Column(name = "merchant_name_en", nullable = false)
    private String merchantNameEn;

    @Column(name = "merchant_name_local", nullable = false)
    private String merchantNameLocal;

    @Column(name = "tax_id", nullable = false)
    private String taxId;

    @Column(name = "classification_of_entity", nullable = false)
    private String classificationOfEntity;

    // ================================
    // Merchant Address
    // ================================
    @Column(name = "addressLine1", nullable = false)
    private String addressLine1;

    @Column(name = "addressLine2")
    private String addressLine2;

    @Column(name = "addressLine3")
    private String addressLine3;

    @Column(name = "addressLine4")
    private String addressLine4;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "postal", nullable = false)
    private String postal;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "telephone1", nullable = false)
    private String telephone1;

    @Column(name = "telephone2")
    private String telephone2;

    // ================================
    // Owner Profile
    // ================================
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private String dateOfBirth;

    @Column(name = "ic_passport", nullable = false)
    private String icPassport;

    @Column(name = "nationality", nullable = false)
    private String nationality;

    @Column(name = "id_upload_front", nullable = false)
    private String idUploadFront;

    @Column(name = "id_upload_back", nullable = false)
    private String idUploadBack;

    @Column(name = "passport_photo", nullable = false)
    private String passportPhoto;

    // ================================
    // Merchant Business
    // ================================
    @Column(name = "industry", nullable = false)
    private String industry;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "number_of_employees", nullable = false)
    private int numberOfEmployees;

    @Column(name = "scheme_required", nullable = false)
    private String schemeRequired;

    @Column(name = "facility_required", nullable = false)
    private String facilityRequired;

    @Column(name = "proof_of_business", nullable = false)
    private String proofOfBusiness;

    // ================================
    // Facial Verification
    // ================================
    private String selfieImage;
    private Double facialSimilarityScore;
    private String confidenceLevel;
    private String verificationStatus;
}