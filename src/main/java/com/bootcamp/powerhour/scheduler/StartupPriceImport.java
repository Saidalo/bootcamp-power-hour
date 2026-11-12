package com.bootcamp.powerhour.scheduler;

import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.service.PriceImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * When the app starts, imports real Elering prices for every day from (today - backfill-days)
 * to tomorrow that doesn't have a full set of prices yet. Turn it off with
 * elering.import-on-startup=false.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "elering.import-on-startup", havingValue = "true")
public class StartupPriceImport {

    private final PriceImportService priceImportService;
    private final int backfillDays;
    private final Clock clock;

    @Autowired
    public StartupPriceImport(PriceImportService priceImportService,
                              @Value("${elering.backfill-days:7}") int backfillDays) {
        this(priceImportService, backfillDays, Clock.system(PriceImportScheduler.BALTIC_ZONE));
    }

    StartupPriceImport(PriceImportService priceImportService, int backfillDays, Clock clock) {
        this.priceImportService = priceImportService;
        this.backfillDays = backfillDays;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void importMissingDays() {
        LocalDate today = LocalDate.now(clock);
        int imported = 0;
        for (LocalDate date = today.minusDays(backfillDays); !date.isAfter(today.plusDays(1)); date = date.plusDays(1)) {
            if (priceImportService.hasPrices(date)) {
                continue;
            }
            PriceImport result = priceImportService.importAllZones(date);
            log.info("Startup import for {}: {}, {} records", date, result.getStatus(), result.getRecordsImported());
            if (result.getStatus() == PriceImportStatus.FAILED) {
                log.warn("Elering is not reachable, stopping the startup import");
                return;
            }
            imported++;
        }
        log.info("Startup import finished: {} days imported", imported);
    }
}