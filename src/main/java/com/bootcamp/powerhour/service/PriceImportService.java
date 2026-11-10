package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.exception.ZoneNotFoundException;
import com.bootcamp.powerhour.provider.PricePoint;
import com.bootcamp.powerhour.provider.PriceProvider;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import com.bootcamp.powerhour.repository.PriceImportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

// Imports one day of prices for one or all zones and records the attempt in price_imports.
@Slf4j
@Service
public class PriceImportService {
    private final PriceProvider priceProvider;
    private final ElectricityPriceRepository priceRepository;
    private final PriceImportRepository importRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReentrantLock importLock = new ReentrantLock();

    public PriceImportService(PriceProvider priceProvider,
                              ElectricityPriceRepository priceRepository,
                              PriceImportRepository importRepository,
                              PlatformTransactionManager transactionManager) {
        this.priceProvider = priceProvider;
        this.priceRepository = priceRepository;
        this.importRepository = importRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PriceImport importAllZones(LocalDate date) {
        return runImport(List.of(BiddingZone.values()), date);
    }

    public PriceImport importZone(String zoneCode, LocalDate date) {
        return runImport(List.of(parseZone(zoneCode)), date);
    }

    // Used by the admin page's Re-import button
    public PriceImport importDay(BiddingZone zone, LocalDate date) {
        return runImport(List.of(zone), date);
    }

    public List<PriceImport> history() {
        return importRepository.findTop50ByOrderByStartedAtDesc();
    }

    private PriceImport runImport(List<BiddingZone> zones, LocalDate date) {
        importLock.lock();
        try {
            PriceImport priceImport = new PriceImport();
            priceImport.setStartedAt(OffsetDateTime.now());
            priceImport.setStatus(PriceImportStatus.RUNNING);
            priceImport.setRecordsImported(0);
            priceImport.setPriceDate(date);
            // one zone -> show it in the admin table; all zones -> leave empty
            priceImport.setBiddingZone(zones.size() == 1 ? zones.get(0) : null);
            priceImport = importRepository.save(priceImport);

            int sum = 0;
            List<String> errors = new ArrayList<>();
            List<BiddingZone> notPublished = new ArrayList<>();
            for (BiddingZone zone : zones) {
                try {
                    List<PricePoint> points = priceProvider.fetchDayAhead(zone, date);
                    if (points.isEmpty()) {
                        log.info("Prices for {} {} not published yet", zone, date);
                        notPublished.add(zone);
                        continue;
                    }
                    Integer saved = saveWithRetry(zone, points, date);
                    sum += saved == null ? 0 : saved;
                    log.info("Imported {} prices for {} {}", saved, zone, date);
                } catch (RuntimeException e) {
                    log.warn("Price import failed for {} {}: {}", zone, date, e.getMessage());
                    errors.add(zone + ": " + e.getMessage());
                }
            }

            priceImport.setRecordsImported(sum);
            priceImport.setFinishedAt(OffsetDateTime.now());
            priceImport.setStatus(resolveStatus(errors, notPublished));
            priceImport.setErrorMessage(buildMessage(errors, notPublished));
            return importRepository.save(priceImport);
        } finally {
            importLock.unlock();
        }
    }

    private Integer saveWithRetry(BiddingZone zone, List<PricePoint> points, LocalDate date) {
        try {
            return transactionTemplate.execute(status -> savePrices(zone, points));
        } catch (DataIntegrityViolationException e) {
            log.warn("Race detected for {} {}, retrying as update", zone, date);
            return transactionTemplate.execute(status -> savePrices(zone, points));
        }
    }

    // Updates prices that already exist and inserts new ones (no duplicates thanks to the unique constraint)
    private int savePrices(BiddingZone zone, List<PricePoint> points) {
        OffsetDateTime from = points.get(0).startsAt();
        OffsetDateTime to = points.get(points.size() - 1).startsAt();

        Map<Instant, ElectricityPrice> existing = priceRepository
                .findByBiddingZoneAndStartsAtBetween(zone, from, to)
                .stream()
                .collect(Collectors.toMap(p -> p.getStartsAt().toInstant(), Function.identity()));

        List<ElectricityPrice> toSave = new ArrayList<>();
        for (PricePoint point : points) {
            ElectricityPrice price = existing.get(point.startsAt().toInstant());
            if (price == null) {
                price = new ElectricityPrice();
                price.setBiddingZone(zone);
                price.setStartsAt(point.startsAt());
            }
            price.setPriceEurKwh(point.priceEurKwh());
            toSave.add(price);
        }
        priceRepository.saveAll(toSave);
        return toSave.size();
    }

    private static BiddingZone parseZone(String code) {
        try {
            return BiddingZone.valueOf(code.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ZoneNotFoundException(code);
        }
    }

    public boolean hasPrices(LocalDate date) {
        ZoneId zone = ZoneId.of("Europe/Tallinn");
        OffsetDateTime from = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        for (BiddingZone biddingZone : BiddingZone.values()) {
            if (!priceRepository.existsInRange(biddingZone, from, to)) {
                return false;
            }
        }
        return true;
    }

    private static PriceImportStatus resolveStatus(List<String> errors, List<BiddingZone> notPublished){
        if (!errors.isEmpty()){
            return PriceImportStatus.FAILED;
        }
        if (!notPublished.isEmpty()){
            return PriceImportStatus.NOT_PUBLISHED;
        }
        return PriceImportStatus.SUCCESS;
    }

    private static String buildMessage(List<String> errors, List<BiddingZone> notPublished){
        List <String> parts = new ArrayList<>(errors);
        if (!notPublished.isEmpty()){
            parts.add("Not published yet: " + notPublished);
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }
}
