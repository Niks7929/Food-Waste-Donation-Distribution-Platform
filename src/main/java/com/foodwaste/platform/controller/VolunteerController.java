package com.foodwaste.platform.controller;

import com.foodwaste.platform.entity.Delivery;
import com.foodwaste.platform.enums.DeliveryStatus;
import com.foodwaste.platform.security.CustomUserDetails;
import com.foodwaste.platform.service.DeliveryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/volunteer")
public class VolunteerController {

    private final DeliveryService deliveryService;

    public VolunteerController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Page<Delivery> myDeliveries = deliveryService.getDeliveriesForVolunteer(userDetails.getId(), PageRequest.of(0, 5));
        Page<Delivery> availableTasks = deliveryService.getUnassignedDeliveries(PageRequest.of(0, 5));

        model.addAttribute("myDeliveries", myDeliveries.getContent());
        model.addAttribute("availableTasks", availableTasks.getContent());
        model.addAttribute("totalAssigned", myDeliveries.getTotalElements());
        model.addAttribute("totalAvailable", availableTasks.getTotalElements());

        return "volunteer/dashboard";
    }

    @GetMapping("/tasks")
    public String myTasks(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          Model model) {
        Page<Delivery> taskPage = deliveryService.getDeliveriesForVolunteer(userDetails.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("taskPage", taskPage);
        return "volunteer/tasks";
    }

    @GetMapping("/tasks/{id}")
    public String viewTask(@PathVariable Long id, Model model) {
        Delivery delivery = deliveryService.getDeliveryById(id);
        model.addAttribute("delivery", delivery);
        model.addAttribute("statuses", DeliveryStatus.values());
        return "volunteer/task-detail";
    }

    @PostMapping("/tasks/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam DeliveryStatus newStatus,
                               @RequestParam(required = false) String notes,
                               @RequestParam(required = false) String confirmationReference,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            deliveryService.updateStatus(id, newStatus, notes, confirmationReference, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Delivery task status updated to: " + newStatus);
            return "redirect:/volunteer/tasks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/volunteer/tasks/" + id;
        }
    }

    @GetMapping("/available")
    public String availableTasks(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        Page<Delivery> availablePage = deliveryService.getUnassignedDeliveries(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        model.addAttribute("availablePage", availablePage);
        return "volunteer/available-tasks";
    }

    @PostMapping("/tasks/{id}/claim")
    public String claimTask(@PathVariable Long id,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            deliveryService.assignVolunteer(id, userDetails.getId(), null);
            redirectAttributes.addFlashAttribute("successMessage", "Task claimed successfully! You are now assigned to this delivery.");
            return "redirect:/volunteer/tasks/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/volunteer/available";
        }
    }

    @GetMapping("/history")
    public String deliveryHistory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
        Page<Delivery> historyPage = deliveryService.getDeliveriesForVolunteer(userDetails.getId(),
                PageRequest.of(page, size, Sort.by("updatedAt").descending()));
        model.addAttribute("historyPage", historyPage);
        return "volunteer/history";
    }
}
