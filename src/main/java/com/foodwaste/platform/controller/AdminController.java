package com.foodwaste.platform.controller;

import com.foodwaste.platform.entity.*;
import com.foodwaste.platform.enums.AccountStatus;
import com.foodwaste.platform.enums.DeliveryStatus;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.repository.UserRepository;
import com.foodwaste.platform.security.CustomUserDetails;
import com.foodwaste.platform.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReportService reportService;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final FoodDonationService foodDonationService;
    private final DeliveryService deliveryService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final ContactMessageService contactMessageService;

    public AdminController(ReportService reportService,
                           OrganizationService organizationService,
                           UserService userService,
                           FoodDonationService foodDonationService,
                           DeliveryService deliveryService,
                           ActivityLogService activityLogService,
                           UserRepository userRepository,
                           ContactMessageService contactMessageService) {
        this.reportService = reportService;
        this.organizationService = organizationService;
        this.userService = userService;
        this.foodDonationService = foodDonationService;
        this.deliveryService = deliveryService;
        this.activityLogService = activityLogService;
        this.userRepository = userRepository;
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", reportService.getDashboardStats());
        model.addAttribute("pendingOrgs", organizationService.getPendingOrganizations(PageRequest.of(0, 5)).getContent());
        model.addAttribute("recentLogs", activityLogService.getRecentLogs(PageRequest.of(0, 8)).getContent());
        model.addAttribute("recentDonations", foodDonationService.getAllDonations(PageRequest.of(0, 5, Sort.by("createdAt").descending())).getContent());
        model.addAttribute("unreadInquiries", contactMessageService.getUnreadCount());
        return "admin/dashboard";
    }

    @GetMapping("/organizations")
    public String listOrganizations(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model) {
        Page<Organization> orgPage = organizationService.getAllOrganizations(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("orgPage", orgPage);
        return "admin/organizations";
    }

    @PostMapping("/organizations/{id}/approve")
    public String approveOrganization(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        try {
            organizationService.approveOrganization(id, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Organization approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/organizations";
    }

    @PostMapping("/organizations/{id}/reject")
    public String rejectOrganization(@PathVariable Long id,
                                     @RequestParam(required = false) String reason,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            organizationService.rejectOrganization(id, userDetails.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Organization rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/organizations";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "15") int size,
                            Model model) {
        Page<User> usersPage = userService.getAllUsers(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("accountStatuses", AccountStatus.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/status")
    public String updateUserStatus(@PathVariable Long id,
                                   @RequestParam AccountStatus status,
                                   @RequestParam(required = false) String remarks,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserAccountStatus(id, status, userDetails.getId(), remarks);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/donations")
    public String listDonations(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        Page<FoodDonation> donationPage = foodDonationService.getAllDonations(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("donationPage", donationPage);
        return "admin/donations";
    }

    @GetMapping("/deliveries")
    public String listDeliveries(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) DeliveryStatus status,
                                 Model model) {
        Page<Delivery> deliveryPage = deliveryService.getAllDeliveries(status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        List<User> volunteers = userRepository.findByRole(Role.VOLUNTEER);

        model.addAttribute("deliveryPage", deliveryPage);
        model.addAttribute("volunteers", volunteers);
        model.addAttribute("currentStatus", status);
        model.addAttribute("deliveryStatuses", DeliveryStatus.values());
        return "admin/deliveries";
    }

    @PostMapping("/deliveries/{id}/assign")
    public String assignVolunteer(@PathVariable Long id,
                                  @RequestParam Long volunteerId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            deliveryService.assignVolunteer(id, volunteerId, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Volunteer assigned to delivery successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/deliveries";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("reports", reportService.getReportsData());
        return "admin/reports";
    }

    @GetMapping("/activity-logs")
    public String activityLogs(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               Model model) {
        Page<ActivityLog> logsPage = activityLogService.getRecentLogs(PageRequest.of(page, size));
        model.addAttribute("logsPage", logsPage);
        return "admin/activity-logs";
    }

    @GetMapping("/inquiries")
    public String inquiries(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "15") int size,
                            Model model) {
        Page<ContactMessage> inquiriesPage = contactMessageService.getAllMessages(PageRequest.of(page, size));
        model.addAttribute("inquiriesPage", inquiriesPage);
        model.addAttribute("unreadCount", contactMessageService.getUnreadCount());
        return "admin/inquiries";
    }

    @PostMapping("/inquiries/{id}/read")
    public String markInquiryRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        contactMessageService.markAsRead(id);
        redirectAttributes.addFlashAttribute("successMessage", "Inquiry marked as read.");
        return "redirect:/admin/inquiries";
    }

    @PostMapping("/inquiries/{id}/delete")
    public String deleteInquiry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        contactMessageService.deleteMessage(id);
        redirectAttributes.addFlashAttribute("successMessage", "Inquiry deleted successfully.");
        return "redirect:/admin/inquiries";
    }
}
