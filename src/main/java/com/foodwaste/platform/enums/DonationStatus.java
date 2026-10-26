package com.foodwaste.platform.enums;

public enum DonationStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    PARTIALLY_RESERVED("Partially Reserved"),
    COLLECTED("Collected"),
    DELIVERED("Delivered"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");

    private final String displayName;

    DonationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
