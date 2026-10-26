package com.foodwaste.platform.repository;

import com.foodwaste.platform.entity.DonationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationStatusHistoryRepository extends JpaRepository<DonationStatusHistory, Long> {
    List<DonationStatusHistory> findByDonationIdOrderByChangedAtDesc(Long donationId);
}
