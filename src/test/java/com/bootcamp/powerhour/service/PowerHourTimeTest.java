package com.bootcamp.powerhour.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the conversions between UTC timestamps and the local day in Riga.
 *
 * @author Karyna Litvinova
 */
class PowerHourTimeTest {

    @Test
    void startOfDayIsMidnightInRigaWrittenInUtc() {
        OffsetDateTime start = PowerHourTime.startOfDay(LocalDate.of(2026, 11, 5));

        assertThat(start.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(start).isEqualTo(OffsetDateTime.of(2026, 11, 4, 22, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void startOfDayInSummerIsThreeHoursBeforeUtcMidnight() {
        OffsetDateTime start = PowerHourTime.startOfDay(LocalDate.of(2026, 7, 1));

        assertThat(start).isEqualTo(OffsetDateTime.of(2026, 6, 30, 21, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void theDayClocksGoForwardHas23Hours() {
        LocalDate day = LocalDate.of(2026, 3, 29);

        Duration length = Duration.between(PowerHourTime.startOfDay(day), PowerHourTime.startOfDay(day.plusDays(1)));

        assertThat(length).isEqualTo(Duration.ofHours(23));
    }

    @Test
    void theDayClocksGoBackHas25Hours() {
        LocalDate day = LocalDate.of(2026, 10, 25);

        Duration length = Duration.between(PowerHourTime.startOfDay(day), PowerHourTime.startOfDay(day.plusDays(1)));

        assertThat(length).isEqualTo(Duration.ofHours(25));
    }

    @Test
    void timeShowsTheClockInRiga() {
        OffsetDateTime utc = OffsetDateTime.of(2026, 11, 4, 23, 30, 0, 0, ZoneOffset.UTC);

        assertThat(PowerHourTime.time(utc)).isEqualTo("01:30");
    }

    @Test
    void timeIsTheSameWhateverOffsetTheInputHas() {
        OffsetDateTime utc = OffsetDateTime.of(2026, 11, 4, 23, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime tokyo = utc.withOffsetSameInstant(ZoneOffset.ofHours(9));

        assertThat(PowerHourTime.time(tokyo)).isEqualTo(PowerHourTime.time(utc));
    }

    @Test
    void dayTimeShowsWeekdayDateAndClockInEnglish() {
        OffsetDateTime utc = OffsetDateTime.of(2026, 11, 4, 23, 30, 0, 0, ZoneOffset.UTC);

        assertThat(PowerHourTime.dayTime(utc)).isEqualTo("Thu 5 Nov, 01:30");
    }

    @Test
    void localDateIsTheDateInRiga() {
        OffsetDateTime utc = OffsetDateTime.of(2026, 11, 4, 23, 30, 0, 0, ZoneOffset.UTC);

        assertThat(PowerHourTime.localDate(utc)).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void localDateOfStartOfDayIsThatDay() {
        LocalDate day = LocalDate.of(2026, 11, 5);

        assertThat(PowerHourTime.localDate(PowerHourTime.startOfDay(day))).isEqualTo(day);
    }

    @Test
    void todayIsTheDateInRiga() {
        assertThat(PowerHourTime.today()).isEqualTo(LocalDate.now(ZoneId.of("Europe/Riga")));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0min",
            "45, 45min",
            "60, 1h",
            "90, 1h 30min",
            "120, 2h",
            "135, 2h 15min",
            "1440, 24h"
    })
    void durationLabelIsShortAndReadable(int minutes, String expected) {
        assertThat(PowerHourTime.durationLabel(minutes)).isEqualTo(expected);
    }
}