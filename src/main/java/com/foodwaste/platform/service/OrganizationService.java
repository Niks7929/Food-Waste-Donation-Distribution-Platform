package com.foodwaste.platform.service;

import com.foodwaste.platform.dto.OrganizationProfileDto;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.NotificationType;
import com.foodwaste.platform.exception.ResourceNotFoundException;
import com.foodwaste.platform.repository.OrganizationRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               ActivityLogService activityLogService,
                               NotificationService notificationService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationByUserId(Long userId) {
        return organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No organization profile found for user ID: " + userId));
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Organization> getPendingOrganizations(Pageable pageable) {
        return organizationRepository.findByApprovalStatus(AccountStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);
    }

    @Transactional
    public Organization updateProfile(Long userId, OrganizationProfileDto dto) {
        Organization org = getOrganizationByUserId(userId);
        User user = org.getUser();

        org.setOrganizationName(dto.getOrganizationName().trim());
        org.setOrganizationType(dto.getOrganizationType());
        org.setRegistrationNumber(dto.getRegistrationNumber());
        org.setAddress(dto.getAddress().trim());
        org.setCity(dto.getCity().trim());
        org.setState(dto.getState().trim());
        org.setPostalCode(dto.getPostalCode().trim());

        if (dto.getContactPersonName() != null && !dto.getContactPersonName().isBlank()) {
            user.setFullName(dto.getContactPersonName().trim());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            user.setPhone(dto.getPhone().trim());
        }

        userRepository.save(user);
        Organization savedOrg = organizationRepository.save(org);

        activityLogService.logActivity(user, "PROFILE_UPDATED", "ORGANIZATION", org.getId(),
                "Updated organization profile details for " + org.getOrganizationName());

        return savedOrg;
    }

    @Transactional
    public void approveOrganization(Long orgId, Long adminId) {
        Organization org = getOrganizationById(orgId);
        User user = org.getUser();
        User admin = (adminId != null) ? userRepository.findById(adminId).orElse(null) : null;

        org.setApprovalStatus(AccountStatus.APPROVED);
        user.setAccountStatus(AccountStatus.APPROVED);

        organizationRepository.save(org);
        userRepository.save(user);

        activityLogService.logActivity(admin, "ORG_APPROVED", "ORGANIZATION", org.getId(),
                "Approved organization: " + org.getOrganizationName());

        notificationService.sendNotification(user, "Organization Approved!",
                "Congratulations! Your organization profile '" + org.getOrganizationName() +
                        "' has been approved by the platform administrator. You can now fully use all features.",
                NotificationType.ACCOUNT_STATUS_CHANGE);
    }

    @Transactional
    public void rejectOrganization(Long orgId, Long adminId, String reason) {
        Organization org = getOrganizationById(orgId);
        User user = org.getUser();
        User admin = (adminId != null) ? userRepository.findById(adminId).orElse(null) : null;

        org.setApprovalStatus(AccountStatus.REJECTED);
        user.setAccountStatus(AccountStatus.REJECTED);

        organizationRepository.save(org);
        userRepository.save(user);

        activityLogService.logActivity(admin, "ORG_REJECTED", "ORGANIZATION", org.getId(),
                "Rejected organization: " + org.getOrganizationName() + ". Reason: " + reason);

        notificationService.sendNotification(user, "Organization Registration Rejected",
                "Your organization registration was rejected. Reason: " + (reason != null ? reason : "Information provided did not meet criteria."),
                NotificationType.ACCOUNT_STATUS_CHANGE);
    }
}
