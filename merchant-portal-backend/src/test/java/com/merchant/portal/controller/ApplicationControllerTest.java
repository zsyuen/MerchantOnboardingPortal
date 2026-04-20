package com.merchant.portal.controller;

import com.merchant.portal.model.Application;
import com.merchant.portal.service.ApplicationService;
import com.merchant.portal.service.FileStorageService;
import com.merchant.portal.service.FaceVerificationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FaceVerificationService faceVerificationService;

    @InjectMocks
    private ApplicationController controller;

    @Test
    void getAllApplications_shouldReturnList() {
        Application app1 = new Application();
        Application app2 = new Application();
        when(applicationService.findAll()).thenReturn(Arrays.asList(app1, app2));

        ResponseEntity<List<Application>> response = controller.getAllApplications();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getByRef_shouldReturnApplication() {
        Application app = new Application();
        app.setReferenceId("REF-001");
        when(applicationService.findByRefId("REF-001")).thenReturn(app);

        ResponseEntity<?> response = controller.getByRef("REF-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByRef_shouldReturn404WhenNotFound() {
        when(applicationService.findByRefId("INVALID")).thenThrow(new EntityNotFoundException("Not found"));

        ResponseEntity<?> response = controller.getByRef("INVALID");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getByStatus_shouldReturnFilteredList() {
        when(applicationService.findByStatus("Pending")).thenReturn(List.of(new Application()));

        ResponseEntity<List<Application>> response = controller.getByStatus("Pending");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getById_shouldReturnApplication() {
        Application app = new Application();
        app.setId(1L);
        when(applicationService.findById(1L)).thenReturn(app);

        ResponseEntity<?> response = controller.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getById_shouldReturn404WhenNotFound() {
        when(applicationService.findById(99L)).thenThrow(new EntityNotFoundException("Not found"));

        ResponseEntity<?> response = controller.getById(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteApplication_shouldReturn204() {
        doNothing().when(applicationService).delete(1L);

        ResponseEntity<?> response = controller.deleteApplication(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteApplication_shouldReturn404WhenNotFound() {
        doThrow(new EntityNotFoundException("Not found")).when(applicationService).delete(99L);

        ResponseEntity<?> response = controller.deleteApplication(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteApplication_shouldReturn500OnError() {
        doThrow(new RuntimeException("DB error")).when(applicationService).delete(1L);

        ResponseEntity<?> response = controller.deleteApplication(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void updateStatus_shouldReturnUpdatedApplication() {
        Application app = new Application();
        app.setId(1L);
        app.setStatus("Approved");
        when(applicationService.updateStatus(1L, "Approved")).thenReturn(app);

        ResponseEntity<?> response = controller.updateStatus(1L, Map.of("status", "Approved"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateStatus_shouldReturnBadRequestWhenStatusMissing() {
        ResponseEntity<?> response = controller.updateStatus(1L, Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateStatus_shouldReturnBadRequestWhenStatusEmpty() {
        ResponseEntity<?> response = controller.updateStatus(1L, Map.of("status", "  "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateStatus_shouldReturn404WhenNotFound() {
        when(applicationService.updateStatus(99L, "Approved"))
                .thenThrow(new EntityNotFoundException("Not found"));

        ResponseEntity<?> response = controller.updateStatus(99L, Map.of("status", "Approved"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

