package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.DashboardStatsDto;
import com.foodwaste.platform.enums.*;
import com.foodwaste.platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final FoodDonationRepository foodDonationRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final DeliveryRepository deliveryRepository;

    public ReportService(UserRepository userRepository,
                         OrganizationRepository organizationRepository,
                         FoodDonationRepository foodDonationRepository,
                         PickupRequestRepository pickupRequestRepository,
                         DeliveryRepository deliveryRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.foodDonationRepository = foodDonationRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTotalUsers(userRepository.count());
        stats.setTotalDonors(userRepository.countByRole(Role.DONOR));
        stats.setTotalNgos(userRepository.countByRole(Role.NGO));
        stats.setTotalVolunteers(userRepository.countByRole(Role.VOLUNTEER));
        stats.setPendingApprovals(organizationRepository.countByApprovalStatus(AccountStatus.PENDING));

        stats.setTotalDonations(foodDonationRepository.count());
        stats.setAvailableDonations(foodDonationRepository.countByStatus(DonationStatus.AVAILABLE) +
                foodDonationRepository.countByStatus(DonationStatus.PARTIALLY_RESERVED));
        stats.setPendingRequests(pickupRequestRepository.countByStatus(PickupRequestStatus.PENDING));
        stats.setActiveDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.ASSIGNED) +
                deliveryRepository.countByDeliveryStatus(DeliveryStatus.PICKED_UP) +
                deliveryRepository.countByDeliveryStatus(DeliveryStatus.IN_TRANSIT));
        stats.setCompletedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.DELIVERED));

        stats.setTotalQuantityDonated(foodDonationRepository.sumTotalQuantity());
        stats.setTotalQuantityAvailable(foodDonationRepository.sumAvailableQuantity());
        stats.setTotalQuantityDelivered(deliveryRepository.sumTotalDeliveredQuantity());

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReportsData() {
        Map<String, Object> reports = new HashMap<>();

        // Summary counts
        reports.put("stats", getDashboardStats());

        // Category breakdown
        List<Object[]> categoryCounts = foodDonationRepository.countDonationsByCategory();
        List<String> categoryLabels = new ArrayList<>();
        List<Long> categoryData = new ArrayList<>();
        for (Object[] row : categoryCounts) {
            FoodCategory cat = (FoodCategory) row[0];
            categoryLabels.add(cat != null ? cat.getDisplayName() : "Unknown");
            categoryData.add((Long) row[1]);
        }
        reports.put("categoryLabels", categoryLabels);
        reports.put("categoryData", categoryData);

        // Food type breakdown (Veg / Non-veg)
        List<Object[]> typeCounts = foodDonationRepository.countDonationsByFoodType();
        List<String> typeLabels = new ArrayList<>();
        List<Long> typeData = new ArrayList<>();
        for (Object[] row : typeCounts) {
            FoodType type = (FoodType) row[0];
            typeLabels.add(type != null ? type.getDisplayName() : "Unknown");
            typeData.add((Long) row[1]);
        }
        reports.put("typeLabels", typeLabels);
        reports.put("typeData", typeData);

        // Donor-wise donations
        List<Object[]> donorRows = foodDonationRepository.findDonationsByDonorOrganization();
        List<String> donorLabels = new ArrayList<>();
        List<BigDecimal> donorData = new ArrayList<>();
        int limit = 0;
        for (Object[] row : donorRows) {
            if (limit++ >= 7) break;
            donorLabels.add((String) row[0]);
            donorData.add((BigDecimal) row[1]);
        }
        reports.put("donorLabels", donorLabels);
        reports.put("donorData", donorData);

        // NGO distribution
        List<Object[]> ngoRows = pickupRequestRepository.findDistributedQuantityByNgo();
        List<String> ngoLabels = new ArrayList<>();
        List<BigDecimal> ngoData = new ArrayList<>();
        limit = 0;
        for (Object[] row : ngoRows) {
            if (limit++ >= 7) break;
            ngoLabels.add((String) row[0]);
            ngoData.add((BigDecimal) row[1]);
        }
        reports.put("ngoLabels", ngoLabels);
        reports.put("ngoData", ngoData);

        // Volunteer performance
        List<Object[]> volRows = deliveryRepository.findVolunteerPerformance();
        List<String> volLabels = new ArrayList<>();
        List<Long> volData = new ArrayList<>();
        limit = 0;
        for (Object[] row : volRows) {
            if (limit++ >= 7) break;
            volLabels.add((String) row[0]);
            volData.add((Long) row[1]);
        }
        reports.put("volLabels", volLabels);
        reports.put("volData", volData);

        return reports;
    }
}
