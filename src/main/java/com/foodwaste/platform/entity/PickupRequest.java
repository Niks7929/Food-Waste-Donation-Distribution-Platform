package com.foodwaste.platform.entity;

import com.foodwaste.platform.enums.PickupRequestStatus;
import com.foodwaste.platform.enums.QuantityUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pickup_requests")
public class PickupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false)
    private FoodDonation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngo_organization_id", nullable = false)
    private Organization ngoOrganization;

    @Column(name = "requested_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false, length = 30)
    private QuantityUnit quantityUnit;

    @Column(name = "requested_pickup_time", nullable = false)
    private LocalDateTime requestedPickupTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PickupRequestStatus status = PickupRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToOne(mappedBy = "pickupRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Delivery delivery;

    public PickupRequest() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FoodDonation getDonation() {
        return donation;
    }

    public void setDonation(FoodDonation donation) {
        this.donation = donation;
    }

    public Organization getNgoOrganization() {
        return ngoOrganization;
    }

    public void setNgoOrganization(Organization ngoOrganization) {
        this.ngoOrganization = ngoOrganization;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(BigDecimal requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public QuantityUnit getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(QuantityUnit quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public LocalDateTime getRequestedPickupTime() {
        return requestedPickupTime;
    }

    public void setRequestedPickupTime(LocalDateTime requestedPickupTime) {
        this.requestedPickupTime = requestedPickupTime;
    }

    public PickupRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PickupRequestStatus status) {
        this.status = status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }
}
