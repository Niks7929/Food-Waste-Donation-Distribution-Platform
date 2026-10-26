package com.foodwaste.platform.scheduler;

import com.foodwaste.platform.service.FoodDonationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DonationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DonationExpiryScheduler.class);
    private final FoodDonationService foodDonationService;

    public DonationExpiryScheduler(FoodDonationService foodDonationService) {
        this.foodDonationService = foodDonationService;
    }

    /**
     * Runs every 2 minutes to check for past pickup deadlines
     * Safe to run repeatedly.
     */
    @Scheduled(fixedRate = 120000, initialDelay = 30000)
    public void processExpiredDonations() {
        try {
            int expired = foodDonationService.expirePastDonations();
            if (expired > 0) {
                log.info("DonationExpiryScheduler: Expired {} donations that exceeded pickup deadline.", expired);
            }
        } catch (Exception e) {
            log.error("Error executing donation expiry scheduled task: ", e);
        }
    }
}
