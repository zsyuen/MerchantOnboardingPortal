package com.merchant.portal.repository;

import com.merchant.portal.model.User;
import com.merchant.portal.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUser(User user);
    boolean existsByUserAndRole_RoleName(User user, String roleName);

    @Transactional
    void deleteByUser(User user);
}
