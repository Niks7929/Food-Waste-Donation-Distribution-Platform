package com.foodwaste.platform.dto;

import com.foodwaste.platform.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public class DeliveryUpdateDto {

    @NotNull(message = "Delivery ID is required")
    private Long deliveryId;

    @NotNull(message = "Delivery status is required")
    private DeliveryStatus status;

    private String notes;
    private String confirmationReference;

    public DeliveryUpdateDto() {}

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getConfirmationReference() {
        return confirmationReference;
    }

    public void setConfirmationReference(String confirmationReference) {
        this.confirmationReference = confirmationReference;
    }
}
