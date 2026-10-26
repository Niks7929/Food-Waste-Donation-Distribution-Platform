package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.PickupRequestDto;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupRequestServiceTest {

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private FoodDonationRepository foodDonationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private FoodDonationService foodDonationService;

    @InjectMocks
    private PickupRequestService pickupRequestService;

    private User ngoUser;
    private Organization ngoOrg;
    private FoodDonation donation;
    private PickupRequestDto requestDto;

    @BeforeEach
    void setUp() {
        ngoUser = new User();
        ngoUser.setId(2L);
        ngoUser.setRole(Role.NGO);
        ngoUser.setAccountStatus(AccountStatus.APPROVED);

        ngoOrg = new Organization();
        ngoOrg.setId(20L);
        ngoOrg.setUser(ngoUser);
        ngoOrg.setOrganizationName("Care Foundation");
        ngoOrg.setApprovalStatus(AccountStatus.APPROVED);

        User donorUser = new User();
        donorUser.setId(1L);

        Organization donorOrg = new Organization();
        donorOrg.setId(10L);
        donorOrg.setUser(donorUser);
        donorOrg.setOrganizationName("Heritage Bistro");

        donation = new FoodDonation();
        donation.setId(100L);
        donation.setDonorOrganization(donorOrg);
        donation.setFoodName("Pulao & Curry");
        donation.setTotalQuantity(new BigDecimal("100"));
        donation.setAvailableQuantity(new BigDecimal("100"));
        donation.setQuantityUnit(QuantityUnit.MEALS);
        donation.setStatus(DonationStatus.AVAILABLE);
        donation.setPickupDeadline(LocalDateTime.now().plusHours(4));

        requestDto = new PickupRequestDto();
        requestDto.setDonationId(100L);
        requestDto.setRequestedQuantity(new BigDecimal("60"));
        requestDto.setRequestedPickupTime(LocalDateTime.now().plusHours(2));
    }

    @Test
    void testRequestDonation_SuccessfulPartialReservation_DeductsQuantityAtomically() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(organizationRepository.findByUserId(2L)).thenReturn(Optional.of(ngoOrg));
        when(foodDonationRepository.findByIdWithLock(100L)).thenReturn(Optional.of(donation));
        when(pickupRequestRepository.existsByDonationIdAndNgoOrganizationIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(pickupRequestRepository.save(any(PickupRequest.class))).thenAnswer(i -> {
            PickupRequest r = i.getArgument(0);
            r.setId(500L);
            return r;
        });

        PickupRequest result = pickupRequestService.requestDonation(requestDto, 2L);

        assertNotNull(result);
        assertEquals(new BigDecimal("60"), result.getRequestedQuantity());
        assertEquals(PickupRequestStatus.PENDING, result.getStatus());

        // Verify atomic deduction
        assertEquals(new BigDecimal("40"), donation.getAvailableQuantity());
        assertEquals(DonationStatus.PARTIALLY_RESERVED, donation.getStatus());

        verify(foodDonationRepository).save(donation);
        verify(notificationService).sendNotification(any(), any(), any(), eq(NotificationType.REQUEST_RECEIVED));
    }

    @Test
    void testRequestDonation_FullReservation_TransitionsToReserved() {
        requestDto.setRequestedQuantity(new BigDecimal("100")); // Full quantity

        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(organizationRepository.findByUserId(2L)).thenReturn(Optional.of(ngoOrg));
        when(foodDonationRepository.findByIdWithLock(100L)).thenReturn(Optional.of(donation));
        when(pickupRequestRepository.existsByDonationIdAndNgoOrganizationIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(pickupRequestRepository.save(any(PickupRequest.class))).thenAnswer(i -> i.getArgument(0));

        pickupRequestService.requestDonation(requestDto, 2L);

        assertEquals(BigDecimal.ZERO, donation.getAvailableQuantity());
        assertEquals(DonationStatus.RESERVED, donation.getStatus());
        verify(foodDonationRepository).save(donation);
    }

    @Test
    void testRequestDonation_PreventsOverbooking_ThrowsExceptionWhenQuantityExceedsAvailable() {
        requestDto.setRequestedQuantity(new BigDecimal("120")); // Exceeds 100 available!

        when(userRepository.findById(2L)).thenReturn(Optional.of(ngoUser));
        when(organizationRepository.findByUserId(2L)).thenReturn(Optional.of(ngoOrg));
        when(foodDonationRepository.findByIdWithLock(100L)).thenReturn(Optional.of(donation));

        BusinessException ex = assertThrows(BusinessException.class, () -> pickupRequestService.requestDonation(requestDto, 2L));
        assertTrue(ex.getMessage().contains("exceeds currently available quantity"));

        // Verify quantity was NOT altered
        assertEquals(new BigDecimal("100"), donation.getAvailableQuantity());
        verify(pickupRequestRepository, never()).save(any());
    }

    @Test
    void testReviewRequest_RejectionRestoresReservedQuantity() {
        donation.setAvailableQuantity(new BigDecimal("40"));
        donation.setStatus(DonationStatus.PARTIALLY_RESERVED);

        PickupRequest pendingRequest = new PickupRequest();
        pendingRequest.setId(500L);
        pendingRequest.setDonation(donation);
        pendingRequest.setNgoOrganization(ngoOrg);
        pendingRequest.setRequestedQuantity(new BigDecimal("60"));
        pendingRequest.setStatus(PickupRequestStatus.PENDING);

        when(pickupRequestRepository.findById(500L)).thenReturn(Optional.of(pendingRequest));
        when(foodDonationRepository.findByIdWithLock(100L)).thenReturn(Optional.of(donation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(donation.getDonorOrganization().getUser()));

        pickupRequestService.reviewRequest(500L, false, 1L, "Cannot fulfill at this time");

        assertEquals(PickupRequestStatus.REJECTED, pendingRequest.getStatus());
        assertEquals(new BigDecimal("100"), donation.getAvailableQuantity(), "Reserved quantity must be restored to donation");
        assertEquals(DonationStatus.AVAILABLE, donation.getStatus());

        verify(foodDonationRepository).save(donation);
        verify(notificationService).sendNotification(any(), any(), any(), eq(NotificationType.REQUEST_REJECTED));
    }
}
