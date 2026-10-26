package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.PickupRequestDto;
import com.foodwaste.platform.entity.*;
import com.foodwaste.platform.enums.*;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.exception.ResourceNotFoundException;
import com.foodwaste.platform.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class PickupRequestService {

    private final PickupRequestRepository pickupRequestRepository;
    private final FoodDonationRepository foodDonationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final FoodDonationService foodDonationService;

    public PickupRequestService(PickupRequestRepository pickupRequestRepository,
                                FoodDonationRepository foodDonationRepository,
                                OrganizationRepository organizationRepository,
                                UserRepository userRepository,
                                DeliveryRepository deliveryRepository,
                                NotificationService notificationService,
                                ActivityLogService activityLogService,
                                FoodDonationService foodDonationService) {
        this.pickupRequestRepository = pickupRequestRepository;
        this.foodDonationRepository = foodDonationRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.foodDonationService = foodDonationService;
    }

    @Transactional
    public PickupRequest requestDonation(PickupRequestDto dto, Long ngoUserId) {
        User ngoUser = userRepository.findById(ngoUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ngoUserId));

        Organization ngoOrg = organizationRepository.findByUserId(ngoUserId)
                .orElseThrow(() -> new BusinessException("No NGO organization profile found for user."));

        if (ngoOrg.getApprovalStatus() != AccountStatus.APPROVED) {
            throw new BusinessException("Your NGO profile is pending approval. Only approved NGOs can request food donations.");
        }

        // PESSIMISTIC WRITE LOCK prevents concurrent race conditions and over-reservation!
        FoodDonation donation = foodDonationRepository.findByIdWithLock(dto.getDonationId())
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + dto.getDonationId()));

        if (donation.isExpired() || donation.getStatus() == DonationStatus.EXPIRED) {
            throw new BusinessException("This donation has expired and can no longer be requested.");
        }

        if (donation.getStatus() == DonationStatus.CANCELLED) {
            throw new BusinessException("This donation was cancelled by the donor.");
        }

        if (donation.getStatus() != DonationStatus.AVAILABLE && donation.getStatus() != DonationStatus.PARTIALLY_RESERVED) {
            throw new BusinessException("This donation is no longer available for reservation.");
        }

        if (dto.getRequestedQuantity() == null || dto.getRequestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Requested quantity must be greater than zero.");
        }

        if (dto.getRequestedQuantity().compareTo(donation.getAvailableQuantity()) > 0) {
            throw new BusinessException("Requested quantity (" + dto.getRequestedQuantity() + " " + donation.getQuantityUnit() +
                    ") exceeds currently available quantity (" + donation.getAvailableQuantity() + " " + donation.getQuantityUnit() + ").");
        }

        // Prevent duplicate pending or approved requests by same NGO for same donation
        boolean hasDuplicate = pickupRequestRepository.existsByDonationIdAndNgoOrganizationIdAndStatusIn(
                donation.getId(), ngoOrg.getId(),
                Arrays.asList(PickupRequestStatus.PENDING, PickupRequestStatus.APPROVED, PickupRequestStatus.ASSIGNED));
        if (hasDuplicate) {
            throw new BusinessException("You already have an active request pending or approved for this donation.");
        }

        // Reserve quantity atomically
        BigDecimal remaining = donation.getAvailableQuantity().subtract(dto.getRequestedQuantity());
        donation.setAvailableQuantity(remaining);

        DonationStatus oldStatus = donation.getStatus();
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            donation.setStatus(DonationStatus.RESERVED);
        } else {
            donation.setStatus(DonationStatus.PARTIALLY_RESERVED);
        }
        foodDonationRepository.save(donation);

        if (oldStatus != donation.getStatus()) {
            foodDonationService.recordStatusHistory(donation, oldStatus.name(), donation.getStatus().name(), ngoUser,
                    "Quantity reserved by " + ngoOrg.getOrganizationName() + " (requested " + dto.getRequestedQuantity() + " " + donation.getQuantityUnit() + ")");
        }

        // Create PickupRequest
        PickupRequest request = new PickupRequest();
        request.setDonation(donation);
        request.setNgoOrganization(ngoOrg);
        request.setRequestedQuantity(dto.getRequestedQuantity());
        request.setQuantityUnit(donation.getQuantityUnit());
        request.setRequestedPickupTime(dto.getRequestedPickupTime());
        request.setStatus(PickupRequestStatus.PENDING);

        PickupRequest saved = pickupRequestRepository.save(request);

        // Notify Donor
        User donorUser = donation.getDonorOrganization().getUser();
        notificationService.sendNotification(donorUser, "New Pickup Request Received",
                ngoOrg.getOrganizationName() + " has requested " + dto.getRequestedQuantity() + " " +
                        donation.getQuantityUnit() + " of '" + donation.getFoodName() + "'. Please review and approve.",
                NotificationType.REQUEST_RECEIVED);

        activityLogService.logActivity(ngoUser, "REQUEST_DONATION", "PICKUP_REQUEST", saved.getId(),
                "Requested " + dto.getRequestedQuantity() + " " + donation.getQuantityUnit() + " of " + donation.getFoodName());

        return saved;
    }

    @Transactional
    public void reviewRequest(Long requestId, boolean approve, Long donorUserId, String remarks) {
        PickupRequest request = pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        FoodDonation donation = foodDonationRepository.findByIdWithLock(request.getDonation().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated donation not found"));

        User donorUser = userRepository.findById(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + donorUserId));

        if (!donation.getDonorOrganization().getUser().getId().equals(donorUserId) && donorUser.getRole() != Role.ADMIN) {
            throw new BusinessException("You are not authorized to review requests for this donation.");
        }

        if (request.getStatus() != PickupRequestStatus.PENDING) {
            throw new BusinessException("This request has already been reviewed (current status: " + request.getStatus() + ").");
        }

        request.setReviewedBy(donorUser);
        request.setReviewedAt(LocalDateTime.now());

        User ngoUser = request.getNgoOrganization().getUser();

        if (approve) {
            request.setStatus(PickupRequestStatus.APPROVED);
            pickupRequestRepository.save(request);

            // Automatically create delivery task for volunteers
            Delivery delivery = new Delivery();
            delivery.setPickupRequest(request);
            delivery.setDeliveryStatus(DeliveryStatus.ASSIGNED);
            deliveryRepository.save(delivery);

            notificationService.sendNotification(ngoUser, "Pickup Request Approved!",
                    "Good news! " + donation.getDonorOrganization().getOrganizationName() +
                            " has approved your request for " + request.getRequestedQuantity() + " " +
                            request.getQuantityUnit() + " of '" + donation.getFoodName() + "'. A volunteer will be assigned for delivery.",
                    NotificationType.REQUEST_APPROVED);

            activityLogService.logActivity(donorUser, "APPROVE_REQUEST", "PICKUP_REQUEST", request.getId(),
                    "Approved request #" + request.getId() + " from " + request.getNgoOrganization().getOrganizationName());
        } else {
            request.setStatus(PickupRequestStatus.REJECTED);
            pickupRequestRepository.save(request);

            // Return reserved quantity back to donation
            BigDecimal restored = donation.getAvailableQuantity().add(request.getRequestedQuantity());
            donation.setAvailableQuantity(restored);

            DonationStatus oldStatus = donation.getStatus();
            if (restored.compareTo(donation.getTotalQuantity()) == 0) {
                donation.setStatus(DonationStatus.AVAILABLE);
            } else {
                donation.setStatus(DonationStatus.PARTIALLY_RESERVED);
            }
            foodDonationRepository.save(donation);

            foodDonationService.recordStatusHistory(donation, oldStatus.name(), donation.getStatus().name(), donorUser,
                    "Restored " + request.getRequestedQuantity() + " " + donation.getQuantityUnit() + " after rejecting request #" + request.getId());

            notificationService.sendNotification(ngoUser, "Pickup Request Rejected",
                    "Your pickup request for '" + donation.getFoodName() + "' was rejected by the donor. Reason: " +
                            (remarks != null ? remarks : "Quantity not available / unable to fulfill."),
                    NotificationType.REQUEST_REJECTED);

            activityLogService.logActivity(donorUser, "REJECT_REQUEST", "PICKUP_REQUEST", request.getId(),
                    "Rejected request #" + request.getId() + " from " + request.getNgoOrganization().getOrganizationName());
        }
    }

    @Transactional
    public void cancelRequest(Long requestId, Long ngoUserId, String reason) {
        PickupRequest request = pickupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));

        if (!request.getNgoOrganization().getUser().getId().equals(ngoUserId)) {
            throw new BusinessException("You are not authorized to cancel this request.");
        }

        if (request.getStatus() != PickupRequestStatus.PENDING) {
            throw new BusinessException("Only pending requests can be cancelled.");
        }

        FoodDonation donation = foodDonationRepository.findByIdWithLock(request.getDonation().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Associated donation not found"));

        request.setStatus(PickupRequestStatus.CANCELLED);
        pickupRequestRepository.save(request);

        // Restore quantity
        BigDecimal restored = donation.getAvailableQuantity().add(request.getRequestedQuantity());
        donation.setAvailableQuantity(restored);
        if (restored.compareTo(donation.getTotalQuantity()) == 0) {
            donation.setStatus(DonationStatus.AVAILABLE);
        } else {
            donation.setStatus(DonationStatus.PARTIALLY_RESERVED);
        }
        foodDonationRepository.save(donation);

        User ngoUser = request.getNgoOrganization().getUser();
        activityLogService.logActivity(ngoUser, "CANCEL_REQUEST", "PICKUP_REQUEST", request.getId(),
                "Cancelled request #" + request.getId() + ". Reason: " + reason);

        // Notify donor
        User donorUser = donation.getDonorOrganization().getUser();
        notificationService.sendNotification(donorUser, "Pickup Request Cancelled",
                request.getNgoOrganization().getOrganizationName() + " has cancelled their pending request for '" +
                        donation.getFoodName() + "'. Reserved quantity has been restored.",
                NotificationType.REQUEST_CANCELLED);
    }

    @Transactional(readOnly = true)
    public Page<PickupRequest> getRequestsForNgo(Long ngoUserId, Pageable pageable) {
        Organization org = organizationRepository.findByUserId(ngoUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found for user: " + ngoUserId));
        return pickupRequestRepository.findByNgoOrganizationIdOrderByCreatedAtDesc(org.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PickupRequest> getRequestsForDonor(Long donorUserId, Pageable pageable) {
        Organization org = organizationRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found for user: " + donorUserId));
        return pickupRequestRepository.findByDonationDonorOrganizationIdOrderByCreatedAtDesc(org.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<PickupRequest> getRequestsByDonationId(Long donationId) {
        return pickupRequestRepository.findByDonationIdOrderByCreatedAtDesc(donationId);
    }

    @Transactional(readOnly = true)
    public PickupRequest getRequestById(Long id) {
        return pickupRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + id));
    }
}
