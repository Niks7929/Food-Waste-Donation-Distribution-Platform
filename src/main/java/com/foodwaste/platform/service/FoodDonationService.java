package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.DonationCreateDto;
import com.foodwaste.platform.dto.DonationFilterDto;
import com.foodwaste.platform.entity.DonationStatusHistory;
import com.foodwaste.platform.entity.FoodDonation;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.DonationStatus;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.exception.ResourceNotFoundException;
import com.foodwaste.platform.repository.DonationStatusHistoryRepository;
import com.foodwaste.platform.repository.FoodDonationRepository;
import com.foodwaste.platform.repository.OrganizationRepository;
import com.foodwaste.platform.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FoodDonationService {

    private final FoodDonationRepository foodDonationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final DonationStatusHistoryRepository donationStatusHistoryRepository;
    private final FileStorageService fileStorageService;
    private final ActivityLogService activityLogService;

    public FoodDonationService(FoodDonationRepository foodDonationRepository,
                               OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               DonationStatusHistoryRepository donationStatusHistoryRepository,
                               FileStorageService fileStorageService,
                               ActivityLogService activityLogService) {
        this.foodDonationRepository = foodDonationRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.donationStatusHistoryRepository = donationStatusHistoryRepository;
        this.fileStorageService = fileStorageService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public FoodDonation createDonation(DonationCreateDto dto, Long donorUserId) {
        User user = userRepository.findById(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + donorUserId));

        Organization org = organizationRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new BusinessException("No organization profile found for this donor account."));

        if (org.getApprovalStatus() != AccountStatus.APPROVED) {
            throw new BusinessException("Your organization profile has not been approved yet. Only approved donors can list food.");
        }

        if (dto.getPickupDeadline() == null || dto.getPickupDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Pickup deadline must be in the future.");
        }

        if (dto.getPreparedAt() != null && dto.getPreparedAt().isAfter(dto.getPickupDeadline())) {
            throw new BusinessException("Preparation date & time cannot be after the pickup deadline.");
        }

        if (dto.getTotalQuantity() == null || dto.getTotalQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Donation quantity must be greater than zero.");
        }

        FoodDonation donation = new FoodDonation();
        donation.setDonorOrganization(org);
        donation.setFoodName(dto.getFoodName().trim());
        donation.setDescription(dto.getDescription());
        donation.setCategory(dto.getCategory());
        donation.setFoodType(dto.getFoodType());
        donation.setTotalQuantity(dto.getTotalQuantity());
        donation.setAvailableQuantity(dto.getTotalQuantity());
        donation.setQuantityUnit(dto.getQuantityUnit());
        donation.setPreparedAt(dto.getPreparedAt());
        donation.setPickupDeadline(dto.getPickupDeadline());
        donation.setPickupAddress(dto.getPickupAddress().trim());
        donation.setLatitude(dto.getLatitude());
        donation.setLongitude(dto.getLongitude());
        donation.setStorageInstructions(dto.getStorageInstructions());
        donation.setAllergenInformation(dto.getAllergenInformation());
        donation.setStatus(DonationStatus.AVAILABLE);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String imageFilename = fileStorageService.storeFile(dto.getImageFile());
            donation.setImagePath(imageFilename);
        }

        FoodDonation savedDonation = foodDonationRepository.save(donation);

        // Record history
        recordStatusHistory(savedDonation, null, DonationStatus.AVAILABLE.name(), user, "Donation created and listed as available");

        activityLogService.logActivity(user, "CREATE_DONATION", "FOOD_DONATION", savedDonation.getId(),
                "Listed " + savedDonation.getTotalQuantity() + " " + savedDonation.getQuantityUnit() + " of " + savedDonation.getFoodName());

        return savedDonation;
    }

    @Transactional
    public FoodDonation updateDonation(Long donationId, DonationCreateDto dto, Long donorUserId) {
        FoodDonation donation = getDonationById(donationId);

        if (!donation.getDonorOrganization().getUser().getId().equals(donorUserId)) {
            throw new BusinessException("You are not authorized to edit this donation listing.");
        }

        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new BusinessException("Cannot edit donation once requests or reservations have begun (current status: " + donation.getStatus() + ").");
        }

        if (dto.getPickupDeadline() != null && dto.getPickupDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Pickup deadline must be in the future.");
        }

        donation.setFoodName(dto.getFoodName().trim());
        donation.setDescription(dto.getDescription());
        donation.setCategory(dto.getCategory());
        donation.setFoodType(dto.getFoodType());
        donation.setTotalQuantity(dto.getTotalQuantity());
        donation.setAvailableQuantity(dto.getTotalQuantity());
        donation.setQuantityUnit(dto.getQuantityUnit());
        donation.setPreparedAt(dto.getPreparedAt());
        donation.setPickupDeadline(dto.getPickupDeadline());
        donation.setPickupAddress(dto.getPickupAddress().trim());
        donation.setStorageInstructions(dto.getStorageInstructions());
        donation.setAllergenInformation(dto.getAllergenInformation());

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String imageFilename = fileStorageService.storeFile(dto.getImageFile());
            donation.setImagePath(imageFilename);
        }

        User user = userRepository.findById(donorUserId).orElse(null);
        FoodDonation updated = foodDonationRepository.save(donation);

        activityLogService.logActivity(user, "UPDATE_DONATION", "FOOD_DONATION", updated.getId(),
                "Updated details of donation: " + updated.getFoodName());

        return updated;
    }

    @Transactional
    public void cancelDonation(Long donationId, Long donorUserId, String reason) {
        FoodDonation donation = getDonationById(donationId);
        User user = userRepository.findById(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + donorUserId));

        if (!donation.getDonorOrganization().getUser().getId().equals(donorUserId) && user.getRole() != com.foodwaste.platform.enums.Role.ADMIN) {
            throw new BusinessException("You are not authorized to cancel this donation.");
        }

        if (donation.getStatus() == DonationStatus.COLLECTED || donation.getStatus() == DonationStatus.DELIVERED) {
            throw new BusinessException("Cannot cancel a donation that has already been collected or delivered.");
        }

        DonationStatus oldStatus = donation.getStatus();
        donation.setStatus(DonationStatus.CANCELLED);
        donation.setAvailableQuantity(BigDecimal.ZERO);
        foodDonationRepository.save(donation);

        recordStatusHistory(donation, oldStatus.name(), DonationStatus.CANCELLED.name(), user, "Cancelled by donor/admin: " + reason);

        activityLogService.logActivity(user, "CANCEL_DONATION", "FOOD_DONATION", donation.getId(),
                "Cancelled donation " + donation.getFoodName() + ". Reason: " + reason);
    }

    @Transactional(readOnly = true)
    public FoodDonation getDonationById(Long donationId) {
        return foodDonationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with ID: " + donationId));
    }

    @Transactional(readOnly = true)
    public Page<FoodDonation> getDonationsByDonor(Long donorUserId, Pageable pageable) {
        Organization org = organizationRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found for user: " + donorUserId));
        return foodDonationRepository.findByDonorOrganizationIdOrderByCreatedAtDesc(org.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<FoodDonation> getAvailableDonations(DonationFilterDto filter, Pageable pageable) {
        Specification<FoodDonation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status must be AVAILABLE or PARTIALLY_RESERVED
            predicates.add(root.get("status").in(Arrays.asList(DonationStatus.AVAILABLE, DonationStatus.PARTIALLY_RESERVED)));

            // Pickup deadline must be in future
            predicates.add(cb.greaterThan(root.get("pickupDeadline"), LocalDateTime.now()));

            // Available quantity > 0
            predicates.add(cb.greaterThan(root.get("availableQuantity"), BigDecimal.ZERO));

            if (filter != null) {
                if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                    String searchTerm = "%" + filter.getSearch().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("foodName")), searchTerm),
                            cb.like(cb.lower(root.get("description")), searchTerm),
                            cb.like(cb.lower(root.get("pickupAddress")), searchTerm)
                    ));
                }
                if (filter.getCategory() != null) {
                    predicates.add(cb.equal(root.get("category"), filter.getCategory()));
                }
                if (filter.getFoodType() != null) {
                    predicates.add(cb.equal(root.get("foodType"), filter.getFoodType()));
                }
                if (filter.getMinQuantity() != null && filter.getMinQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("availableQuantity"), filter.getMinQuantity()));
                }
                if (filter.getCity() != null && !filter.getCity().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("pickupAddress")), "%" + filter.getCity().trim().toLowerCase() + "%"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return foodDonationRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FoodDonation> getAllDonations(Pageable pageable) {
        return foodDonationRepository.findAll(pageable);
    }

    @Transactional
    public int expirePastDonations() {
        LocalDateTime now = LocalDateTime.now();
        List<FoodDonation> activePastDeadline = foodDonationRepository.findByStatusInAndPickupDeadlineBefore(
                Arrays.asList(DonationStatus.AVAILABLE, DonationStatus.PARTIALLY_RESERVED), now);

        int count = 0;
        for (FoodDonation donation : activePastDeadline) {
            DonationStatus oldStatus = donation.getStatus();
            donation.setStatus(DonationStatus.EXPIRED);
            donation.setAvailableQuantity(BigDecimal.ZERO);
            foodDonationRepository.save(donation);

            recordStatusHistory(donation, oldStatus.name(), DonationStatus.EXPIRED.name(), null, "Automated expiry check: past pickup deadline");
            count++;
        }
        return count;
    }

    public void recordStatusHistory(FoodDonation donation, String oldStatus, String newStatus, User user, String remarks) {
        DonationStatusHistory history = new DonationStatusHistory(donation, oldStatus, newStatus, user, remarks);
        donationStatusHistoryRepository.save(history);
    }
}
