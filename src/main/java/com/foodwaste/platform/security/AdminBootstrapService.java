package com.foodwaste.platform.security;

import com.foodwaste.platform.entity.*;
import com.foodwaste.platform.enums.*;
import com.foodwaste.platform.repository.*;
import com.foodwaste.platform.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    @Value("${app.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.email:admin@123gmail.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.name:Platform Super Admin}")
    private String adminName;

    @Value("${app.admin.phone:+919876543210}")
    private String adminPhone;

    @Value("${app.seed.demo.data:false}")
    private boolean seedDemoData;

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final FoodDonationRepository foodDonationRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogService activityLogService;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(UserRepository userRepository,
                                 OrganizationRepository organizationRepository,
                                 FoodDonationRepository foodDonationRepository,
                                 PickupRequestRepository pickupRequestRepository,
                                 DeliveryRepository deliveryRepository,
                                 NotificationRepository notificationRepository,
                                 ActivityLogService activityLogService,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.foodDonationRepository = foodDonationRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogService = activityLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled) {
            log.info("Admin bootstrap is disabled via configuration.");
            return;
        }

        // 1. Ensure Super Administrator exists
        User admin = userRepository.findByEmail(adminEmail.toLowerCase()).orElse(null);
        if (admin == null) {
            // Check if existing admin with previous email exists and update to new credentials
            admin = userRepository.findByRole(Role.ADMIN).stream().findFirst().orElse(null);
            if (admin != null) {
                admin.setEmail(adminEmail.toLowerCase());
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin = userRepository.save(admin);
            } else {
                admin = new User();
                admin.setFullName(adminName);
                admin.setEmail(adminEmail.toLowerCase());
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setPhone(adminPhone);
                admin.setRole(Role.ADMIN);
                admin.setAccountStatus(AccountStatus.APPROVED);
                admin = userRepository.save(admin);
            }

            activityLogService.logActivity(admin, "ADMIN_BOOTSTRAP", "USER", admin.getId(),
                    "Super administrator configured with email: " + adminEmail);
            log.info("Administrator configured with email: {}", adminEmail);
        } else {
            // Always ensure the configured password is encrypted and saved
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
        }

        // 2. Seed realistic development data ONLY if explicitly enabled
        if (seedDemoData && userRepository.count() <= 1) {
            seedDevelopmentData(admin);
        }
    }

    private void seedDevelopmentData(User admin) {
        log.info("Seeding realistic development sample data for initial testing...");

        String defaultDevPassword = passwordEncoder.encode("Password@123");

        // Seed Donor 1: Green Leaf Bistro (Restaurant) - APPROVED
        User donor1User = new User("Rajesh Sharma", "donor1@bistro.com", defaultDevPassword, "+919820011223", Role.DONOR, AccountStatus.APPROVED);
        donor1User = userRepository.save(donor1User);
        Organization donor1Org = new Organization();
        donor1Org.setUser(donor1User);
        donor1Org.setOrganizationName("Green Leaf Bistro & Cafe");
        donor1Org.setOrganizationType(OrganizationType.RESTAURANT);
        donor1Org.setRegistrationNumber("FSSAI-1122334455");
        donor1Org.setAddress("42 Bandra West, Linking Road");
        donor1Org.setCity("Mumbai");
        donor1Org.setState("Maharashtra");
        donor1Org.setPostalCode("400050");
        donor1Org.setApprovalStatus(AccountStatus.APPROVED);
        donor1Org = organizationRepository.save(donor1Org);

        // Seed Donor 2: Grand Heritage Banquet (Marriage Hall) - APPROVED
        User donor2User = new User("Sunita Patil", "donor2@heritage.com", defaultDevPassword, "+919820044556", Role.DONOR, AccountStatus.APPROVED);
        donor2User = userRepository.save(donor2User);
        Organization donor2Org = new Organization();
        donor2Org.setUser(donor2User);
        donor2Org.setOrganizationName("Grand Heritage Banquet & Lawns");
        donor2Org.setOrganizationType(OrganizationType.MARRIAGE_HALL);
        donor2Org.setRegistrationNumber("MH-PUN-BNQ-7788");
        donor2Org.setAddress("Plot 15, Senapati Bapat Road");
        donor2Org.setCity("Pune");
        donor2Org.setState("Maharashtra");
        donor2Org.setPostalCode("411016");
        donor2Org.setApprovalStatus(AccountStatus.APPROVED);
        donor2Org = organizationRepository.save(donor2Org);

        // Seed Donor 3: City Fresh Bakery - PENDING (for testing admin approval)
        User donor3User = new User("Vikram Roy", "donor3@freshbake.com", defaultDevPassword, "+919820099887", Role.DONOR, AccountStatus.PENDING);
        donor3User = userRepository.save(donor3User);
        Organization donor3Org = new Organization();
        donor3Org.setUser(donor3User);
        donor3Org.setOrganizationName("City Fresh Artisan Bakery");
        donor3Org.setOrganizationType(OrganizationType.BAKERY);
        donor3Org.setRegistrationNumber("FSSAI-9988776655");
        donor3Org.setAddress("Shop 4, Deccan Gymkhana");
        donor3Org.setCity("Pune");
        donor3Org.setState("Maharashtra");
        donor3Org.setPostalCode("411004");
        donor3Org.setApprovalStatus(AccountStatus.PENDING);
        organizationRepository.save(donor3Org);

        // Seed NGO 1: Care & Feed Foundation - APPROVED
        User ngo1User = new User("Anjali Kulkarni", "ngo1@carefeed.org", defaultDevPassword, "+919830011223", Role.NGO, AccountStatus.APPROVED);
        ngo1User = userRepository.save(ngo1User);
        Organization ngo1Org = new Organization();
        ngo1Org.setUser(ngo1User);
        ngo1Org.setOrganizationName("Care & Feed Foundation");
        ngo1Org.setOrganizationType(OrganizationType.NGO);
        ngo1Org.setRegistrationNumber("NGO-TRUST-88990");
        ngo1Org.setAddress("Bldg 3, Dharavi Community Center");
        ngo1Org.setCity("Mumbai");
        ngo1Org.setState("Maharashtra");
        ngo1Org.setPostalCode("400017");
        ngo1Org.setApprovalStatus(AccountStatus.APPROVED);
        ngo1Org = organizationRepository.save(ngo1Org);

        // Seed NGO 2: Hope Daily Bread Initiative - APPROVED
        User ngo2User = new User("Rohan Joshi", "ngo2@hopedaily.org", defaultDevPassword, "+919830044556", Role.NGO, AccountStatus.APPROVED);
        ngo2User = userRepository.save(ngo2User);
        Organization ngo2Org = new Organization();
        ngo2Org.setUser(ngo2User);
        ngo2Org.setOrganizationName("Hope Daily Bread Initiative");
        ngo2Org.setOrganizationType(OrganizationType.NGO);
        ngo2Org.setRegistrationNumber("PUN-SOC-55443");
        ngo2Org.setAddress("Survey 12, Kothrud Depot");
        ngo2Org.setCity("Pune");
        ngo2Org.setState("Maharashtra");
        ngo2Org.setPostalCode("411038");
        ngo2Org.setApprovalStatus(AccountStatus.APPROVED);
        ngo2Org = organizationRepository.save(ngo2Org);

        // Seed Volunteer 1: Amit Sharma - APPROVED
        User vol1 = new User("Amit Sharma", "volunteer1@foodwaste.org", defaultDevPassword, "+919840011223", Role.VOLUNTEER, AccountStatus.APPROVED);
        userRepository.save(vol1);

        // Seed Volunteer 2: Priya Deshmukh - APPROVED
        User vol2 = new User("Priya Deshmukh", "volunteer2@foodwaste.org", defaultDevPassword, "+919840044556", Role.VOLUNTEER, AccountStatus.APPROVED);
        userRepository.save(vol2);

        // Seed Donations
        // Donation 1: AVAILABLE Cooked meals
        FoodDonation d1 = new FoodDonation();
        d1.setDonorOrganization(donor1Org);
        d1.setFoodName("Fresh Dal Makhani & Jeera Rice Packets");
        d1.setDescription("Surplus banquet lunch boxes, prepared today noon. Hot packed in hygienic foil containers.");
        d1.setCategory(FoodCategory.COOKED_MEALS);
        d1.setFoodType(FoodType.VEGETARIAN);
        d1.setTotalQuantity(new BigDecimal("50"));
        d1.setAvailableQuantity(new BigDecimal("50"));
        d1.setQuantityUnit(QuantityUnit.MEALS);
        d1.setPreparedAt(LocalDateTime.now().minusHours(2));
        d1.setPickupDeadline(LocalDateTime.now().plusHours(6));
        d1.setPickupAddress("Green Leaf Bistro, 42 Bandra West, Mumbai");
        d1.setStorageInstructions("Keep insulated or consume within 6 hours. Do not expose to direct heat.");
        d1.setAllergenInformation("Contains dairy (butter/cream). No nuts.");
        d1.setStatus(DonationStatus.AVAILABLE);
        foodDonationRepository.save(d1);

        // Donation 2: PARTIALLY_RESERVED Cooked banquet dinner
        FoodDonation d2 = new FoodDonation();
        d2.setDonorOrganization(donor2Org);
        d2.setFoodName("Assorted Paneer Curry, Rotis & Pulao");
        d2.setDescription("Surplus food from an afternoon wedding reception. Freshly packed into food-grade containers.");
        d2.setCategory(FoodCategory.COOKED_MEALS);
        d2.setFoodType(FoodType.VEGETARIAN);
        d2.setTotalQuantity(new BigDecimal("100"));
        d2.setAvailableQuantity(new BigDecimal("40"));
        d2.setQuantityUnit(QuantityUnit.MEALS);
        d2.setPreparedAt(LocalDateTime.now().minusHours(3));
        d2.setPickupDeadline(LocalDateTime.now().plusHours(8));
        d2.setPickupAddress("Grand Heritage Banquet, Plot 15, Senapati Bapat Road, Pune");
        d2.setStorageInstructions("Refrigerate if not serving immediately.");
        d2.setAllergenInformation("Dairy (paneer, ghee), wheat (rotis).");
        d2.setStatus(DonationStatus.PARTIALLY_RESERVED);
        d2 = foodDonationRepository.save(d2);

        // Create Pickup Request & Delivery for Donation 2
        PickupRequest req1 = new PickupRequest();
        req1.setDonation(d2);
        req1.setNgoOrganization(ngo2Org);
        req1.setRequestedQuantity(new BigDecimal("60"));
        req1.setQuantityUnit(QuantityUnit.MEALS);
        req1.setRequestedPickupTime(LocalDateTime.now().plusHours(2));
        req1.setStatus(PickupRequestStatus.ASSIGNED);
        req1.setReviewedBy(donor2User);
        req1.setReviewedAt(LocalDateTime.now().minusMinutes(30));
        req1 = pickupRequestRepository.save(req1);

        Delivery del1 = new Delivery();
        del1.setPickupRequest(req1);
        del1.setVolunteer(vol2);
        del1.setDeliveryStatus(DeliveryStatus.ASSIGNED);
        deliveryRepository.save(del1);

        // Donation 3: COMPLETED / DELIVERED donation
        FoodDonation d3 = new FoodDonation();
        d3.setDonorOrganization(donor1Org);
        d3.setFoodName("Whole Wheat Bread Loaves & Vegetable Sandwiches");
        d3.setDescription("Freshly baked loaves and breakfast sandwiches from morning buffet.");
        d3.setCategory(FoodCategory.BAKERY);
        d3.setFoodType(FoodType.VEGETARIAN);
        d3.setTotalQuantity(new BigDecimal("35"));
        d3.setAvailableQuantity(BigDecimal.ZERO);
        d3.setQuantityUnit(QuantityUnit.PACKETS);
        d3.setPreparedAt(LocalDateTime.now().minusDays(1));
        d3.setPickupDeadline(LocalDateTime.now().minusHours(1));
        d3.setPickupAddress("Green Leaf Bistro, 42 Bandra West, Mumbai");
        d3.setStatus(DonationStatus.DELIVERED);
        d3 = foodDonationRepository.save(d3);

        PickupRequest req2 = new PickupRequest();
        req2.setDonation(d3);
        req2.setNgoOrganization(ngo1Org);
        req2.setRequestedQuantity(new BigDecimal("35"));
        req2.setQuantityUnit(QuantityUnit.PACKETS);
        req2.setRequestedPickupTime(LocalDateTime.now().minusHours(10));
        req2.setStatus(PickupRequestStatus.COMPLETED);
        req2.setReviewedBy(donor1User);
        req2.setReviewedAt(LocalDateTime.now().minusHours(11));
        req2 = pickupRequestRepository.save(req2);

        Delivery del2 = new Delivery();
        del2.setPickupRequest(req2);
        del2.setVolunteer(vol1);
        del2.setPickupTime(LocalDateTime.now().minusHours(9));
        del2.setDeliveryTime(LocalDateTime.now().minusHours(8));
        del2.setDeliveryStatus(DeliveryStatus.DELIVERED);
        del2.setDeliveryNotes("Delivered in person to Care & Feed community shelter. All packets in good shape.");
        del2.setConfirmationReference("REF-DEL-8877");
        deliveryRepository.save(del2);

        // Seed notifications
        notificationRepository.save(new Notification(donor1User, "Welcome to the Platform",
                "Your restaurant profile has been approved. You can start creating donation listings anytime.",
                NotificationType.ACCOUNT_STATUS_CHANGE));

        notificationRepository.save(new Notification(ngo1User, "Donation Delivery Completed",
                "Your requested food (35 Packets of Whole Wheat Bread Loaves) was delivered by volunteer Amit Sharma.",
                NotificationType.DELIVERED));

        notificationRepository.save(new Notification(vol2, "New Delivery Assignment",
                "You have been assigned to pick up 60 Meals from Grand Heritage Banquet.",
                NotificationType.VOLUNTEER_ASSIGNED));

        log.info("Development sample data seeded successfully!");
    }
}
