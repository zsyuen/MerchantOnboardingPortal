package com.merchant.portal.service;

import com.merchant.portal.model.*;
import com.merchant.portal.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    @Test
    void assignRoleToUser_shouldAssignRole() {
        User user = new User();
        user.setRole("admin");

        Role role = new Role("admin", "Administrator");
        when(roleRepository.findByRoleName("admin")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserAndRole_RoleName(user, "admin")).thenReturn(false);

        userRoleService.assignRoleToUser(user);

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_shouldNotDuplicate() {
        User user = new User();
        user.setRole("admin");

        Role role = new Role("admin", "Administrator");
        when(roleRepository.findByRoleName("admin")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserAndRole_RoleName(user, "admin")).thenReturn(true);

        userRoleService.assignRoleToUser(user);

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void assignRoleToUser_shouldDoNothingIfRoleNull() {
        User user = new User();
        user.setRole(null);

        userRoleService.assignRoleToUser(user);

        verifyNoInteractions(roleRepository);
    }

    @Test
    void assignRoleToUser_shouldDoNothingIfRoleNotFound() {
        User user = new User();
        user.setRole("unknown");

        when(roleRepository.findByRoleName("unknown")).thenReturn(Optional.empty());

        userRoleService.assignRoleToUser(user);

        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void getPermissionsForUser_shouldReturnPermissions() {
        User user = new User();
        Role role = new Role("admin", "Admin");
        UserRole userRole = new UserRole(user, role);

        Permission perm1 = new Permission("READ", "Read access");
        Permission perm2 = new Permission("WRITE", "Write access");
        RolePermission rp1 = new RolePermission(role, perm1);
        RolePermission rp2 = new RolePermission(role, perm2);

        when(userRoleRepository.findByUser(user)).thenReturn(List.of(userRole));
        when(rolePermissionRepository.findByRole(role)).thenReturn(List.of(rp1, rp2));

        List<String> permissions = userRoleService.getPermissionsForUser(user);

        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("READ"));
        assertTrue(permissions.contains("WRITE"));
    }

    @Test
    void getPermissionsForUser_shouldReturnEmptyIfNoRoles() {
        User user = new User();
        when(userRoleRepository.findByUser(user)).thenReturn(Collections.emptyList());

        List<String> permissions = userRoleService.getPermissionsForUser(user);

        assertTrue(permissions.isEmpty());
    }

    @Test
    void getPermissionsForUser_shouldDeduplicatePermissions() {
        User user = new User();
        Role role1 = new Role("admin", "Admin");
        Role role2 = new Role("reviewer", "Reviewer");
        UserRole ur1 = new UserRole(user, role1);
        UserRole ur2 = new UserRole(user, role2);

        Permission perm = new Permission("READ", "Read");
        RolePermission rp1 = new RolePermission(role1, perm);
        RolePermission rp2 = new RolePermission(role2, perm);

        when(userRoleRepository.findByUser(user)).thenReturn(List.of(ur1, ur2));
        when(rolePermissionRepository.findByRole(role1)).thenReturn(List.of(rp1));
        when(rolePermissionRepository.findByRole(role2)).thenReturn(List.of(rp2));

        List<String> permissions = userRoleService.getPermissionsForUser(user);

        assertEquals(1, permissions.size());
    }
}

