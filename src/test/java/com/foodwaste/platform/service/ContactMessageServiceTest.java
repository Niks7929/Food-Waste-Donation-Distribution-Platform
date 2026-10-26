package com.foodwaste.platform.service;

import com.foodwaste.platform.entity.ContactMessage;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.enums.NotificationType;
import com.foodwaste.platform.enums.Role;
import com.foodwaste.platform.repository.ContactMessageRepository;
import com.foodwaste.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactMessageService contactMessageService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@foodwaste.org");
        adminUser.setRole(Role.ADMIN);
    }

    @Test
    void testSaveMessage_SavesAndNotifiesAdmin() {
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminUser));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenAnswer(i -> {
            ContactMessage msg = i.getArgument(0);
            msg.setId(10L);
            return msg;
        });

        ContactMessage saved = contactMessageService.saveMessage("Rahul Patil", "rahul@example.com", "Restaurant Owner", "Need help registering.");

        assertNotNull(saved);
        assertEquals("Rahul Patil", saved.getFullName());
        assertFalse(saved.isRead());
        verify(contactMessageRepository, times(1)).save(any(ContactMessage.class));
        verify(notificationService, times(1)).sendNotification(eq(adminUser), anyString(), anyString(), eq(NotificationType.GENERAL));
    }

    @Test
    void testMarkAsRead() {
        ContactMessage msg = new ContactMessage("Rahul", "rahul@test.com", "Donor", "Hello");
        msg.setId(5L);
        when(contactMessageRepository.findById(5L)).thenReturn(Optional.of(msg));

        contactMessageService.markAsRead(5L);

        assertTrue(msg.isRead());
        verify(contactMessageRepository, times(1)).save(msg);
    }

    @Test
    void testGetUnreadCount() {
        when(contactMessageRepository.countByIsReadFalse()).thenReturn(3L);

        long count = contactMessageService.getUnreadCount();

        assertEquals(3L, count);
        verify(contactMessageRepository, times(1)).countByIsReadFalse();
    }
}
