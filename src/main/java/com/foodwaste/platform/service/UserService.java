package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.UserRegistrationDto;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.NotificationType;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.exception.ResourceNotFoundException;
import com.foodwaste.platform.repository.OrganizationRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       ActivityLogService activityLogService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        // Security check: Public registration MUST NEVER allow selecting ADMIN
        if (dto.getRole() == Role.ADMIN) {
            throw new BusinessException("Administrative accounts cannot be registered publicly.");
        }

        // Validate password confirmation
        if (!dto.isPasswordMatching()) {
            throw new BusinessException("Password confirmation does not match.");
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("An account with email " + email + " is already registered.");
        }

        // Account status logic: Donor and NGO require admin approval; Volunteers can be approved or pending
        AccountStatus initialStatus = (dto.getRole() == Role.DONOR || dto.getRole() == Role.NGO)
                ? AccountStatus.PENDING
                : AccountStatus.APPROVED;

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone().trim());
        user.setRole(dto.getRole());
        user.setAccountStatus(initialStatus);

        user = userRepository.save(user);

        // If registering as DONOR or NGO, create and associate Organization entity
        if (dto.getRole() == Role.DONOR || dto.getRole() == Role.NGO) {
            if (dto.getOrganizationName() == null || dto.getOrganizationName().trim().isEmpty()) {
                throw new BusinessException("Organization name is required for " + dto.getRole());
            }

            Organization org = new Organization();
            org.setUser(user);
            org.setOrganizationName(dto.getOrganizationName().trim());
            org.setOrganizationType(dto.getOrganizationType());
            org.setRegistrationNumber(dto.getRegistrationNumber());
            org.setAddress(dto.getAddress());
            org.setCity(dto.getCity());
            org.setState(dto.getState());
            org.setPostalCode(dto.getPostalCode());
            org.setApprovalStatus(AccountStatus.PENDING);

            organizationRepository.save(org);
            user.setOrganization(org);
        }

        // Audit log
        activityLogService.logActivity(user, "USER_REGISTERED", "USER", user.getId(),
                "Registered new " + user.getRole() + " account with status " + user.getAccountStatus());

        // In-app welcome notification
        String welcomeMsg = (initialStatus == AccountStatus.PENDING)
                ? "Welcome to Food Waste Donation & Distribution Platform! Your organization profile has been submitted and is currently awaiting administrator review and approval."
                : "Welcome! Your volunteer account has been created successfully. You can now view and accept food delivery assignments.";

        notificationService.sendNotification(user, "Welcome to the Platform", welcomeMsg, NotificationType.GENERAL);

        return user;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    @Transactional
    public void updateUserAccountStatus(Long userId, AccountStatus status, Long adminId, String remarks) {
        User user = findById(userId);
        User admin = (adminId != null) ? findById(adminId) : null;

        AccountStatus oldStatus = user.getAccountStatus();
        user.setAccountStatus(status);
        userRepository.save(user);

        // Also sync organization approval status if exists
        if (user.getOrganization() != null) {
            user.getOrganization().setApprovalStatus(status);
            organizationRepository.save(user.getOrganization());
        }

        activityLogService.logActivity(admin, "ACCOUNT_STATUS_CHANGE", "USER", user.getId(),
                "Changed status of user " + user.getEmail() + " from " + oldStatus + " to " + status + ". Remarks: " + remarks);

        notificationService.sendNotification(user, "Account Status Updated",
                "Your account status has been changed to: " + status + (remarks != null && !remarks.isBlank() ? " (" + remarks + ")" : ""),
                NotificationType.ACCOUNT_STATUS_CHANGE);
    }
}
