package com.foodwaste.platform.enums;

public enum DeliveryStatus {
    ASSIGNED("Assigned"),
    PICKED_UP("Picked Up"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    FAILED("Failed");

    private final String displayName;

    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
