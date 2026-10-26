package com.foodwaste.platform.service;

import com.foodwaste.platform.entity.ActivityLog;
import com.foodwaste.platform.entity.User;
import com.foodwaste.platform.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void logActivity(User user, String action, String entityType, Long entityId, String details) {
        ActivityLog log = new ActivityLog(user, action, entityType, entityId, details);
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getRecentLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> getUserLogs(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
