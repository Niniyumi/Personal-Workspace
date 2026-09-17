package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class CoursePlaybackServiceTest {
    @Test
    void issuesAnUnpredictableTicketThatReadsOnlyTheAuthorizedOriginal() throws Exception {
        CourseImportService imports = mock(CourseImportService.class);
        ByteArrayResource original = new ByteArrayResource("audio".getBytes());
        when(imports.original(42, 9)).thenReturn(original);
        CoursePlaybackService service = new CoursePlaybackService(imports,
                Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC));

        String ticket = service.issue(42, 9);

        assertThat(ticket).hasSizeGreaterThan(30);
        assertThat(service.open(ticket)).isSameAs(original);
        assertThatThrownBy(() -> service.open("unknown-ticket"))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void keepsPlaybackAvailableForAFullLengthRecording() throws Exception {
        CourseImportService imports = mock(CourseImportService.class);
        ByteArrayResource original = new ByteArrayResource("audio".getBytes());
        when(imports.original(42, 9)).thenReturn(original);
        Clock clock = mock(Clock.class);
        Instant issuedAt = Instant.parse("2026-09-13T12:00:00Z");
        when(clock.instant()).thenReturn(issuedAt, issuedAt.plus(151, java.time.temporal.ChronoUnit.MINUTES));
        CoursePlaybackService service = new CoursePlaybackService(imports, clock);

        String ticket = service.issue(42, 9);

        assertThat(service.open(ticket)).isSameAs(original);
    }
}
