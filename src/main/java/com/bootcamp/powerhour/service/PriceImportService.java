package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.exception.PriceProviderException;
import com.bootcamp.powerhour.exception.ZoneNotFoundException;
import com.bootcamp.powerhour.provider.PricePoint;
import com.bootcamp.powerhour.provider.PriceProvider;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import com.bootcamp.powerhour.repository.PriceImportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

//  Imports one day of prices for one zone and records the attempt in price_imports.
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceImportService {
    private final PriceProvider priceProvider;
    private final ElectricityPriceRepository priceRepository;
    private final PriceImportRepository importRepository;
    private final TransactionTemplate transactionalTemplate;

    public PriceImport importDay(BiddingZone zone, LocalDate date) {
        PriceImport record = new PriceImport();
        record.setStartedAt(OffsetDateTime.now());
        record.setBiddingZone(zone);
        record.setPriceDate(date);
        record.setStatus(PriceImportStatus.RUNNING);
        record = importRepository.save(record);
    public PriceImportService(PriceProvider priceProvider, PriceImportRepository importRepository, ElectricityPriceRepository priceRepository, PlatformTransactionManager transactionManager){
        this.priceProvider = priceProvider;
        this.importRepository = importRepository;
        this.priceRepository = priceRepository;
        this.transactionalTemplate = new TransactionTemplate(transactionManager);
    }

        try {
            List<PricePoint> points = priceProvider.fetchDayAhead(zone, date);
            priceRepository.deleteByBiddingZoneAndStartsAtGreaterThanEqualAndStartsAtLessThan(
                    zone, PowerHourTime.startOfDay(date), PowerHourTime.startOfDay(date.plusDays(1)));
            priceRepository.saveAll(points.stream().map(p -> toEntity(zone, p)).toList());
            record.setStatus(PriceImportStatus.SUCCESS);
            record.setRecordsImported(points.size());
            log.info("Price import for {} {}: {} rows", zone, date, points.size());
        } catch (RuntimeException e) {
            record.setStatus(PriceImportStatus.FAILED);
            record.setRecordsImported(0);
            record.setErrorMessage(e.getMessage());
            log.error("Price import for {} {} failed", zone, date, e);
        }
        record.setFinishedAt(OffsetDateTime.now());
        return importRepository.save(record);
    }
    private PriceImport runImport (List<BiddingZone> zones, LocalDate date){
        PriceImport priceImport = new PriceImport();
        priceImport.setStartedAt(OffsetDateTime.now());
        priceImport.setStatus(PriceImportStatus.RUNNING);
        priceImport.setRecordsImported(0);
        priceImport = importRepository.save(priceImport);
        int sum = 0;
        List <String> errors = new ArrayList<>();
        for (BiddingZone zone : zones){
            try {
                List<PricePoint> points = priceProvider.fetchDayAhead(zone, date);
                if (points.isEmpty()) {
                    throw new PriceProviderException("No prices published yet");
                }
                Integer saved = transactionalTemplate.execute(status -> savePrices(zone, points));
                sum += saved == null ? 0 : saved;
                log.info("Imported {} prices for {} {}", saved, zone, date);
            } catch (RuntimeException e){
                log.warn("Price import failed for {} {} : {}", zone, date, e.getMessage());
                errors.add(zone + ": " + e.getMessage());
            }
        }
        priceImport.setRecordsImported(sum);
        priceImport.setFinishedAt(OffsetDateTime.now());
        priceImport.setStatus(errors.isEmpty() ? PriceImportStatus.SUCCESS : PriceImportStatus.FAILED);
        priceImport.setErrorMessage(errors.isEmpty() ? null : String.join("; ", errors));
        return importRepository.save(priceImport);

    public List<PriceImport> history() {
        return importRepository.findTop50ByOrderByStartedAtDesc();
    }

    private static ElectricityPrice toEntity(BiddingZone zone, PricePoint point) {
        ElectricityPrice price = new ElectricityPrice();
        price.setBiddingZone(zone);
        price.setStartsAt(point.startsAt());
        price.setPriceEurKwh(point.priceEurKwh());
        return price;
    private int savePrices (BiddingZone zone, List<PricePoint> points){
        OffsetDateTime from = points.get(0).startsAt();
        OffsetDateTime to = points.get(points.size() - 1).startsAt();

        Map<Instant, ElectricityPrice> existing = priceRepository
                .findByBiddingZoneAndStartsAtBetween(zone, from, to)
                .stream()
                .collect(Collectors.toMap(p ->
                p.getStartsAt().toInstant(), Function.identity()));
        List<ElectricityPrice> toSave = new ArrayList<>();
        for (PricePoint point : points){
            ElectricityPrice price = existing.get(point.startsAt().toInstant());
            if (price == null){
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

    private static BiddingZone parseZone(String code){
        try {
            return BiddingZone.valueOf(code.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ZoneNotFoundException(code);
        }
    }

    public PriceImport importAllZones (LocalDate date){
        return runImport(List.of(BiddingZone.values()), date);
    }

    public PriceImport importZone (String zoneCode, LocalDate date){
        return runImport(List.of(parseZone(zoneCode)), date);
    }
}
