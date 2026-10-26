package com.foodwaste.platform.service;

import com.foodwaste.platform.entity.*;
import com.foodwaste.platform.enums.*;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryStatusHistoryRepository deliveryStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private FoodDonationRepository foodDonationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private FoodDonationService foodDonationService;

    @InjectMocks
    private DeliveryService deliveryService;

    private User volunteer;
    private User unauthorizedVolunteer;
    private Delivery delivery;
    private PickupRequest pickupRequest;
    private FoodDonation donation;

    @BeforeEach
    void setUp() {
        volunteer = new User();
        volunteer.setId(3L);
        volunteer.setFullName("Courier Amit");
        volunteer.setRole(Role.VOLUNTEER);

        unauthorizedVolunteer = new User();
        unauthorizedVolunteer.setId(4L);
        unauthorizedVolunteer.setFullName("Unauthorized Person");
        unauthorizedVolunteer.setRole(Role.VOLUNTEER);

        User donorUser = new User();
        donorUser.setId(1L);
        Organization donorOrg = new Organization();
        donorOrg.setUser(donorUser);
        donorOrg.setOrganizationName("Test Donor Org");

        User ngoUser = new User();
        ngoUser.setId(2L);
        Organization ngoOrg = new Organization();
        ngoOrg.setUser(ngoUser);
        ngoOrg.setOrganizationName("Test NGO Org");

        donation = new FoodDonation();
        donation.setId(100L);
        donation.setDonorOrganization(donorOrg);
        donation.setFoodName("Dal Rice");
        donation.setStatus(DonationStatus.RESERVED);

        pickupRequest = new PickupRequest();
        pickupRequest.setId(500L);
        pickupRequest.setDonation(donation);
        pickupRequest.setNgoOrganization(ngoOrg);
        pickupRequest.setRequestedQuantity(new BigDecimal("50"));
        pickupRequest.setQuantityUnit(QuantityUnit.MEALS);
        pickupRequest.setStatus(PickupRequestStatus.ASSIGNED);

        delivery = new Delivery();
        delivery.setId(700L);
        delivery.setPickupRequest(pickupRequest);
        delivery.setVolunteer(volunteer);
        delivery.setDeliveryStatus(DeliveryStatus.ASSIGNED);
    }

    @Test
    void testUpdateStatus_ProgressesFromAssignedToPickedUp() {
        when(deliveryRepository.findById(700L)).thenReturn(Optional.of(delivery));
        when(userRepository.findById(3L)).thenReturn(Optional.of(volunteer));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        Delivery updated = deliveryService.updateStatus(700L, DeliveryStatus.PICKED_UP, "Collected in food warmers", null, 3L);

        assertEquals(DeliveryStatus.PICKED_UP, updated.getDeliveryStatus());
        assertNotNull(updated.getPickupTime(), "Pickup timestamp must be recorded");
        assertEquals("Collected in food warmers", updated.getDeliveryNotes());

        verify(deliveryRepository).save(delivery);
        verify(deliveryStatusHistoryRepository).save(any());
    }

    @Test
    void testUpdateStatus_MarksDeliveredAndCompletesRequest() {
        delivery.setDeliveryStatus(DeliveryStatus.IN_TRANSIT);

        when(deliveryRepository.findById(700L)).thenReturn(Optional.of(delivery));
        when(userRepository.findById(3L)).thenReturn(Optional.of(volunteer));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        Delivery updated = deliveryService.updateStatus(700L, DeliveryStatus.DELIVERED, "Handed to kitchen lead", "CONF-9988", 3L);

        assertEquals(DeliveryStatus.DELIVERED, updated.getDeliveryStatus());
        assertNotNull(updated.getDeliveryTime(), "Delivery completion timestamp must be recorded");
        assertEquals(PickupRequestStatus.COMPLETED, delivery.getPickupRequest().getStatus(), "Request must be marked COMPLETED");

        verify(notificationService, times(2)).sendNotification(any(), any(), any(), eq(NotificationType.DELIVERED));
    }

    @Test
    void testUpdateStatus_RejectsUnauthorizedVolunteer() {
        when(deliveryRepository.findById(700L)).thenReturn(Optional.of(delivery));
        when(userRepository.findById(4L)).thenReturn(Optional.of(unauthorizedVolunteer));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                deliveryService.updateStatus(700L, DeliveryStatus.PICKED_UP, "Hacking status", null, 4L));

        assertTrue(ex.getMessage().contains("Only the assigned volunteer can update this delivery status"));
        verify(deliveryRepository, never()).save(any());
    }
}
