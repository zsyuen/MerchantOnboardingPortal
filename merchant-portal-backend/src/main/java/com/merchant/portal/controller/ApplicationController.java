package com.merchant.portal.controller;

import com.merchant.portal.model.Application;
import com.merchant.portal.repository.ApplicationRepository;
import com.merchant.portal.service.ApplicationService;
import com.merchant.portal.service.FileStorageService;
import com.merchant.portal.service.MerchantAiService;
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
    private final ApplicationRepository applicationRepository;
    private final FileStorageService fileStorageService;
    private final FaceVerificationService faceVerificationService;
    private final MerchantAiService merchantAiService;

    public ApplicationController(ApplicationService applicationService,
                                 ApplicationRepository applicationRepository,
                                 FileStorageService fileStorageService,
                                 FaceVerificationService faceVerificationService,
                                 MerchantAiService merchantAiService) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.fileStorageService = fileStorageService;
        this.faceVerificationService = faceVerificationService;
        this.merchantAiService = merchantAiService;
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
            // Save files temporarily without application link (application not yet saved)
            UUID ownerIdFrontId    = fileStorageService.save(ownerIdFront,    "ID_FRONT",          null, null);
            UUID ownerIdBackId     = fileStorageService.save(ownerIdBack,     "ID_BACK",           null, null);
            UUID passportPhotoId   = fileStorageService.save(passportPhoto,   "PASSPORT_PHOTO",    null, null);
            UUID proofOfBusinessId = fileStorageService.save(proofOfBusiness, "PROOF_OF_BUSINESS", null, null);
            // Selfie saved with 5-day retention
            UUID liveSelfieId      = fileStorageService.save(liveSelfie, "SELFIE", LocalDateTime.now().plusDays(5), null);

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

            // Now that application is saved, link each document back to it
            com.merchant.portal.model.MerchantDocument docFront    = fileStorageService.getFile(ownerIdFrontId);
            com.merchant.portal.model.MerchantDocument docBack     = fileStorageService.getFile(ownerIdBackId);
            com.merchant.portal.model.MerchantDocument docPassport = fileStorageService.getFile(passportPhotoId);
            com.merchant.portal.model.MerchantDocument docProof    = fileStorageService.getFile(proofOfBusinessId);
            com.merchant.portal.model.MerchantDocument docSelfie   = fileStorageService.getFile(liveSelfieId);

            docFront.setApplication(saved);
            docBack.setApplication(saved);
            docPassport.setApplication(saved);
            docProof.setApplication(saved);
            docSelfie.setApplication(saved);

            fileStorageService.saveDocument(docFront);
            fileStorageService.saveDocument(docBack);
            fileStorageService.saveDocument(docPassport);
            fileStorageService.saveDocument(docProof);
            fileStorageService.saveDocument(docSelfie);

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

    @PostMapping(value = "/{id}/analyze", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> analyzeApplication(@PathVariable Long id) {
        try {
            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Application not found with ID: " + id));

            double faceMatchScore = application.getFacialSimilarityScore() != null
                    ? application.getFacialSimilarityScore()
                    : 0.0d;

            String analysis = merchantAiService.analyzeMerchantApplication(application, faceMatchScore);
            return ResponseEntity.ok(analysis);
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to analyze application {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to analyze application: " + ex.getMessage());
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
