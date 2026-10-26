package com.foodwaste.platform.enums;

public enum QuantityUnit {
    PACKETS("Packets"),
    MEALS("Meals / Servings"),
    KG("Kilograms (kg)"),
    LITERS("Liters (L)"),
    BOXES("Boxes");

    private final String displayName;

    QuantityUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
