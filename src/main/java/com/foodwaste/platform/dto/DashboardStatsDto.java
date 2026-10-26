package com.foodwaste.platform.dto;

import java.math.BigDecimal;

public class DashboardStatsDto {

    private long totalUsers;
    private long totalDonors;
    private long totalNgos;
    private long totalVolunteers;
    private long pendingApprovals;

    private long totalDonations;
    private long availableDonations;
    private long pendingRequests;
    private long activeDeliveries;
    private long completedDeliveries;

    private BigDecimal totalQuantityDonated = BigDecimal.ZERO;
    private BigDecimal totalQuantityAvailable = BigDecimal.ZERO;
    private BigDecimal totalQuantityDelivered = BigDecimal.ZERO;

    public DashboardStatsDto() {}

    // Getters and Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalDonors() {
        return totalDonors;
    }

    public void setTotalDonors(long totalDonors) {
        this.totalDonors = totalDonors;
    }

    public long getTotalNgos() {
        return totalNgos;
    }

    public void setTotalNgos(long totalNgos) {
        this.totalNgos = totalNgos;
    }

    public long getTotalVolunteers() {
        return totalVolunteers;
    }

    public void setTotalVolunteers(long totalVolunteers) {
        this.totalVolunteers = totalVolunteers;
    }

    public long getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(long pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }

    public long getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(long totalDonations) {
        this.totalDonations = totalDonations;
    }

    public long getAvailableDonations() {
        return availableDonations;
    }

    public void setAvailableDonations(long availableDonations) {
        this.availableDonations = availableDonations;
    }

    public long getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(long pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public long getActiveDeliveries() {
        return activeDeliveries;
    }

    public void setActiveDeliveries(long activeDeliveries) {
        this.activeDeliveries = activeDeliveries;
    }

    public long getCompletedDeliveries() {
        return completedDeliveries;
    }

    public void setCompletedDeliveries(long completedDeliveries) {
        this.completedDeliveries = completedDeliveries;
    }

    public BigDecimal getTotalQuantityDonated() {
        return totalQuantityDonated;
    }

    public void setTotalQuantityDonated(BigDecimal totalQuantityDonated) {
        this.totalQuantityDonated = totalQuantityDonated;
    }

    public BigDecimal getTotalQuantityAvailable() {
        return totalQuantityAvailable;
    }

    public void setTotalQuantityAvailable(BigDecimal totalQuantityAvailable) {
        this.totalQuantityAvailable = totalQuantityAvailable;
    }

    public BigDecimal getTotalQuantityDelivered() {
        return totalQuantityDelivered;
    }

    public void setTotalQuantityDelivered(BigDecimal totalQuantityDelivered) {
        this.totalQuantityDelivered = totalQuantityDelivered;
    }
}
