package com.foodwaste.platform.service;

import com.foodwaste.platform.entity.*;
import com.foodwaste.platform.enums.DeliveryStatus;
import com.foodwaste.platform.enums.DonationStatus;
import com.foodwaste.platform.enums.NotificationType;
import com.foodwaste.platform.enums.PickupRequestStatus;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.exception.ResourceNotFoundException;
import com.foodwaste.platform.repository.DeliveryRepository;
import com.foodwaste.platform.repository.DeliveryStatusHistoryRepository;
import com.foodwaste.platform.repository.FoodDonationRepository;
import com.foodwaste.platform.repository.PickupRequestRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryStatusHistoryRepository deliveryStatusHistoryRepository;
    private final UserRepository userRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final FoodDonationRepository foodDonationRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final FoodDonationService foodDonationService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           DeliveryStatusHistoryRepository deliveryStatusHistoryRepository,
                           UserRepository userRepository,
                           PickupRequestRepository pickupRequestRepository,
                           FoodDonationRepository foodDonationRepository,
                           NotificationService notificationService,
                           ActivityLogService activityLogService,
                           FoodDonationService foodDonationService) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryStatusHistoryRepository = deliveryStatusHistoryRepository;
        this.userRepository = userRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.foodDonationRepository = foodDonationRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.foodDonationService = foodDonationService;
    }

    @Transactional
    public Delivery assignVolunteer(Long deliveryId, Long volunteerUserId, Long adminUserId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));

        User volunteer = userRepository.findById(volunteerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer user not found: " + volunteerUserId));

        if (volunteer.getRole() != Role.VOLUNTEER) {
            throw new BusinessException("User is not a registered volunteer.");
        }

        User admin = (adminUserId != null) ? userRepository.findById(adminUserId).orElse(null) : null;

        delivery.setVolunteer(volunteer);
        delivery.setDeliveryStatus(DeliveryStatus.ASSIGNED);
        Delivery saved = deliveryRepository.save(delivery);

        // Update pickup request status
        PickupRequest request = delivery.getPickupRequest();
        request.setStatus(PickupRequestStatus.ASSIGNED);
        pickupRequestRepository.save(request);

        // Record history
        recordHistory(saved, null, DeliveryStatus.ASSIGNED.name(), admin != null ? admin : volunteer,
                "Assigned to volunteer: " + volunteer.getFullName());

        activityLogService.logActivity(admin != null ? admin : volunteer, "ASSIGN_VOLUNTEER", "DELIVERY", saved.getId(),
                "Assigned volunteer " + volunteer.getFullName() + " to delivery #" + saved.getId());

        // Notify Volunteer
        notificationService.sendNotification(volunteer, "New Food Delivery Assignment",
                "You have been assigned to deliver " + request.getRequestedQuantity() + " " +
                        request.getQuantityUnit() + " of food from " + request.getDonation().getDonorOrganization().getOrganizationName() +
                        " to " + request.getNgoOrganization().getOrganizationName(),
                NotificationType.VOLUNTEER_ASSIGNED);

        return saved;
    }

    @Transactional
    public Delivery updateStatus(Long deliveryId, DeliveryStatus newStatus, String notes, String confirmationRef, Long volunteerUserId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + deliveryId));

        User user = userRepository.findById(volunteerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + volunteerUserId));

        // Check authority: assigned volunteer or admin
        boolean isAssignedVolunteer = delivery.getVolunteer() != null && delivery.getVolunteer().getId().equals(volunteerUserId);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAssignedVolunteer && !isAdmin) {
            throw new BusinessException("Only the assigned volunteer can update this delivery status.");
        }

        DeliveryStatus currentStatus = delivery.getDeliveryStatus();

        // Valid state machine transitions
        if (newStatus == DeliveryStatus.PICKED_UP) {
            if (currentStatus != DeliveryStatus.ASSIGNED) {
                throw new BusinessException("Can only mark PICKED_UP when delivery is in ASSIGNED status.");
            }
            delivery.setPickupTime(LocalDateTime.now());
            delivery.getPickupRequest().setStatus(PickupRequestStatus.PICKED_UP);

            // Update donation status to COLLECTED if available quantity is 0
            FoodDonation donation = delivery.getPickupRequest().getDonation();
            if (donation.getStatus() == DonationStatus.RESERVED) {
                donation.setStatus(DonationStatus.COLLECTED);
                foodDonationRepository.save(donation);
                foodDonationService.recordStatusHistory(donation, DonationStatus.RESERVED.name(), DonationStatus.COLLECTED.name(), user, "Food picked up by volunteer");
            }
        } else if (newStatus == DeliveryStatus.IN_TRANSIT) {
            if (currentStatus != DeliveryStatus.PICKED_UP) {
                throw new BusinessException("Can only mark IN_TRANSIT after food has been PICKED_UP.");
            }
            delivery.getPickupRequest().setStatus(PickupRequestStatus.IN_TRANSIT);
        } else if (newStatus == DeliveryStatus.DELIVERED) {
            if (currentStatus != DeliveryStatus.PICKED_UP && currentStatus != DeliveryStatus.IN_TRANSIT) {
                throw new BusinessException("Can only mark DELIVERED from PICKED_UP or IN_TRANSIT status.");
            }
            delivery.setDeliveryTime(LocalDateTime.now());
            delivery.getPickupRequest().setStatus(PickupRequestStatus.COMPLETED);

            // Check if donation is fully distributed and mark DELIVERED
            FoodDonation donation = delivery.getPickupRequest().getDonation();
            if (donation.getStatus() == DonationStatus.COLLECTED || donation.getStatus() == DonationStatus.RESERVED) {
                donation.setStatus(DonationStatus.DELIVERED);
                foodDonationRepository.save(donation);
                foodDonationService.recordStatusHistory(donation, donation.getStatus().name(), DonationStatus.DELIVERED.name(), user, "Delivery completed to NGO");
            }
        } else if (newStatus == DeliveryStatus.FAILED) {
            // Delivery failed
            delivery.getPickupRequest().setStatus(PickupRequestStatus.CANCELLED);
        }

        delivery.setDeliveryStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            delivery.setDeliveryNotes(notes);
        }
        if (confirmationRef != null && !confirmationRef.isBlank()) {
            delivery.setConfirmationReference(confirmationRef);
        }

        Delivery updated = deliveryRepository.save(delivery);
        pickupRequestRepository.save(delivery.getPickupRequest());

        recordHistory(updated, currentStatus.name(), newStatus.name(), user, notes);

        activityLogService.logActivity(user, "UPDATE_DELIVERY_STATUS", "DELIVERY", updated.getId(),
                "Updated delivery #" + updated.getId() + " status to " + newStatus);

        // Notify NGO and Donor on Delivery completion
        if (newStatus == DeliveryStatus.DELIVERED) {
            User ngoUser = delivery.getPickupRequest().getNgoOrganization().getUser();
            User donorUser = delivery.getPickupRequest().getDonation().getDonorOrganization().getUser();

            notificationService.sendNotification(ngoUser, "Food Donation Delivered!",
                    "Your requested food (" + delivery.getPickupRequest().getRequestedQuantity() + " " +
                            delivery.getPickupRequest().getQuantityUnit() + " of " +
                            delivery.getPickupRequest().getDonation().getFoodName() + ") has been successfully delivered by volunteer " +
                            (delivery.getVolunteer() != null ? delivery.getVolunteer().getFullName() : "") + ".",
                    NotificationType.DELIVERED);

            notificationService.sendNotification(donorUser, "Donation Delivery Completed",
                    "Your surplus food donation '" + delivery.getPickupRequest().getDonation().getFoodName() +
                            "' has been delivered to " + delivery.getPickupRequest().getNgoOrganization().getOrganizationName() + ".",
                    NotificationType.DELIVERED);
        }

        return updated;
    }

    @Transactional(readOnly = true)
    public Page<Delivery> getDeliveriesForVolunteer(Long volunteerUserId, Pageable pageable) {
        return deliveryRepository.findByVolunteerIdOrderByCreatedAtDesc(volunteerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Delivery> getUnassignedDeliveries(Pageable pageable) {
        return deliveryRepository.findByVolunteerIsNullAndDeliveryStatus(DeliveryStatus.ASSIGNED, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Delivery> getAllDeliveries(DeliveryStatus status, Pageable pageable) {
        if (status != null) {
            return deliveryRepository.findByDeliveryStatusOrderByCreatedAtDesc(status, pageable);
        }
        return deliveryRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + id));
    }

    private void recordHistory(Delivery delivery, String oldStatus, String newStatus, User user, String remarks) {
        DeliveryStatusHistory history = new DeliveryStatusHistory(delivery, oldStatus, newStatus, user, remarks);
        deliveryStatusHistoryRepository.save(history);
    }
}
