package com.merchant.portal.service;

import com.merchant.portal.model.Application;
import com.merchant.portal.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository repository;
    private final EmailService emailService;

    public ApplicationService(ApplicationRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    // Create
    public Application save(Application app) {
        System.out.println("Saving new application: " + app.getCompanyName());
        if (app.getReferenceId() == null) {
            app.setReferenceId(ApplicationIdGenerator.generateId());
        }
        if (app.getStatus() == null || app.getStatus().isEmpty()) {
            app.setStatus("Pending");
        }
        app.setSubmissionDate(LocalDateTime.now());
        Application saved = repository.save(app);

        // Send confirmation email with reference ID
        try {
            String applicantName = saved.getFirstName() + " " + saved.getLastName();
            emailService.sendApplicationConfirmation(saved.getEmail(), applicantName, saved.getReferenceId());
            log.info("Confirmation email sent to {}", saved.getEmail());
        } catch (Exception e) {
            log.error("Failed to send confirmation email to {}: {}", saved.getEmail(), e.getMessage());
        }

        return saved;
    }

    // Read
    public List<Application> findAll() {
        return repository.findAll();
    }

    // Read by reference
    public Application findByRefId(String refId) {
        return repository.findByReferenceId(refId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found with refId: " + refId));
    }

    // Read by ID
    public Application findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found with ID: " + id));
    }

    // Read by status
    public List<Application> findByStatus(String status) {
        return repository.findByStatusIgnoreCase(status);
    }

    //Specific Status Update
    public Application updateStatus(Long id, String newStatus) {
        Application existing = findById(id);
        existing.setStatus(newStatus);
        return repository.save(existing);
    }

    //Delete
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Application not found with ID: " + id);
        }
        repository.deleteById(id);
    }
}