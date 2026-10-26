package com.foodwaste.platform.dto;

import com.foodwaste.platform.enums.FoodCategory;
import com.foodwaste.platform.enums.FoodType;
import com.foodwaste.platform.enums.QuantityUnit;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DonationCreateDto {

    private Long id;

    @NotBlank(message = "Food name or title is required")
    @Size(min = 3, max = 150, message = "Food name must be between 3 and 150 characters")
    private String foodName;

    private String description;

    @NotNull(message = "Food category is required")
    private FoodCategory category;

    @NotNull(message = "Food type (Veg / Non-Veg) is required")
    private FoodType foodType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.1", inclusive = true, message = "Quantity must be greater than zero")
    private BigDecimal totalQuantity;

    @NotNull(message = "Quantity unit is required")
    private QuantityUnit quantityUnit;

    @NotNull(message = "Preparation date and time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime preparedAt;

    @NotNull(message = "Pickup deadline is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime pickupDeadline;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String storageInstructions;
    private String allergenInformation;

    private MultipartFile imageFile;
    private String existingImagePath;

    public DonationCreateDto() {}

    public boolean isPickupDeadlineValid() {
        if (pickupDeadline == null) return false;
        return pickupDeadline.isAfter(LocalDateTime.now());
    }

    public boolean isPreparationBeforeDeadline() {
        if (preparedAt == null || pickupDeadline == null) return true;
        return preparedAt.isBefore(pickupDeadline);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public QuantityUnit getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(QuantityUnit quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public LocalDateTime getPreparedAt() {
        return preparedAt;
    }

    public void setPreparedAt(LocalDateTime preparedAt) {
        this.preparedAt = preparedAt;
    }

    public LocalDateTime getPickupDeadline() {
        return pickupDeadline;
    }

    public void setPickupDeadline(LocalDateTime pickupDeadline) {
        this.pickupDeadline = pickupDeadline;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getStorageInstructions() {
        return storageInstructions;
    }

    public void setStorageInstructions(String storageInstructions) {
        this.storageInstructions = storageInstructions;
    }

    public String getAllergenInformation() {
        return allergenInformation;
    }

    public void setAllergenInformation(String allergenInformation) {
        this.allergenInformation = allergenInformation;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public String getExistingImagePath() {
        return existingImagePath;
    }

    public void setExistingImagePath(String existingImagePath) {
        this.existingImagePath = existingImagePath;
    }
}
