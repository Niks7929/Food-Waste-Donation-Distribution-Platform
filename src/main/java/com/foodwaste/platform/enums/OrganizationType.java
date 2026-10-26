package com.foodwaste.platform.enums;

public enum OrganizationType {
    RESTAURANT("Restaurant"),
    HOTEL("Hotel"),
    MESS("Mess / Canteen"),
    HOSTEL("Hostel"),
    MARRIAGE_HALL("Marriage / Banquet Hall"),
    FOOD_SHOP("Food Shop / Grocery"),
    BAKERY("Bakery"),
    NGO("Non-Governmental Organization"),
    COMMUNITY_KITCHEN("Community Kitchen"),
    OTHER("Other");

    private final String displayName;

    OrganizationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
