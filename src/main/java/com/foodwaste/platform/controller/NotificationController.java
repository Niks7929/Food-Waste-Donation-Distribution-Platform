package com.foodwaste.platform.controller;

import com.foodwaste.platform.entity.Notification;
import com.foodwaste.platform.security.CustomUserDetails;
import com.foodwaste.platform.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listNotifications(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "15") int size,
                                    Model model) {
        Page<Notification> notificationPage = notificationService.getUserNotifications(userDetails.getId(), PageRequest.of(page, size));
        long unreadCount = notificationService.getUnreadCount(userDetails.getId());

        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("unreadCount", unreadCount);
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        notificationService.markAsRead(id, userDetails.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        notificationService.markAllRead(userDetails.getId());
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
