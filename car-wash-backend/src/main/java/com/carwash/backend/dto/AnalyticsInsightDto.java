package com.carwash.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Descriptive-analysis blurb for the Owner analytics dashboard.
 * Returned by GET /api/v1/owner/analytics/insight.
 */
public class AnalyticsInsightDto {

    private LocalDate date;
    private String text;
    private boolean publicHoliday;
    private String holidayName;
    private List<UpcomingHoliday> upcomingHolidays;

    public AnalyticsInsightDto(LocalDate date, String text, boolean publicHoliday,
                                String holidayName, List<UpcomingHoliday> upcomingHolidays) {
        this.date             = date;
        this.text             = text;
        this.publicHoliday    = publicHoliday;
        this.holidayName      = holidayName;
        this.upcomingHolidays = upcomingHolidays;
    }

    public LocalDate getDate()                          { return date; }
    public String getText()                              { return text; }
    public boolean isPublicHoliday()                      { return publicHoliday; }
    public String getHolidayName()                        { return holidayName; }
    public List<UpcomingHoliday> getUpcomingHolidays()    { return upcomingHolidays; }

    public static class UpcomingHoliday {
        private LocalDate date;
        private String name;

        public UpcomingHoliday(LocalDate date, String name) {
            this.date = date;
            this.name = name;
        }

        public LocalDate getDate() { return date; }
        public String getName()    { return name; }
    }
}
