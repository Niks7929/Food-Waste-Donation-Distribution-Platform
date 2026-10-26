package com.foodwaste.platform.controller;

import com.foodwaste.platform.dto.DonationFilterDto;
import com.foodwaste.platform.dto.OrganizationProfileDto;
import com.foodwaste.platform.dto.PickupRequestDto;
import com.foodwaste.platform.entity.FoodDonation;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.PickupRequest;
import com.foodwaste.platform.enums.FoodCategory;
import com.foodwaste.platform.enums.FoodType;
import com.foodwaste.platform.enums.OrganizationType;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.security.CustomUserDetails;
import com.foodwaste.platform.service.FoodDonationService;
import com.foodwaste.platform.service.OrganizationService;
import com.foodwaste.platform.service.PickupRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ngo")
public class NgoController {

    private final FoodDonationService foodDonationService;
    private final PickupRequestService pickupRequestService;
    private final OrganizationService organizationService;

    public NgoController(FoodDonationService foodDonationService,
                         PickupRequestService pickupRequestService,
                         OrganizationService organizationService) {
        this.foodDonationService = foodDonationService;
        this.pickupRequestService = pickupRequestService;
        this.organizationService = organizationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Organization org = organizationService.getOrganizationByUserId(userDetails.getId());
        Page<FoodDonation> availableDonations = foodDonationService.getAvailableDonations(null, PageRequest.of(0, 4));
        Page<PickupRequest> myRequests = pickupRequestService.getRequestsForNgo(userDetails.getId(), PageRequest.of(0, 5));

        model.addAttribute("organization", org);
        model.addAttribute("availableDonations", availableDonations.getContent());
        model.addAttribute("recentRequests", myRequests.getContent());
        model.addAttribute("totalRequests", myRequests.getTotalElements());

        return "ngo/dashboard";
    }

    @GetMapping("/donations")
    public String browseDonations(@ModelAttribute("filter") DonationFilterDto filter,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "9") int size,
                                  Model model) {
        Page<FoodDonation> donationPage = foodDonationService.getAvailableDonations(filter,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("donationPage", donationPage);
        model.addAttribute("categories", FoodCategory.values());
        model.addAttribute("foodTypes", FoodType.values());
        return "ngo/available-donations";
    }

    @GetMapping("/donations/{id}")
    public String viewDonation(@PathVariable Long id, Model model) {
        FoodDonation donation = foodDonationService.getDonationById(id);
        PickupRequestDto requestDto = new PickupRequestDto();
        requestDto.setDonationId(donation.getId());
        requestDto.setRequestedQuantity(donation.getAvailableQuantity());

        model.addAttribute("donation", donation);
        model.addAttribute("requestDto", requestDto);
        return "ngo/donation-detail";
    }

    @PostMapping("/requests/create")
    public String requestDonation(@Valid @ModelAttribute("requestDto") PickupRequestDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a valid quantity and pickup time.");
            return "redirect:/ngo/donations/" + dto.getDonationId();
        }

        try {
            pickupRequestService.requestDonation(dto, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pickup request submitted successfully! The donor has been notified to review your request.");
            return "redirect:/ngo/requests";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/ngo/donations/" + dto.getDonationId();
        }
    }

    @GetMapping("/requests")
    public String myRequests(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        Page<PickupRequest> requestPage = pickupRequestService.getRequestsForNgo(userDetails.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("requestPage", requestPage);
        return "ngo/requests";
    }

    @PostMapping("/requests/{id}/cancel")
    public String cancelRequest(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "Cancelled by NGO") String reason,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            pickupRequestService.cancelRequest(id, userDetails.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Pickup request cancelled. Reserved quantity restored.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/ngo/requests";
    }

    @GetMapping("/history")
    public String distributionHistory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      Model model) {
        Page<PickupRequest> requestPage = pickupRequestService.getRequestsForNgo(userDetails.getId(),
                PageRequest.of(page, size, Sort.by("updatedAt").descending()));
        model.addAttribute("requestPage", requestPage);
        return "ngo/history";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Organization org = organizationService.getOrganizationByUserId(userDetails.getId());
        OrganizationProfileDto dto = new OrganizationProfileDto();
        dto.setId(org.getId());
        dto.setOrganizationName(org.getOrganizationName());
        dto.setOrganizationType(org.getOrganizationType());
        dto.setRegistrationNumber(org.getRegistrationNumber());
        dto.setAddress(org.getAddress());
        dto.setCity(org.getCity());
        dto.setState(org.getState());
        dto.setPostalCode(org.getPostalCode());
        dto.setContactPersonName(org.getUser().getFullName());
        dto.setPhone(org.getUser().getPhone());

        model.addAttribute("profile", dto);
        model.addAttribute("organization", org);
        model.addAttribute("orgTypes", OrganizationType.values());
        return "ngo/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profile") OrganizationProfileDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("orgTypes", OrganizationType.values());
            return "ngo/profile";
        }

        try {
            organizationService.updateProfile(userDetails.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Organization profile updated successfully.");
            return "redirect:/ngo/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("orgTypes", OrganizationType.values());
            return "ngo/profile";
        }
    }
}
