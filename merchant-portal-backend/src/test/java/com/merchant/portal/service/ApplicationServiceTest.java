package com.merchant.portal.service;

import com.merchant.portal.model.Application;
import com.merchant.portal.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationService applicationService;

    private Application sampleApp;

    @BeforeEach
    void setUp() {
        sampleApp = new Application();
        sampleApp.setId(1L);
        sampleApp.setCompanyName("Test Corp");
        sampleApp.setFirstName("John");
        sampleApp.setLastName("Doe");
        sampleApp.setEmail("john@test.com");
    }

    @Test
    void save_shouldGenerateReferenceIdAndSetDefaults() {
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.save(sampleApp);

        assertNotNull(result.getReferenceId());
        assertEquals("Pending", result.getStatus());
        assertNotNull(result.getSubmissionDate());
        verify(emailService).sendApplicationConfirmation(eq("john@test.com"), eq("John Doe"), anyString());
    }

    @Test
    void save_shouldNotOverrideExistingReferenceId() {
        sampleApp.setReferenceId("EXISTING-001");
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.save(sampleApp);

        assertEquals("EXISTING-001", result.getReferenceId());
    }

    @Test
    void save_shouldNotOverrideExistingStatus() {
        sampleApp.setStatus("Approved");
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.save(sampleApp);

        assertEquals("Approved", result.getStatus());
    }

    @Test
    void save_shouldContinueEvenIfEmailFails() {
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendApplicationConfirmation(anyString(), anyString(), anyString());

        Application result = applicationService.save(sampleApp);

        assertNotNull(result);
    }

    @Test
    void findAll_shouldReturnAllApplications() {
        when(repository.findAll()).thenReturn(Arrays.asList(sampleApp, new Application()));

        List<Application> result = applicationService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findByRefId_shouldReturnApplication() {
        when(repository.findByReferenceId("REF-001")).thenReturn(Optional.of(sampleApp));

        Application result = applicationService.findByRefId("REF-001");

        assertEquals("Test Corp", result.getCompanyName());
    }

    @Test
    void findByRefId_shouldThrowWhenNotFound() {
        when(repository.findByReferenceId("INVALID")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> applicationService.findByRefId("INVALID"));
    }

    @Test
    void findById_shouldReturnApplication() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleApp));

        Application result = applicationService.findById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> applicationService.findById(99L));
    }

    @Test
    void findByStatus_shouldReturnMatchingApplications() {
        when(repository.findByStatusIgnoreCase("Pending")).thenReturn(List.of(sampleApp));

        List<Application> result = applicationService.findByStatus("Pending");

        assertEquals(1, result.size());
    }

    @Test
    void updateStatus_shouldUpdateAndSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleApp));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Application result = applicationService.updateStatus(1L, "Approved");

        assertEquals("Approved", result.getStatus());
    }

    @Test
    void updateStatus_shouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> applicationService.updateStatus(99L, "Approved"));
    }

    @Test
    void delete_shouldDeleteWhenExists() {
        when(repository.existsById(1L)).thenReturn(true);

        applicationService.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> applicationService.delete(99L));
    }
}

