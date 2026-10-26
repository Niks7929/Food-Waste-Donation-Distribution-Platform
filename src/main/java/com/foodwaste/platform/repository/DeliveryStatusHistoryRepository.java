package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.DeliveryStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryStatusHistoryRepository extends JpaRepository<DeliveryStatusHistory, Long> {
    List<DeliveryStatusHistory> findByDeliveryIdOrderByChangedAtDesc(Long deliveryId);
}
