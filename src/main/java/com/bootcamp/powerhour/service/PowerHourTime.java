package com.bootcamp.powerhour.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Lithuania, Latvia and Estonia share one time zone, so the app uses it everywhere. */
public final class PowerHourTime {
    public static final ZoneId ZONE = ZoneId.of("Europe/Riga");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);

    private PowerHourTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
    }

    public static String time(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(ZONE).format(TIME);
    }

    public static String dayTime(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(ZONE).format(DAY_TIME);
    }

    public static String durationLabel(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) {
            return m + "min";
        }
        return m == 0 ? h + "h" : h + "h " + m + "min";
    }

    public static LocalDate localDate(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(ZONE).toLocalDate();
    }
}