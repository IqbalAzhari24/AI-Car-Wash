package com.carwash.backend.util;

import java.time.LocalTime;

/** Shared slot-inventory constants used by CatalogSeeder and SlotReplenishmentCron. */
public final class SlotConstants {

    public static final LocalTime OPEN      = LocalTime.of(9, 0);
    public static final LocalTime LAST_SLOT = LocalTime.of(17, 30);
    public static final int SLOT_MINUTES    = 30;
    public static final int DAYS_AHEAD      = 14;
    public static final int MAX_PER_SLOT    = 3;

    private SlotConstants() {}
}
