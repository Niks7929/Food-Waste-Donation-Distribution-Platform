package com.foodwaste.platform.service;

import com.foodwaste.platform.entity.ContactMessage;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.NotificationType;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.repository.ContactMessageRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactMessageService {

    private static final Logger log = LoggerFactory.getLogger(ContactMessageService.class);

    private final ContactMessageRepository contactMessageRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ContactMessageService(ContactMessageRepository contactMessageRepository,
                                 NotificationService notificationService,
                                 UserRepository userRepository) {
        this.contactMessageRepository = contactMessageRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ContactMessage saveMessage(String fullName, String email, String roleOrOrg, String message) {
        ContactMessage contactMessage = new ContactMessage(fullName, email, roleOrOrg, message);
        contactMessage = contactMessageRepository.save(contactMessage);

        log.info("New public inquiry received from: {} ({})", fullName, email);

        // Notify all platform administrators
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        String preview = message.length() > 90 ? message.substring(0, 90) + "..." : message;
        for (User admin : admins) {
            notificationService.sendNotification(
                    admin,
                    "New Inquiry: " + fullName,
                    "From: " + fullName + " (" + (roleOrOrg != null && !roleOrOrg.isBlank() ? roleOrOrg : "General") + ") | Email: " + email + " - \"" + preview + "\"",
                    NotificationType.GENERAL
            );
        }

        return contactMessage;
    }

    @Transactional(readOnly = true)
    public Page<ContactMessage> getAllMessages(Pageable pageable) {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<ContactMessage> getAllMessagesList() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return contactMessageRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        contactMessageRepository.findById(id).ifPresent(msg -> {
            msg.setRead(true);
            contactMessageRepository.save(msg);
        });
    }

    @Transactional
    public void deleteMessage(Long id) {
        contactMessageRepository.deleteById(id);
    }
}
