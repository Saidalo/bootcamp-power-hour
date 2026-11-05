package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.provider.PricePoint;
import com.bootcamp.powerhour.provider.PriceProvider;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import com.bootcamp.powerhour.repository.PriceImportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

//  Imports one day of prices for one zone and records the attempt in price_imports.
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceImportService {
    private final PriceProvider priceProvider;
    private final ElectricityPriceRepository priceRepository;
    private final PriceImportRepository importRepository;

    public PriceImport importDay(BiddingZone zone, LocalDate date) {
        PriceImport record = new PriceImport();
        record.setStartedAt(OffsetDateTime.now());
        record.setBiddingZone(zone);
        record.setPriceDate(date);
        record.setStatus(PriceImportStatus.RUNNING);
        record = importRepository.save(record);

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

    public List<PriceImport> history() {
        return importRepository.findTop50ByOrderByStartedAtDesc();
    }

    private static ElectricityPrice toEntity(BiddingZone zone, PricePoint point) {
        ElectricityPrice price = new ElectricityPrice();
        price.setBiddingZone(zone);
        price.setStartsAt(point.startsAt());
        price.setPriceEurKwh(point.priceEurKwh());
        return price;
    }
}