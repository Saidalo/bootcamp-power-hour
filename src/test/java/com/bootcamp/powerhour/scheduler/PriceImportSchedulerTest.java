package com.bootcamp.powerhour.scheduler;

import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.service.PriceImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PriceImportSchedulerTest {
    @Mock
    private PriceImportService priceImportService;

    @Test
    void importTomorrowPrices_shouldImportAllZonesForTomorrow() {

        Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), PriceImportScheduler.BALTIC_ZONE);
        when(priceImportService.importAllZones(any())).thenReturn(successfulImport());

        new PriceImportScheduler(priceImportService, clock).importTomorrowPrices();

        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 6));
    }

    @Test
    void importTomorrowPrices_shouldUseBalticDateNotUtc() {

        Clock clock = Clock.fixed(Instant.parse("2026-11-05T23:30:00Z"), PriceImportScheduler.BALTIC_ZONE);
        when(priceImportService.importAllZones(any())).thenReturn(successfulImport());
        new PriceImportScheduler(priceImportService, clock).importTomorrowPrices();

        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 7));
    }

    private static PriceImport successfulImport(){
        PriceImport result = new PriceImport();
        result.setStatus(PriceImportStatus.SUCCESS);
        result.setRecordsImported(288);
        return result;
    }

    @Test
    void importTomorrowPrices_shouldSkipWhenTomorrowIsAlreadyImported() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), PriceImportScheduler.BALTIC_ZONE);
        when(priceImportService.hasPrices(LocalDate.of(2026, 11, 6))).thenReturn(true);

        new PriceImportScheduler(priceImportService, clock).importTomorrowPrices();

        verify(priceImportService, never()).importAllZones(any());
    }

    @Test
    void importTomorrowPrices_shouldRunOnConfiguredCronInBalticTime() throws NoSuchMethodException {
        Scheduled scheduled = PriceImportScheduler.class
                .getMethod("importTomorrowPrices")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).as("without @Scheduled the daily import never runs").isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${elering.import-cron}");
        assertThat(scheduled.zone()).isEqualTo("Europe/Tallinn");
    }

    @Test
    void importCron_shouldRetryEvery15MinutesUntilPricesArePublished() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        CronExpression cron = CronExpression.parse(properties.getProperty("elering.import-cron"));

        assertThat(cron.next(LocalDateTime.of(2026, 11, 5, 13, 59))).isEqualTo(LocalDateTime.of(2026, 11, 5, 14, 0));
        assertThat(cron.next(LocalDateTime.of(2026, 11, 5, 14, 0))).isEqualTo(LocalDateTime.of(2026, 11, 5, 14, 15));
        assertThat(cron.next(LocalDateTime.of(2026, 11, 5, 16, 45))).isEqualTo(LocalDateTime.of(2026, 11, 6, 14, 0));
    }
}
