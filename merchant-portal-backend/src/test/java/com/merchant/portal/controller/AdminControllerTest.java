package com.merchant.portal.controller;

import com.merchant.portal.model.User;
import com.merchant.portal.repository.UserRepository;
import com.merchant.portal.repository.UserRoleRepository;
import com.merchant.portal.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private AdminController controller;

    @Test
    void createAdmin_shouldReturn201() {
        User user = new User();
        user.setUsername("newadmin");
        user.setPassword("password123");

        when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.createAdmin(user);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userRoleService).assignRoleToUser(any(User.class));
    }

    @Test
    void createAdmin_shouldReturn409WhenDuplicate() {
        User user = new User();
        user.setUsername("existing");

        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = controller.createAdmin(user);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void createAdmin_shouldDefaultRoleToAdmin() {
        User user = new User();
        user.setUsername("newadmin");
        user.setPassword("pass");

        when(userRepository.findByUsername("newadmin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertEquals("admin", saved.getRole());
            assertEquals("Granted", saved.getStatus());
            return saved;
        });

        controller.createAdmin(user);
    }

    @Test
    void checkUsername_shouldReturnTrue() {
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(new User()));

        ResponseEntity<Map<String, Boolean>> response = controller.checkUsername("taken");

        assertTrue(response.getBody().get("exists"));
    }

    @Test
    void checkUsername_shouldReturnFalse() {
        when(userRepository.findByUsername("free")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Boolean>> response = controller.checkUsername("free");

        assertFalse(response.getBody().get("exists"));
    }

    @Test
    void getAllAdmins_shouldReturnAdminList() {
        when(userRepository.findByRole("admin")).thenReturn(List.of(new User()));

        ResponseEntity<List<User>> response = controller.getAllAdmins();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void revokeAdmin_shouldSetStatusRevoked() {
        User user = new User();
        user.setId(1L);
        user.setStatus("Granted");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.revokeAdmin(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Revoked", user.getStatus());
    }

    @Test
    void revokeAdmin_shouldReturn404WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.revokeAdmin(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void grantAdmin_shouldSetStatusGranted() {
        User user = new User();
        user.setId(1L);
        user.setStatus("Revoked");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.grantAdmin(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Granted", user.getStatus());
    }

    @Test
    void grantAdmin_shouldReturn404WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.grantAdmin(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteAdmin_shouldReturn200() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.deleteAdmin(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRoleRepository).deleteByUser(user);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteAdmin_shouldReturn404WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deleteAdmin(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

