package com.foodwaste.platform.enums;

public enum FoodCategory {
    COOKED_MEALS("Cooked Meals"),
    RAW_INGREDIENTS("Raw Ingredients"),
    PACKAGED_FOOD("Packaged Food"),
    BAKERY("Bakery Items"),
    FRUITS_VEGETABLES("Fruits & Vegetables"),
    BEVERAGES("Beverages"),
    DAIRY("Dairy Products"),
    OTHER("Other");

    private final String displayName;

    FoodCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
