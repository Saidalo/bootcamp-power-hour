package com.bootcamp.powerhour.scheduler;

import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.service.PriceImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
