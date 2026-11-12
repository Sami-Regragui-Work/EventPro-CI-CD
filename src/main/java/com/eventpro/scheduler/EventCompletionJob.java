package com.eventpro.scheduler;

import com.eventpro.domain.enums.EventStatus;
import com.eventpro.repository.EventRepository;
import com.eventpro.service.EventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventCompletionJob {
    private static final Logger log = LoggerFactory.getLogger(EventCompletionJob.class);

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000)
    public void completeOverEvents() {
        List<Long> eventIds = this.eventRepository
                .findIdsByStatusAndEndDateBefore(EventStatus.PUBLISHED, this.clock.instant());

        for (Long eventId : eventIds) {
            try {
                this.eventService.completeIfOver(eventId);
            } catch (RuntimeException failure) {
                log.error("Could not complete event {}", eventId, failure);
            }
        }
    }
}
