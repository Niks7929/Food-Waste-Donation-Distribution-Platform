package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.UserRegistrationDto;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.OrganizationType;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.repository.OrganizationRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDto validDonorDto;

    @BeforeEach
    void setUp() {
        validDonorDto = new UserRegistrationDto();
        validDonorDto.setFullName("Test Donor");
        validDonorDto.setEmail("donor@test.com");
        validDonorDto.setPassword("Secret@123");
        validDonorDto.setConfirmPassword("Secret@123");
        validDonorDto.setPhone("+919876543210");
        validDonorDto.setRole(Role.DONOR);
        validDonorDto.setOrganizationName("Test Bistro");
        validDonorDto.setOrganizationType(OrganizationType.RESTAURANT);
        validDonorDto.setAddress("123 Street");
        validDonorDto.setCity("Mumbai");
        validDonorDto.setState("MH");
        validDonorDto.setPostalCode("400001");
    }

    @Test
    void testRegisterUser_SuccessfulDonorRegistration_HashesPasswordAndSetsPendingStatus() {
        when(userRepository.existsByEmail("donor@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("$2a$10$hashedPasswordSample");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        User registered = userService.registerUser(validDonorDto);

        assertNotNull(registered);
        assertEquals("Test Donor", registered.getFullName());
        assertEquals("donor@test.com", registered.getEmail());
        assertEquals(AccountStatus.PENDING, registered.getAccountStatus(), "Donor accounts must start as PENDING for admin approval");
        assertEquals(Role.DONOR, registered.getRole());
        assertEquals("$2a$10$hashedPasswordSample", registered.getPasswordHash());

        verify(passwordEncoder).encode("Secret@123");
        verify(organizationRepository).save(any(Organization.class));
        verify(activityLogService).logActivity(any(), eq("USER_REGISTERED"), eq("USER"), eq(10L), any());
    }

    @Test
    void testRegisterUser_ProhibitsAdminRegistration() {
        validDonorDto.setRole(Role.ADMIN);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.registerUser(validDonorDto));
        assertTrue(ex.getMessage().contains("Administrative accounts cannot be registered publicly"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenPasswordsDoNotMatch() {
        validDonorDto.setConfirmPassword("MismatchPass");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.registerUser(validDonorDto));
        assertTrue(ex.getMessage().contains("Password confirmation does not match"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("donor@test.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.registerUser(validDonorDto));
        assertTrue(ex.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any());
    }
}
