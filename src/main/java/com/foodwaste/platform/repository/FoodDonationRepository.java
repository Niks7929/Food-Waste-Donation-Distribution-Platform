package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.FoodDonation;
import com.foodwaste.platform.enums.DonationStatus;
import com.foodwaste.platform.enums.FoodCategory;
import com.foodwaste.platform.enums.FoodType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodDonationRepository extends JpaRepository<FoodDonation, Long>, JpaSpecificationExecutor<FoodDonation> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM FoodDonation d WHERE d.id = :id")
    Optional<FoodDonation> findByIdWithLock(@Param("id") Long id);

    Page<FoodDonation> findByDonorOrganizationIdOrderByCreatedAtDesc(Long donorOrgId, Pageable pageable);

    Page<FoodDonation> findByStatusInOrderByCreatedAtDesc(List<DonationStatus> statuses, Pageable pageable);

    List<FoodDonation> findByStatusInAndPickupDeadlineBefore(List<DonationStatus> statuses, LocalDateTime dateTime);

    long countByStatus(DonationStatus status);

    @Query("SELECT COALESCE(SUM(d.totalQuantity), 0) FROM FoodDonation d")
    BigDecimal sumTotalQuantity();

    @Query("SELECT COALESCE(SUM(d.availableQuantity), 0) FROM FoodDonation d WHERE d.status IN ('AVAILABLE', 'PARTIALLY_RESERVED')")
    BigDecimal sumAvailableQuantity();

    @Query("SELECT COALESCE(SUM(d.totalQuantity), 0) FROM FoodDonation d WHERE d.status = 'DELIVERED'")
    BigDecimal sumDeliveredQuantity();

    @Query("SELECT d.donorOrganization.organizationName, SUM(d.totalQuantity) " +
           "FROM FoodDonation d GROUP BY d.donorOrganization.organizationName ORDER BY SUM(d.totalQuantity) DESC")
    List<Object[]> findDonationsByDonorOrganization();

    @Query("SELECT d.category, COUNT(d) FROM FoodDonation d GROUP BY d.category")
    List<Object[]> countDonationsByCategory();

    @Query("SELECT d.foodType, COUNT(d) FROM FoodDonation d GROUP BY d.foodType")
    List<Object[]> countDonationsByFoodType();
}
