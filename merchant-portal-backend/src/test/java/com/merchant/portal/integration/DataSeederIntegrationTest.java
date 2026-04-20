package com.merchant.portal.integration;

import com.merchant.portal.model.*;
import com.merchant.portal.repository.*;
import com.merchant.portal.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for DataSeeder + UserRoleService.
 * Verifies that seeded data exists in H2 and role/permission assignment works end-to-end.
 */
class DataSeederIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserRoleService userRoleService;

    // ─── VERIFY DATASEEDER OUTPUT ───────────────────────────────────

    @Test
    void dataSeeder_shouldCreateReviewerUser() {
        assertTrue(userRepository.findByUsername("reviewer").isPresent(),
                "DataSeeder should create a 'reviewer' user");

        User reviewer = userRepository.findByUsername("reviewer").get();
        assertEquals("reviewer", reviewer.getRole());
        assertEquals("reviewer@test.com", reviewer.getEmail());
    }

    @Test
    void dataSeeder_shouldSeedRoles() {
        assertTrue(roleRepository.findByRoleName("reviewer").isPresent(),
                "DataSeeder should create 'reviewer' role");
        assertTrue(roleRepository.findByRoleName("admin").isPresent(),
                "DataSeeder should create 'admin' role");
    }

    @Test
    void dataSeeder_shouldSeedPermissions() {
        assertTrue(permissionRepository.findByPermissionName("APPROVE_REJECT_APPLICATION").isPresent(),
                "DataSeeder should create APPROVE_REJECT_APPLICATION permission");
        assertTrue(permissionRepository.findByPermissionName("MANAGE_ADMIN_ACCESS").isPresent(),
                "DataSeeder should create MANAGE_ADMIN_ACCESS permission");
    }

    @Test
    void dataSeeder_shouldAssignPermissionsToReviewerRole() {
        Role reviewerRole = roleRepository.findByRoleName("reviewer").orElseThrow();
        List<RolePermission> perms = rolePermissionRepository.findByRole(reviewerRole);

        // Reviewer should have both APPROVE_REJECT_APPLICATION and MANAGE_ADMIN_ACCESS
        assertEquals(2, perms.size());
        List<String> permNames = perms.stream()
                .map(rp -> rp.getPermission().getPermissionName())
                .toList();
        assertTrue(permNames.contains("APPROVE_REJECT_APPLICATION"));
        assertTrue(permNames.contains("MANAGE_ADMIN_ACCESS"));
    }

    @Test
    void dataSeeder_shouldAssignPermissionsToAdminRole() {
        Role adminRole = roleRepository.findByRoleName("admin").orElseThrow();
        List<RolePermission> perms = rolePermissionRepository.findByRole(adminRole);

        // Admin should only have APPROVE_REJECT_APPLICATION
        assertEquals(1, perms.size());
        assertEquals("APPROVE_REJECT_APPLICATION", perms.get(0).getPermission().getPermissionName());
    }

    @Test
    void dataSeeder_shouldAssignUserRoleToReviewer() {
        User reviewer = userRepository.findByUsername("reviewer").orElseThrow();
        List<UserRole> userRoles = userRoleRepository.findByUser(reviewer);

        assertFalse(userRoles.isEmpty(), "Reviewer should have at least one UserRole entry");
        assertEquals("reviewer", userRoles.get(0).getRole().getRoleName());
    }

    // ─── USER ROLE SERVICE END-TO-END ───────────────────────────────

    @Test
    void getPermissionsForUser_shouldReturnReviewerPermissions() {
        User reviewer = userRepository.findByUsername("reviewer").orElseThrow();
        List<String> permissions = userRoleService.getPermissionsForUser(reviewer);

        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("APPROVE_REJECT_APPLICATION"));
        assertTrue(permissions.contains("MANAGE_ADMIN_ACCESS"));
    }

    @Test
    void assignRoleToUser_shouldCreateUserRoleEntry() {
        // Create a new user
        User newUser = new User();
        newUser.setUsername("roleTestUser");
        newUser.setPassword("encoded");
        newUser.setEmail("roletest@test.com");
        newUser.setRole("admin");
        newUser = userRepository.save(newUser);

        // Assign role
        userRoleService.assignRoleToUser(newUser);

        // Verify UserRole entry created
        List<UserRole> roles = userRoleRepository.findByUser(newUser);
        assertEquals(1, roles.size());
        assertEquals("admin", roles.get(0).getRole().getRoleName());

        // Verify permissions through the service
        List<String> perms = userRoleService.getPermissionsForUser(newUser);
        assertTrue(perms.contains("APPROVE_REJECT_APPLICATION"));
    }

    @Test
    void assignRoleToUser_shouldNotDuplicateExistingAssignment() {
        User reviewer = userRepository.findByUsername("reviewer").orElseThrow();
        int countBefore = userRoleRepository.findByUser(reviewer).size();

        // Assign again — should not create duplicate
        userRoleService.assignRoleToUser(reviewer);

        int countAfter = userRoleRepository.findByUser(reviewer).size();
        assertEquals(countBefore, countAfter, "Should not create duplicate UserRole");
    }
}

