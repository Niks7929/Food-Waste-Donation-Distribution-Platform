package com.foodwaste.platform.dto;

import com.foodwaste.platform.enums.FoodCategory;
import com.foodwaste.platform.enums.FoodType;

import java.math.BigDecimal;

public class DonationFilterDto {

    private String search;
    private FoodCategory category;
    private FoodType foodType;
    private BigDecimal minQuantity;
    private String city;

    public DonationFilterDto() {}

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public FoodCategory getCategory() {
        return category;
    }

    public void setCategory(FoodCategory category) {
        this.category = category;
    }

    public FoodType getFoodType() {
        return foodType;
    }

    public void setFoodType(FoodType foodType) {
        this.foodType = foodType;
    }

    public BigDecimal getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(BigDecimal minQuantity) {
        this.minQuantity = minQuantity;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
