package com.foodwaste.platform.enums;

public enum PickupRequestStatus {
    PENDING("Pending Donor Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    ASSIGNED("Volunteer Assigned"),
    PICKED_UP("Picked Up"),
    IN_TRANSIT("In Transit"),
    COMPLETED("Completed");

    private final String displayName;

    PickupRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
