package com.foodwaste.platform.enums;

public enum FoodType {
    VEGETARIAN("Vegetarian"),
    NON_VEGETARIAN("Non-Vegetarian"),
    VEGAN("Vegan"),
    EGGETARIAN("Eggetarian");

    private final String displayName;

    FoodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
