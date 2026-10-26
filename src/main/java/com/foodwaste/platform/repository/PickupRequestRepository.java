package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.PickupRequest;
import com.foodwaste.platform.enums.PickupRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {

    Page<PickupRequest> findByNgoOrganizationIdOrderByCreatedAtDesc(Long ngoOrgId, Pageable pageable);

    Page<PickupRequest> findByDonationDonorOrganizationIdOrderByCreatedAtDesc(Long donorOrgId, Pageable pageable);

    List<PickupRequest> findByDonationIdOrderByCreatedAtDesc(Long donationId);

    Page<PickupRequest> findByStatusOrderByCreatedAtDesc(PickupRequestStatus status, Pageable pageable);

    boolean existsByDonationIdAndNgoOrganizationIdAndStatusIn(Long donationId, Long ngoOrgId, List<PickupRequestStatus> statuses);

    long countByStatus(PickupRequestStatus status);

    @Query("SELECT r.ngoOrganization.organizationName, SUM(r.requestedQuantity) " +
           "FROM PickupRequest r WHERE r.status = 'COMPLETED' " +
           "GROUP BY r.ngoOrganization.organizationName ORDER BY SUM(r.requestedQuantity) DESC")
    List<Object[]> findDistributedQuantityByNgo();
}
