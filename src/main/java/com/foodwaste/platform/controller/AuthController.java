package com.foodwaste.platform.controller;

import com.foodwaste.platform.dto.UserRegistrationDto;
import com.foodwaste.platform.enums.OrganizationType;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.exception.BusinessException;
import com.foodwaste.platform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new UserRegistrationDto());
        }

        // Roles permitted for public registration (NEVER ADMIN)
        List<Role> allowedRoles = Arrays.asList(Role.DONOR, Role.NGO, Role.VOLUNTEER);
        model.addAttribute("roles", allowedRoles);
        model.addAttribute("orgTypes", OrganizationType.values());

        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute("user") UserRegistrationDto dto,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Arrays.asList(Role.DONOR, Role.NGO, Role.VOLUNTEER));
            model.addAttribute("orgTypes", OrganizationType.values());
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
            if (dto.getRole() == Role.DONOR || dto.getRole() == Role.NGO) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Registration submitted successfully! Your organization profile is pending administrator approval before you can start.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Volunteer registration successful! You can now log in and browse available delivery tasks.");
            }
            return "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", Arrays.asList(Role.DONOR, Role.NGO, Role.VOLUNTEER));
            model.addAttribute("orgTypes", OrganizationType.values());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred during registration. Please check your inputs.");
            model.addAttribute("roles", Arrays.asList(Role.DONOR, Role.NGO, Role.VOLUNTEER));
            model.addAttribute("orgTypes", OrganizationType.values());
            return "auth/register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
