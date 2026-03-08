package com.merchant.portal.repository;

import com.merchant.portal.model.Role;
import com.merchant.portal.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole(Role role);
}

