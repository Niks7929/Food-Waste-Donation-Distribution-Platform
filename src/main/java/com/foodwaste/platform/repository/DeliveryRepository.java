package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.Delivery;
import com.foodwaste.platform.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByPickupRequestId(Long pickupRequestId);

    Page<Delivery> findByVolunteerIdOrderByCreatedAtDesc(Long volunteerId, Pageable pageable);

    Page<Delivery> findByVolunteerIsNullAndDeliveryStatus(DeliveryStatus status, Pageable pageable);

    Page<Delivery> findByDeliveryStatusOrderByCreatedAtDesc(DeliveryStatus status, Pageable pageable);

    long countByDeliveryStatus(DeliveryStatus status);

    @Query("SELECT COALESCE(SUM(d.pickupRequest.requestedQuantity), 0) FROM Delivery d WHERE d.deliveryStatus = 'DELIVERED'")
    BigDecimal sumTotalDeliveredQuantity();

    @Query("SELECT d.volunteer.fullName, COUNT(d) FROM Delivery d WHERE d.deliveryStatus = 'DELIVERED' AND d.volunteer IS NOT NULL " +
           "GROUP BY d.volunteer.fullName ORDER BY COUNT(d) DESC")
    List<Object[]> findVolunteerPerformance();
}
