package com.carwash.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Malaysia public holiday lookup, sourced from Google's officially-maintained
 * "Holidays in Malaysia" calendar feed (no API key required). Nager.Date and
 * similar public-holiday APIs do not cover Malaysia, since several holidays
 * follow the Islamic lunar calendar and are announced/adjusted per year.
 * Results are cached in-memory and refreshed at most once per day.
 */
@Service
public class MalaysiaCalendarService {

    private static final Logger log = LoggerFactory.getLogger(MalaysiaCalendarService.class);

    private static final String ICS_URL =
            "https://calendar.google.com/calendar/ical/en.malaysia%23holiday%40group.v.calendar.google.com/public/basic.ics";

    // The feed is titled "Holidays and Observances in Malaysia" — it mixes true gazetted
    // public holidays (DESCRIPTION starts "Public holiday...") with cultural observances
    // like Valentine's Day or Earth Day (DESCRIPTION starts "Observance..."). Capture both
    // DESCRIPTION and SUMMARY (in that fixed order within a VEVENT) so only real holidays
    // are cached — see PUBLIC_HOLIDAY_PREFIX check in parseAndCache().
    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "BEGIN:VEVENT.*?DTSTART;VALUE=DATE:(\\d{8}).*?DESCRIPTION:(.*?)\\r?\\n(?:.*?\\r?\\n)*?SUMMARY:(.*?)\\r?\\n",
            Pattern.DOTALL);
    private static final String PUBLIC_HOLIDAY_PREFIX = "Public holiday";

    // Tool/analytics calls run synchronously on whatever thread invokes them (Reactor Netty
    // callback for Timah, servlet thread for the REST controller) — a plain JDK HttpClient
    // call is safe there; do NOT swap this for a reactive WebClient#block().
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Map<LocalDate, String> holidaysByDate = new ConcurrentHashMap<>();
    private volatile Instant lastFetched = Instant.EPOCH;

    /** Holiday name for the given date, or empty if it's not a public holiday. */
    public Optional<String> getHolidayName(LocalDate date) {
        ensureFresh();
        return Optional.ofNullable(holidaysByDate.get(date));
    }

    public boolean isPublicHoliday(LocalDate date) {
        return getHolidayName(date).isPresent();
    }

    /** Public holidays falling within [from, from + days). */
    public List<HolidayEntry> getUpcoming(LocalDate from, int days) {
        ensureFresh();
        List<HolidayEntry> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            String name = holidaysByDate.get(d);
            if (name != null) result.add(new HolidayEntry(d, name));
        }
        return result;
    }

    private synchronized void ensureFresh() {
        boolean stale = Instant.now().isAfter(lastFetched.plus(Duration.ofHours(24)));
        if (!holidaysByDate.isEmpty() && !stale) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(ICS_URL))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                parseAndCache(response.body());
                lastFetched = Instant.now();
            } else {
                log.warn("Malaysia calendar fetch returned HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Malaysia calendar fetch failed, keeping existing cache: {}", e.getMessage());
        }
    }

    private void parseAndCache(String ics) {
        Map<LocalDate, String> parsed = new HashMap<>();
        Matcher m = EVENT_PATTERN.matcher(ics);
        while (m.find()) {
            try {
                String description = m.group(2).trim();
                if (!description.startsWith(PUBLIC_HOLIDAY_PREFIX)) {
                    continue; // observance (Valentine's Day, Earth Day, etc.), not a gazetted holiday
                }
                LocalDate date = LocalDate.parse(m.group(1), DateTimeFormatter.BASIC_ISO_DATE);
                String name = m.group(3).trim();
                parsed.putIfAbsent(date, name); // first entry wins where multiple states share a date
            } catch (Exception ignored) {
                // malformed VEVENT block — skip
            }
        }
        if (!parsed.isEmpty()) {
            holidaysByDate.clear();
            holidaysByDate.putAll(parsed);
        }
    }

    public record HolidayEntry(LocalDate date, String name) { }
}
