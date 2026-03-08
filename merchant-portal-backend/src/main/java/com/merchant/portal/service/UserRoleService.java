package com.merchant.portal.service;

import com.merchant.portal.model.*;
import com.merchant.portal.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public UserRoleService(RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Automatically assigns the correct Role record to a user based on their role string.
     * Called whenever a new user is created.
     */
    public void assignRoleToUser(User user) {
        String roleName = user.getRole(); // e.g. "reviewer" or "admin"
        if (roleName == null) return;

        roleRepository.findByRoleName(roleName).ifPresent(role -> {
            // Avoid duplicate assignment
            if (!userRoleRepository.existsByUserAndRole_RoleName(user, roleName)) {
                userRoleRepository.save(new UserRole(user, role));
            }
        });
    }

    /**
     * Returns a list of permission names for a given user, derived from their role.
     */
    public List<String> getPermissionsForUser(User user) {
        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        return userRoles.stream()
                .flatMap(userRole -> rolePermissionRepository.findByRole(userRole.getRole()).stream())
                .map(rolePermission -> rolePermission.getPermission().getPermissionName())
                .distinct()
                .collect(Collectors.toList());
    }
}

