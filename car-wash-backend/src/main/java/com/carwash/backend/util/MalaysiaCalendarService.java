package com.carwash.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Malaysian public holiday and operating-day calendar service.
 *
 * <p>Two categories of holidays are handled:
 * <ol>
 *   <li><b>Fixed holidays</b> — same month/day every year (e.g. National Day 31 Aug).
 *       Hardcoded as {@link MonthDay} constants.</li>
 *   <li><b>Dynamic holidays</b> — Islamic/lunar dates that shift each Gregorian year
 *       (Hari Raya Aidilfitri, Hari Raya Aidiladha, Awal Muharram, Maulidur Rasul).
 *       Injected via {@code app.malaysia.dynamic-holidays} in {@code application.yml}
 *       as a list of ISO-8601 date strings (yyyy-MM-dd).  Update this list each year.</li>
 * </ol>
 *
 * <p>Friday is treated as a non-operating day due to Friday prayer windows.
 *
 * <p>Usage example in {@code application.yml}:
 * <pre>
 * app:
 *   malaysia:
 *     dynamic-holidays:
 *       - "2026-03-30"  # Hari Raya Aidilfitri Day 1
 *       - "2026-03-31"  # Hari Raya Aidilfitri Day 2
 *       - "2026-06-06"  # Hari Raya Aidiladha
 *       - "2026-06-27"  # Awal Muharram
 *       - "2026-09-05"  # Maulidur Rasul
 * </pre>
 */
@Service
public class MalaysiaCalendarService {

    private static final Logger log = LoggerFactory.getLogger(MalaysiaCalendarService.class);

    // -------------------------------------------------------------------------
    // Fixed national public holidays (Federal-level, observed every year)
    // -------------------------------------------------------------------------

    /** National / Federal fixed holidays by month-day (independent of year). */
    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1,  1),   // New Year's Day
            MonthDay.of(5,  1),   // Labour Day
            MonthDay.of(8,  31),  // National Day (Hari Merdeka)
            MonthDay.of(9,  16),  // Malaysia Day
            MonthDay.of(12, 25)   // Christmas Day
            // Note: Chinese New Year is lunar — configure via dynamic-holidays each year
    );

    // -------------------------------------------------------------------------
    // Dynamic holidays (Islamic lunar / Chinese lunar — change yearly)
    // Injected from application.yml: app.malaysia.dynamic-holidays
    // -------------------------------------------------------------------------

    private final Set<LocalDate> dynamicHolidays;

    /**
     * Constructs the calendar service.
     *
     * @param rawDates list of ISO-8601 date strings from {@code app.malaysia.dynamic-holidays};
     *                 defaults to empty list if property is not configured
     */
    public MalaysiaCalendarService(
            @Value("${app.malaysia.dynamic-holidays:}") List<String> rawDates) {

        this.dynamicHolidays = rawDates.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> {
                    try {
                        return LocalDate.parse(s.trim());
                    } catch (Exception e) {
                        log.warn("MalaysiaCalendarService: could not parse dynamic holiday date '{}', skipping.", s);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        log.info("MalaysiaCalendarService: loaded {} fixed + {} dynamic holiday dates.",
                FIXED_HOLIDAYS.size(), dynamicHolidays.size());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given date falls on a Malaysian public holiday.
     * Does NOT check Friday prayer windows — use {@link #isOperatingDay(LocalDate)} for that.
     *
     * @param date the date to check
     * @return {@code true} if {@code date} is a public holiday
     */
    public boolean isPublicHoliday(LocalDate date) {
        return FIXED_HOLIDAYS.contains(MonthDay.from(date))
                || dynamicHolidays.contains(date);
    }

    /**
     * Returns {@code true} if the shop operates on the given date.
     *
     * <p>A day is <em>non-operating</em> when any of the following is true:
     * <ul>
     *   <li>It is a Friday (Friday prayer window — shop is closed all day)</li>
     *   <li>It is a Malaysian public holiday (fixed or dynamic)</li>
     * </ul>
     *
     * @param date the date to check
     * @return {@code true} if the shop is open on {@code date}
     */
    public boolean isOperatingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.FRIDAY && !isPublicHoliday(date);
    }
}
