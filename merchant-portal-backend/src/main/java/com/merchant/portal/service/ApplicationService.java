package com.merchant.portal.service;

import com.merchant.portal.model.Application;
import com.merchant.portal.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    // Create
    public Application save(Application app) {
        System.out.println("🟢 Saving new application: " + app.getCompanyName());
        if (app.getReferenceId() == null) {
            app.setReferenceId(ApplicationIdGenerator.generateId());
        }
        if (app.getStatus() == null || app.getStatus().isEmpty()) {
            app.setStatus("Pending");
        }
        app.setSubmissionDate(LocalDateTime.now());
        return repository.save(app);
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
                .orElseThrow(() -> new EntityNotFoundException("Application not found with id: " + id));
    }

    // Read by status
    public List<Application> findByStatus(String status) {
        return repository.findByStatusIgnoreCase(status);
    }

    //General Update
    public Application update(Long id, Application updated) {
        Application existing = findById(id);
        existing.setStatus(updated.getStatus());
        existing.setCompanyName(updated.getCompanyName());
        existing.setTaxId(updated.getTaxId());
        return repository.save(existing);
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
            throw new EntityNotFoundException("Application not found with id: " + id);
        }
        repository.deleteById(id);
    }
}