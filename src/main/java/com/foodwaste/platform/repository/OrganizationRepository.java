package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByUser(User user);
    Optional<Organization> findByUserId(Long userId);
    List<Organization> findByApprovalStatus(AccountStatus status);
    Page<Organization> findByApprovalStatus(AccountStatus status, Pageable pageable);
    Page<Organization> findByOrganizationType(OrganizationType type, Pageable pageable);
    long countByApprovalStatus(AccountStatus status);
    long countByOrganizationType(OrganizationType type);
}
