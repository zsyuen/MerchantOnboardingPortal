package com.merchant.portal.config;

import com.merchant.portal.model.*;
import com.merchant.portal.repository.*;
import com.merchant.portal.service.UserRoleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleService userRoleService;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      RoleRepository roleRepository,
                      PermissionRepository permissionRepository,
                      RolePermissionRepository rolePermissionRepository,
                      UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleService = userRoleService;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. Seed Permissions
        Permission approveReject = seedPermission("APPROVE_REJECT_APPLICATION", "Approve or reject merchant applications");
        Permission manageAdmin   = seedPermission("MANAGE_ADMIN_ACCESS", "Grant or revoke admin access");

        // 2. Seed Roles
        Role reviewerRole = seedRole("reviewer", "Super admin - can approve/reject and manage admins");
        Role adminRole    = seedRole("admin",    "Admin - can approve/reject merchant applications");

        // 3. Seed Role-Permission mappings
        seedRolePermission(reviewerRole, approveReject);
        seedRolePermission(reviewerRole, manageAdmin);
        seedRolePermission(adminRole,    approveReject);

        // 4. Seed default reviewer user
        if (userRepository.findByUsername("reviewer").isEmpty()) {
            User reviewer = new User();
            reviewer.setUsername("reviewer");
            reviewer.setPassword(passwordEncoder.encode("reviewer123"));
            reviewer.setEmail("reviewer@test.com");
            reviewer.setRole("reviewer");
            userRepository.save(reviewer);
            userRoleService.assignRoleToUser(reviewer);
            System.out.println("Default Reviewer created.");
        } else {
            // Ensure existing reviewer has role assigned in userrole table
            userRepository.findByUsername("reviewer").ifPresent(userRoleService::assignRoleToUser);
            System.out.println("Reviewer already exists.");
        }

        // 5. Backfill: ensure ALL existing users have their userrole entry
        userRepository.findAll().forEach(userRoleService::assignRoleToUser);
    }

    private Permission seedPermission(String name, String description) {
        return permissionRepository.findByPermissionName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name, description)));
    }

    private Role seedRole(String name, String description) {
        return roleRepository.findByRoleName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }

    private void seedRolePermission(Role role, Permission permission) {
        boolean exists = rolePermissionRepository.findByRole(role)
                .stream()
                .anyMatch(rp -> rp.getPermission().getPermissionName()
                        .equals(permission.getPermissionName()));
        if (!exists) {
            rolePermissionRepository.save(new RolePermission(role, permission));
        }
    }
}
