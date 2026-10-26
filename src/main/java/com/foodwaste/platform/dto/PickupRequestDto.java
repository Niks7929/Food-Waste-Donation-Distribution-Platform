package com.foodwaste.platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PickupRequestDto {

    @NotNull(message = "Donation ID is required")
    private Long donationId;

    @NotNull(message = "Requested quantity is required")
    @DecimalMin(value = "0.1", inclusive = true, message = "Requested quantity must be greater than zero")
    private BigDecimal requestedQuantity;

    @NotNull(message = "Estimated pickup time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime requestedPickupTime;

    private String notes;

    public PickupRequestDto() {}

    public Long getDonationId() {
        return donationId;
    }

    public void setDonationId(Long donationId) {
        this.donationId = donationId;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public LocalDateTime getRequestedPickupTime() {
        return requestedPickupTime;
    }

    public void setRequestedPickupTime(LocalDateTime requestedPickupTime) {
        this.requestedPickupTime = requestedPickupTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
