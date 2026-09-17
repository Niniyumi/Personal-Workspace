package com.niniyumi.personalagent.course.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class CoursePlaybackService {
    private final CourseImportService imports;
    private final Clock clock;
    private final Map<String, PlaybackTicket> tickets = new ConcurrentHashMap<>();

    public CoursePlaybackService(CourseImportService imports, Clock clock) {
        this.imports = imports;
        this.clock = clock;
    }

    public String issue(long userId, long courseId) {
        imports.original(userId, courseId);
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new PlaybackTicket(userId, courseId, clock.instant().plus(180, ChronoUnit.MINUTES)));
        return ticket;
    }

    public Resource open(String ticket) {
        PlaybackTicket playback = tickets.get(ticket);
        if (playback == null || !playback.expiresAt().isAfter(clock.instant())) {
            tickets.remove(ticket);
            throw new CourseNotFoundException();
        }
        return imports.original(playback.userId(), playback.courseId());
    }

    private record PlaybackTicket(long userId, long courseId, Instant expiresAt) { }
}
