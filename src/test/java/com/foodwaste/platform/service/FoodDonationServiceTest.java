package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.DonationCreateDto;
import com.foodwaste.platform.entity.FoodDonation;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.*;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.repository.DonationStatusHistoryRepository;
import com.foodwaste.platform.repository.FoodDonationRepository;
import com.foodwaste.platform.repository.OrganizationRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodDonationServiceTest {

    @Mock
    private FoodDonationRepository foodDonationRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DonationStatusHistoryRepository donationStatusHistoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private FoodDonationService foodDonationService;

    private User approvedDonorUser;
    private Organization approvedOrg;
    private DonationCreateDto validDto;

    @BeforeEach
    void setUp() {
        approvedDonorUser = new User();
        approvedDonorUser.setId(1L);
        approvedDonorUser.setRole(Role.DONOR);
        approvedDonorUser.setAccountStatus(AccountStatus.APPROVED);

        approvedOrg = new Organization();
        approvedOrg.setId(10L);
        approvedOrg.setUser(approvedDonorUser);
        approvedOrg.setOrganizationName("Test Bistro");
        approvedOrg.setApprovalStatus(AccountStatus.APPROVED);

        validDto = new DonationCreateDto();
        validDto.setFoodName("Fresh Meals");
        validDto.setCategory(FoodCategory.COOKED_MEALS);
        validDto.setFoodType(FoodType.VEGETARIAN);
        validDto.setTotalQuantity(new BigDecimal("50"));
        validDto.setQuantityUnit(QuantityUnit.MEALS);
        validDto.setPreparedAt(LocalDateTime.now().minusHours(1));
        validDto.setPickupDeadline(LocalDateTime.now().plusHours(5));
        validDto.setPickupAddress("123 Food Street");
    }

    @Test
    void testCreateDonation_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(approvedDonorUser));
        when(organizationRepository.findByUserId(1L)).thenReturn(Optional.of(approvedOrg));
        when(foodDonationRepository.save(any(FoodDonation.class))).thenAnswer(i -> {
            FoodDonation d = i.getArgument(0);
            d.setId(100L);
            return d;
        });

        FoodDonation created = foodDonationService.createDonation(validDto, 1L);

        assertNotNull(created);
        assertEquals("Fresh Meals", created.getFoodName());
        assertEquals(new BigDecimal("50"), created.getTotalQuantity());
        assertEquals(new BigDecimal("50"), created.getAvailableQuantity());
        assertEquals(DonationStatus.AVAILABLE, created.getStatus());

        verify(foodDonationRepository).save(any(FoodDonation.class));
        verify(donationStatusHistoryRepository).save(any());
    }

    @Test
    void testCreateDonation_RejectsPastPickupDeadline() {
        validDto.setPickupDeadline(LocalDateTime.now().minusHours(2)); // Past deadline!

        when(userRepository.findById(1L)).thenReturn(Optional.of(approvedDonorUser));
        when(organizationRepository.findByUserId(1L)).thenReturn(Optional.of(approvedOrg));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodDonationService.createDonation(validDto, 1L));
        assertTrue(ex.getMessage().contains("must be in the future"));
        verify(foodDonationRepository, never()).save(any());
    }

    @Test
    void testCreateDonation_RejectsZeroOrNegativeQuantity() {
        validDto.setTotalQuantity(new BigDecimal("0")); // Invalid quantity!

        when(userRepository.findById(1L)).thenReturn(Optional.of(approvedDonorUser));
        when(organizationRepository.findByUserId(1L)).thenReturn(Optional.of(approvedOrg));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodDonationService.createDonation(validDto, 1L));
        assertTrue(ex.getMessage().contains("greater than zero"));
        verify(foodDonationRepository, never()).save(any());
    }

    @Test
    void testCreateDonation_RejectsUnapprovedOrganization() {
        approvedOrg.setApprovalStatus(AccountStatus.PENDING); // Not yet approved!

        when(userRepository.findById(1L)).thenReturn(Optional.of(approvedDonorUser));
        when(organizationRepository.findByUserId(1L)).thenReturn(Optional.of(approvedOrg));

        BusinessException ex = assertThrows(BusinessException.class, () -> foodDonationService.createDonation(validDto, 1L));
        assertTrue(ex.getMessage().contains("not been approved yet"));
        verify(foodDonationRepository, never()).save(any());
    }

    @Test
    void testExpirePastDonations() {
        FoodDonation expiredDonation = new FoodDonation();
        expiredDonation.setId(200L);
        expiredDonation.setStatus(DonationStatus.AVAILABLE);
        expiredDonation.setPickupDeadline(LocalDateTime.now().minusHours(1));
        expiredDonation.setAvailableQuantity(new BigDecimal("20"));

        when(foodDonationRepository.findByStatusInAndPickupDeadlineBefore(any(), any()))
                .thenReturn(Collections.singletonList(expiredDonation));

        int expiredCount = foodDonationService.expirePastDonations();

        assertEquals(1, expiredCount);
        assertEquals(DonationStatus.EXPIRED, expiredDonation.getStatus());
        assertEquals(BigDecimal.ZERO, expiredDonation.getAvailableQuantity());
        verify(foodDonationRepository).save(expiredDonation);
    }
}
