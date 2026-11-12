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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartupPriceImportTest {

    // 2026-11-12 14:00 in Riga
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), PriceImportScheduler.BALTIC_ZONE);

    @Mock
    private PriceImportService priceImportService;

    @Test
    void importsEveryMissingDayFromBackfillStartToTomorrow() {
        when(priceImportService.importAllZones(any())).thenReturn(withStatus(PriceImportStatus.SUCCESS));

        new StartupPriceImport(priceImportService, 2, CLOCK).importMissingDays();

        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 10));
        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 11));
        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 12));
        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 13));
        verify(priceImportService, times(4)).importAllZones(any());
    }

    @Test
    void skipsDaysThatAlreadyHaveAllPrices() {
        when(priceImportService.hasPrices(LocalDate.of(2026, 11, 12))).thenReturn(true);
        when(priceImportService.importAllZones(any())).thenReturn(withStatus(PriceImportStatus.SUCCESS));

        new StartupPriceImport(priceImportService, 0, CLOCK).importMissingDays();

        verify(priceImportService, never()).importAllZones(LocalDate.of(2026, 11, 12));
        verify(priceImportService).importAllZones(LocalDate.of(2026, 11, 13));
    }

    @Test
    void stopsWhenEleringIsDown() {
        when(priceImportService.importAllZones(any())).thenReturn(withStatus(PriceImportStatus.FAILED));

        new StartupPriceImport(priceImportService, 7, CLOCK).importMissingDays();

        verify(priceImportService, times(1)).importAllZones(any());
    }

    private static PriceImport withStatus(PriceImportStatus status) {
        PriceImport result = new PriceImport();
        result.setStatus(status);
        result.setRecordsImported(0);
        return result;
    }
}