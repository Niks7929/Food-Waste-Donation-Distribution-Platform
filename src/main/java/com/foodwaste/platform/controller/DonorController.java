package com.foodwaste.platform.controller;

import com.foodwaste.platform.dto.DonationCreateDto;
import com.foodwaste.platform.dto.OrganizationProfileDto;
import com.foodwaste.platform.entity.FoodDonation;
import com.foodwaste.platform.entity.Organization;
import com.foodwaste.platform.entity.PickupRequest;
import com.foodwaste.platform.enums.FoodCategory;
import com.foodwaste.platform.enums.FoodType;
import com.foodwaste.platform.enums.OrganizationType;
import com.foodwaste.platform.enums.QuantityUnit;
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

import java.util.List;

@Controller
@RequestMapping("/donor")
public class DonorController {

    private final FoodDonationService foodDonationService;
    private final PickupRequestService pickupRequestService;
    private final OrganizationService organizationService;

    public DonorController(FoodDonationService foodDonationService,
                           PickupRequestService pickupRequestService,
                           OrganizationService organizationService) {
        this.foodDonationService = foodDonationService;
        this.pickupRequestService = pickupRequestService;
        this.organizationService = organizationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Organization org = organizationService.getOrganizationByUserId(userDetails.getId());
        Page<FoodDonation> donations = foodDonationService.getDonationsByDonor(userDetails.getId(), PageRequest.of(0, 5));
        Page<PickupRequest> requests = pickupRequestService.getRequestsForDonor(userDetails.getId(), PageRequest.of(0, 5));

        model.addAttribute("organization", org);
        model.addAttribute("recentDonations", donations.getContent());
        model.addAttribute("recentRequests", requests.getContent());
        model.addAttribute("totalDonations", donations.getTotalElements());
        model.addAttribute("totalRequests", requests.getTotalElements());

        return "donor/dashboard";
    }

    @GetMapping("/donations")
    public String myDonations(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Page<FoodDonation> donationPage = foodDonationService.getDonationsByDonor(userDetails.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("donationPage", donationPage);
        return "donor/donations";
    }

    @GetMapping("/donations/create")
    public String createDonationForm(Model model) {
        if (!model.containsAttribute("donation")) {
            model.addAttribute("donation", new DonationCreateDto());
        }
        populateDonationEnums(model);
        return "donor/create-donation";
    }

    @PostMapping("/donations/create")
    public String handleCreateDonation(@Valid @ModelAttribute("donation") DonationCreateDto dto,
                                       BindingResult bindingResult,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateDonationEnums(model);
            return "donor/create-donation";
        }

        try {
            FoodDonation created = foodDonationService.createDonation(dto, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Food donation listing '" + created.getFoodName() + "' has been published successfully!");
            return "redirect:/donor/donations";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateDonationEnums(model);
            return "donor/create-donation";
        }
    }

    @GetMapping("/donations/{id}")
    public String viewDonation(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        FoodDonation donation = foodDonationService.getDonationById(id);
        List<PickupRequest> requests = pickupRequestService.getRequestsByDonationId(id);

        model.addAttribute("donation", donation);
        model.addAttribute("requests", requests);
        return "donor/donation-detail";
    }

    @GetMapping("/donations/{id}/edit")
    public String editDonationForm(@PathVariable Long id,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        FoodDonation donation = foodDonationService.getDonationById(id);

        DonationCreateDto dto = new DonationCreateDto();
        dto.setId(donation.getId());
        dto.setFoodName(donation.getFoodName());
        dto.setDescription(donation.getDescription());
        dto.setCategory(donation.getCategory());
        dto.setFoodType(donation.getFoodType());
        dto.setTotalQuantity(donation.getTotalQuantity());
        dto.setQuantityUnit(donation.getQuantityUnit());
        dto.setPreparedAt(donation.getPreparedAt());
        dto.setPickupDeadline(donation.getPickupDeadline());
        dto.setPickupAddress(donation.getPickupAddress());
        dto.setStorageInstructions(donation.getStorageInstructions());
        dto.setAllergenInformation(donation.getAllergenInformation());
        dto.setExistingImagePath(donation.getImagePath());

        model.addAttribute("donation", dto);
        populateDonationEnums(model);
        return "donor/edit-donation";
    }

    @PostMapping("/donations/{id}/edit")
    public String handleEditDonation(@PathVariable Long id,
                                     @Valid @ModelAttribute("donation") DonationCreateDto dto,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            populateDonationEnums(model);
            return "donor/edit-donation";
        }

        try {
            foodDonationService.updateDonation(id, dto, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Donation updated successfully.");
            return "redirect:/donor/donations/" + id;
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            populateDonationEnums(model);
            return "donor/edit-donation";
        }
    }

    @PostMapping("/donations/{id}/cancel")
    public String cancelDonation(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "Cancelled by donor") String reason,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            foodDonationService.cancelDonation(id, userDetails.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Donation listing has been cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/donations";
    }

    @GetMapping("/requests")
    public String incomingRequests(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   Model model) {
        Page<PickupRequest> requestPage = pickupRequestService.getRequestsForDonor(userDetails.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("requestPage", requestPage);
        return "donor/requests";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(required = false) String remarks,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            pickupRequestService.reviewRequest(id, true, userDetails.getId(), remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Pickup request approved! A delivery task has been created for volunteers.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/requests";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam(required = false) String remarks,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            pickupRequestService.reviewRequest(id, false, userDetails.getId(), remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Pickup request rejected. Reserved quantity has been restored.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/donor/requests";
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
        return "donor/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profile") OrganizationProfileDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("orgTypes", OrganizationType.values());
            return "donor/profile";
        }

        try {
            organizationService.updateProfile(userDetails.getId(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Organization profile updated successfully.");
            return "redirect:/donor/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("orgTypes", OrganizationType.values());
            return "donor/profile";
        }
    }

    private void populateDonationEnums(Model model) {
        model.addAttribute("categories", FoodCategory.values());
        model.addAttribute("foodTypes", FoodType.values());
        model.addAttribute("units", QuantityUnit.values());
    }
}
