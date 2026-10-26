package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
    long countByRole(Role role);
    long countByAccountStatus(AccountStatus status);
}
