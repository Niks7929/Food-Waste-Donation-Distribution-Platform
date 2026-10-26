package com.foodwaste.platform.integration;

import com.foodwaste.platform.dto.DonationCreateDto;
import com.foodwaste.platform.dto.PickupRequestDto;
import com.foodwaste.platform.entity.*;
import com.foodwaste.platform.enums.*;
import com.foodwaste.platform.repository.*;
import com.foodwaste.platform.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EndToEndWorkflowIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private FoodDonationService foodDonationService;

    @Autowired
    private PickupRequestService pickupRequestService;

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FoodDonationRepository foodDonationRepository;

    @Autowired
    private PickupRequestRepository pickupRequestRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    @Transactional
    @DisplayName("Complete Real-World End-to-End Workflow: Donor -> NGO -> Donor Approve -> Admin Assign -> Volunteer Deliver")
    void testFullPlatformWorkflowEndToEnd() {
        // -------------------------------------------------------------
        // STEP 1: Identify or Setup Participating Users and Organizations
        // -------------------------------------------------------------
        User adminUser = userRepository.findByEmail("admin@123gmail.com")
                .orElseGet(() -> userRepository.save(new User("Admin", "admin@123gmail.com", "admin123", "+919999999999", Role.ADMIN, AccountStatus.APPROVED)));

        User donorUser = userRepository.findByEmail("testdonor@bistro.com")
                .orElseGet(() -> {
                    User u = new User("Test Donor", "testdonor@bistro.com", "pass123", "+919820011223", Role.DONOR, AccountStatus.APPROVED);
                    u = userRepository.save(u);
                    Organization o = new Organization();
                    o.setUser(u);
                    o.setOrganizationName("Bistro Test");
                    o.setOrganizationType(OrganizationType.RESTAURANT);
                    o.setRegistrationNumber("FSSAI-999");
                    o.setAddress("Bandra, Mumbai");
                    o.setCity("Mumbai");
                    o.setState("Maharashtra");
                    o.setPostalCode("400050");
                    o.setApprovalStatus(AccountStatus.APPROVED);
                    organizationRepository.save(o);
                    return u;
                });
        Organization donorOrg = organizationService.getOrganizationByUserId(donorUser.getId());
        assertNotNull(donorOrg, "Donor organization must exist");

        User ngoUser = userRepository.findByEmail("testngo@carefeed.org")
                .orElseGet(() -> {
                    User u = new User("Test NGO", "testngo@carefeed.org", "pass123", "+919830011223", Role.NGO, AccountStatus.APPROVED);
                    u = userRepository.save(u);
                    Organization o = new Organization();
                    o.setUser(u);
                    o.setOrganizationName("Care NGO");
                    o.setOrganizationType(OrganizationType.NGO);
                    o.setRegistrationNumber("NGO-999");
                    o.setAddress("Dharavi, Mumbai");
                    o.setCity("Mumbai");
                    o.setState("Maharashtra");
                    o.setPostalCode("400017");
                    o.setApprovalStatus(AccountStatus.APPROVED);
                    organizationRepository.save(o);
                    return u;
                });
        Organization ngoOrg = organizationService.getOrganizationByUserId(ngoUser.getId());
        assertNotNull(ngoOrg, "NGO organization must exist");

        User volunteerUser = userRepository.findByEmail("testvolunteer@foodwaste.org")
                .orElseGet(() -> userRepository.save(new User("Test Volunteer", "testvolunteer@foodwaste.org", "pass123", "+919840011223", Role.VOLUNTEER, AccountStatus.APPROVED)));

        // -------------------------------------------------------------
        // STEP 2: Donor creates a Food Listing (50 meals)
        // -------------------------------------------------------------
        DonationCreateDto donationDto = new DonationCreateDto();
        donationDto.setFoodName("Fresh Veg Biryani Boxes");
        donationDto.setDescription("Freshly cooked sealed vegetable biryani boxes with raita");
        donationDto.setCategory(FoodCategory.COOKED_MEALS);
        donationDto.setFoodType(FoodType.VEGETARIAN);
        donationDto.setTotalQuantity(new BigDecimal("50.00"));
        donationDto.setQuantityUnit(QuantityUnit.PACKETS);
        donationDto.setPreparedAt(LocalDateTime.now().minusHours(1));
        donationDto.setPickupDeadline(LocalDateTime.now().plusHours(4));
        donationDto.setPickupAddress(donorOrg.getAddress());
        donationDto.setStorageInstructions("Keep sealed in insulated bags until distribution");
        donationDto.setAllergenInformation("Contains dairy (ghee/paneer)");

        FoodDonation createdDonation = foodDonationService.createDonation(donationDto, donorUser.getId());
        assertNotNull(createdDonation.getId(), "Donation must be saved with generated ID");
        assertEquals(DonationStatus.AVAILABLE, createdDonation.getStatus(), "Initial status must be AVAILABLE");
        assertEquals(new BigDecimal("50.00"), createdDonation.getAvailableQuantity());

        // -------------------------------------------------------------
        // STEP 3: NGO browses and submits Pickup Request for 20 meals
        // -------------------------------------------------------------
        PickupRequestDto requestDto = new PickupRequestDto();
        requestDto.setDonationId(createdDonation.getId());
        requestDto.setRequestedQuantity(new BigDecimal("20.00"));
        requestDto.setRequestedPickupTime(LocalDateTime.now().plusHours(2));

        PickupRequest submittedRequest = pickupRequestService.requestDonation(requestDto, ngoUser.getId());
        assertNotNull(submittedRequest.getId(), "PickupRequest must have an ID");
        assertEquals(PickupRequestStatus.PENDING, submittedRequest.getStatus());

        // Verify atomic inventory reservation: 50 - 20 = 30 remaining
        FoodDonation lockedDonation = foodDonationRepository.findById(createdDonation.getId()).orElseThrow();
        assertEquals(new BigDecimal("30.00"), lockedDonation.getAvailableQuantity());
        assertEquals(DonationStatus.PARTIALLY_RESERVED, lockedDonation.getStatus());

        // -------------------------------------------------------------
        // STEP 4: Donor reviews & Approves the Pickup Request
        // -------------------------------------------------------------
        pickupRequestService.reviewRequest(submittedRequest.getId(), true, donorUser.getId(), "Approved for distribution");

        PickupRequest approvedRequest = pickupRequestRepository.findById(submittedRequest.getId()).orElseThrow();
        assertEquals(PickupRequestStatus.APPROVED, approvedRequest.getStatus());

        // Verify Delivery task was automatically created by the system
        Delivery createdDelivery = deliveryRepository.findByPickupRequestId(approvedRequest.getId())
                .orElseThrow(() -> new AssertionError("Delivery task must be automatically generated upon request approval"));
        assertEquals(DeliveryStatus.ASSIGNED, createdDelivery.getDeliveryStatus());

        // -------------------------------------------------------------
        // STEP 5: Admin assigns Volunteer to the Delivery task
        // -------------------------------------------------------------
        Delivery assignedDelivery = deliveryService.assignVolunteer(createdDelivery.getId(), volunteerUser.getId(), adminUser.getId());
        assertNotNull(assignedDelivery.getVolunteer());
        assertEquals(volunteerUser.getId(), assignedDelivery.getVolunteer().getId());

        // -------------------------------------------------------------
        // STEP 6: Volunteer confirms Pickup from Donor location
        // -------------------------------------------------------------
        Delivery pickedUpDelivery = deliveryService.updateStatus(
                assignedDelivery.getId(),
                DeliveryStatus.PICKED_UP,
                "Picked up from donor counter in good condition",
                null,
                volunteerUser.getId()
        );
        assertEquals(DeliveryStatus.PICKED_UP, pickedUpDelivery.getDeliveryStatus());
        assertNotNull(pickedUpDelivery.getPickupTime());

        // -------------------------------------------------------------
        // STEP 7: Volunteer delivers food to NGO Community Center
        // -------------------------------------------------------------
        Delivery completedDelivery = deliveryService.updateStatus(
                pickedUpDelivery.getId(),
                DeliveryStatus.DELIVERED,
                "Delivered safely to NGO coordinator",
                "CONF-REF-" + System.currentTimeMillis(),
                volunteerUser.getId()
        );
        assertEquals(DeliveryStatus.DELIVERED, completedDelivery.getDeliveryStatus());
        assertNotNull(completedDelivery.getDeliveryTime());

        // -------------------------------------------------------------
        // STEP 8: Verify Complete Ecosystem Status Updates & Audit Notifications
        // -------------------------------------------------------------
        // Pickup request must be marked COMPLETED
        PickupRequest finalRequest = pickupRequestRepository.findById(submittedRequest.getId()).orElseThrow();
        assertEquals(PickupRequestStatus.COMPLETED, finalRequest.getStatus());

        // Notifications sent to Donor and NGO verifying delivery
        List<Notification> ngoNotifications = notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(ngoUser.getId());
        boolean ngoDeliveredNotified = ngoNotifications.stream()
                .anyMatch(n -> n.getNotificationType() == NotificationType.DELIVERED);
        assertTrue(ngoDeliveredNotified, "NGO must receive delivery completion notification");

        List<Notification> donorNotifications = notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(donorUser.getId());
        boolean donorDeliveredNotified = donorNotifications.stream()
                .anyMatch(n -> n.getNotificationType() == NotificationType.DELIVERED);
        assertTrue(donorDeliveredNotified, "Donor must receive delivery completion notification");
    }
}
