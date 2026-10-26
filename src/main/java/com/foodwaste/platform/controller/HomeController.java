package com.foodwaste.platform.controller;

import com.foodwaste.platform.service.ContactMessageService;
import com.foodwaste.platform.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final ReportService reportService;
    private final ContactMessageService contactMessageService;

    public HomeController(ReportService reportService, ContactMessageService contactMessageService) {
        this.reportService = reportService;
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", reportService.getDashboardStats());
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/how-it-works")
    public String howItWorks() {
        return "how-it-works";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContactMessage(@RequestParam("fullName") String fullName,
                                       @RequestParam("email") String email,
                                       @RequestParam(value = "roleOrOrg", required = false) String roleOrOrg,
                                       @RequestParam("message") String message,
                                       RedirectAttributes redirectAttributes) {
        if (fullName == null || fullName.isBlank() || email == null || email.isBlank() || message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please fill in all required fields.");
            return "redirect:/contact";
        }

        contactMessageService.saveMessage(fullName.trim(), email.trim(), roleOrOrg, message.trim());
        redirectAttributes.addFlashAttribute("successMessage", "Thank you! Your message has been sent directly to the Platform Administrator. We will get back to you shortly.");
        return "redirect:/contact";
    }
}
