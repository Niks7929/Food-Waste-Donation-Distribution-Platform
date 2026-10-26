package com.foodwaste.platform.enums;

public enum NotificationType {
    REQUEST_RECEIVED("New Request Received"),
    REQUEST_APPROVED("Request Approved"),
    REQUEST_REJECTED("Request Rejected"),
    REQUEST_CANCELLED("Request Cancelled"),
    VOLUNTEER_ASSIGNED("Volunteer Assigned"),
    PICKED_UP("Food Picked Up"),
    IN_TRANSIT("Food In Transit"),
    DELIVERED("Delivery Completed"),
    DONATION_EXPIRING("Donation Expiring Soon"),
    ACCOUNT_STATUS_CHANGE("Account Status Updated"),
    GENERAL("Notification");

    private final String defaultTitle;

    NotificationType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
