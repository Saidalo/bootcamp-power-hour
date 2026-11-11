package com.bootcamp.powerhour.scheduler;

import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.service.PriceImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
public class PriceImportScheduler {
    static final ZoneId BALTIC_ZONE = ZoneId.of("Europe/Tallinn");

    private final PriceImportService priceImportService;
    private final Clock clock;

    @Autowired
    public PriceImportScheduler (PriceImportService priceImportService){
        this(priceImportService, Clock.system(BALTIC_ZONE));
    }

    PriceImportScheduler(PriceImportService priceImportService, Clock clock){
        this.priceImportService = priceImportService;
        this.clock = clock;
    }

    public void importTomorrowPrices(){
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        if (priceImportService.hasPrices(tomorrow)) {
            log.debug("Prices for {} already imported, skipping", tomorrow);
            return;
        }
        log.info("Scheduled price import for {} started", tomorrow);
        PriceImport result = priceImportService.importAllZones(tomorrow);
        log.info("Scheduled price import for {} finished: {}, {} records", tomorrow, result.getStatus(), result.getRecordsImported());
    }
}
