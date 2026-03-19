package com.merchant.portal.controller;

import com.merchant.portal.model.Application;
import com.merchant.portal.service.ApplicationService;
import com.merchant.portal.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import com.merchant.portal.service.FaceVerificationService;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);
    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final FaceVerificationService faceVerificationService;

    public ApplicationController(ApplicationService applicationService, FileStorageService fileStorageService, FaceVerificationService faceVerificationService) {
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
        this.faceVerificationService = faceVerificationService;
    }

    // Handle form data
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> createApplication(
            // Map all form fields and files individually
            @RequestParam("companyName") String companyName,
            @RequestParam("businessRegNo") String businessRegNo,
            @RequestParam("incorporationDate") String incorporationDate,
            @RequestParam("countryOfCorp") String countryOfCorp,
            @RequestParam("merchantNameEn") String merchantNameEn,
            @RequestParam("merchantNameLocal") String merchantNameLocal,
            @RequestParam("taxId") String taxId,
            @RequestParam("entityType") String entityType,
            @RequestParam("address1") String address1,
            @RequestParam(name = "address2", required = false) String address2,
            @RequestParam(name = "address3", required = false) String address3,
            @RequestParam(name = "address4", required = false) String address4,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("postal") String postal,
            @RequestParam("country") String country,
            @RequestParam("phone1") String phone1,
            @RequestParam(name = "phone2", required = false) String phone2,
            @RequestParam("ownerFirstName") String ownerFirstName,
            @RequestParam("ownerLastName") String ownerLastName,
            @RequestParam("ownerEmail") String ownerEmail,
            @RequestParam("ownerDob") String ownerDob,
            @RequestParam("ownerIdNo") String ownerIdNo,
            @RequestParam("ownerNationality") String ownerNationality,
            @RequestParam("industry") String industry,
            @RequestParam("businessType") String businessType,
            @RequestParam("numEmployees") int numEmployees,
            @RequestParam("schemeRequired") String schemeRequired,
            @RequestParam("facilityRequired") String facilityRequired,
            @RequestPart("ownerIdFront") MultipartFile ownerIdFront,
            @RequestPart("ownerIdBack") MultipartFile ownerIdBack,
            @RequestPart("passportPhoto") MultipartFile passportPhoto,
            @RequestPart("proofOfBusiness") MultipartFile proofOfBusiness,
            @RequestPart("liveSelfie") MultipartFile liveSelfie
    ) {
        try {
            // Save files to DB and get their generated UUIDs
            UUID ownerIdFrontId    = fileStorageService.save(ownerIdFront);
            UUID ownerIdBackId     = fileStorageService.save(ownerIdBack);
            UUID passportPhotoId   = fileStorageService.save(passportPhoto);
            UUID proofOfBusinessId = fileStorageService.save(proofOfBusiness);
            // Selfie is saved with a 5-day retention — scheduler will purge it after expiry
            UUID liveSelfieId      = fileStorageService.save(liveSelfie, "SELFIE", LocalDateTime.now().plusDays(5));

            // Retrieve raw bytes from DB for face comparison
            byte[] passportBytes = fileStorageService.getFile(passportPhotoId).getData();
            byte[] selfieBytes   = fileStorageService.getFile(liveSelfieId).getData();

            // Compare passport photo against live selfie and get similarity score
            double score = faceVerificationService.compareFaces(passportBytes, selfieBytes);
            // Get Confidence level (High/ Medium/ Low) for admin reviewing
            String confidence = faceVerificationService.getConfidenceLevel(score);

            // Set initial status to Pending for admin review
            String initialStatus = "Pending";

            Application app = new Application();
            app.setCompanyName(companyName);
            app.setBusinessRegNo(businessRegNo);
            app.setDateOfIncorporation(incorporationDate);
            app.setCountryOfCorporation(countryOfCorp);
            app.setMerchantNameEn(merchantNameEn);
            app.setMerchantNameLocal(merchantNameLocal);
            app.setTaxId(taxId);
            app.setClassificationOfEntity(entityType);
            app.setAddressLine1(address1);
            app.setAddressLine2(address2);
            app.setAddressLine3(address3);
            app.setAddressLine4(address4);
            app.setCity(city);
            app.setState(state);
            app.setPostal(postal);
            app.setCountry(country);
            app.setTelephone1(phone1);
            app.setTelephone2(phone2);
            app.setFirstName(ownerFirstName);
            app.setLastName(ownerLastName);
            app.setEmail(ownerEmail);
            app.setDateOfBirth(ownerDob);
            app.setIcPassport(ownerIdNo);
            app.setNationality(ownerNationality);
            app.setIndustry(industry);
            app.setBusinessType(businessType);
            app.setNumberOfEmployees(numEmployees);
            app.setSchemeRequired(schemeRequired);
            app.setFacilityRequired(facilityRequired);
            app.setSelfieImage(liveSelfieId.toString());
            app.setFacialSimilarityScore(score);
            app.setConfidenceLevel(confidence);
            app.setVerificationStatus(initialStatus);

            // Store document UUIDs as references in the Application record
            app.setIdUploadFront(ownerIdFrontId.toString());
            app.setIdUploadBack(ownerIdBackId.toString());
            app.setPassportPhoto(passportPhotoId.toString());
            app.setProofOfBusiness(proofOfBusinessId.toString());

            Application saved = applicationService.save(app);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            log.error("Failed to save application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save application: " + e.getMessage());
        }
    }

    // List
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Application>> getAllApplications() {
        return ResponseEntity.ok(applicationService.findAll());
    }

    // Get by reference
    @GetMapping(value = "/ref/{refId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByRef(@PathVariable String refId) {
        try {
            return ResponseEntity.ok(applicationService.findByRefId(refId));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Application not found for refId: " + refId);
        }
    }

    // Get by status
    @GetMapping(value = "/by-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Application>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(applicationService.findByStatus(status));
    }

    // Get by ID
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(applicationService.findById(id));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Application not found with ID: " + id);
        }
    }

    // Update
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateApplication(@PathVariable Long id, @RequestBody Application updated) {
        try {
            Application existing = applicationService.findById(id);
            if (isBlank(updated.getReferenceId())) {
                updated.setReferenceId(existing.getReferenceId());
            }
            if (updated.getSubmissionDate() == null) {
                updated.setSubmissionDate(existing.getSubmissionDate());
            }
            Application result = applicationService.update(id, updated);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Cannot update. Application not found with ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update application: " + e.getMessage());
        }
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        try {
            applicationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Application not found for deletion with ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete application: " + e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String newStatus = payload.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status is required.");
            }
            Application updatedApplication = applicationService.updateStatus(id, newStatus);
            return ResponseEntity.ok(updatedApplication);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
