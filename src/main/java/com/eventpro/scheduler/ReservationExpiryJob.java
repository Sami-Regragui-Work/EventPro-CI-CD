package com.eventpro.scheduler;

import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.repository.ReservationRepository;
import com.eventpro.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationRepository repository;
    private final ReservationService service;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000)
    public void expireUnpaidReservations() {
        List<Reservation> expirableReservations = this.repository.findByStatusAndExpirationDateLessThanEqual(ReservationStatus.PENDING_PAYMENT, this.clock.instant());

        for (Reservation expirableReservation : expirableReservations) {
            try {
                this.service.expire(expirableReservation.getId());
            } catch (RuntimeException failure) {
                log.error("Could not expire reservation {}", expirableReservation.getId(), failure);
            }
        }
    }
}
