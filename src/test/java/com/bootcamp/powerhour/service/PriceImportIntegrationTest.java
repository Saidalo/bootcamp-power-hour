package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.ElectricityPrice;
import com.bootcamp.powerhour.entity.PriceImport;
import com.bootcamp.powerhour.entity.PriceImportStatus;
import com.bootcamp.powerhour.exception.PriceProviderException;
import com.bootcamp.powerhour.provider.PricePoint;
import com.bootcamp.powerhour.provider.PriceProvider;
import com.bootcamp.powerhour.repository.ElectricityPriceRepository;
import com.bootcamp.powerhour.repository.PriceImportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:price-import-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "elering.import-on-startup=false",
        "elering.import-cron=-"})
public class PriceImportIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 6);
    private static final OffsetDateTime FIRST_SLOT = OffsetDateTime.of(2026, 11, 5, 22, 0, 0, 0, ZoneOffset.UTC);

    @MockitoBean
    private PriceProvider priceProvider;

    @Autowired
    private PriceImportService priceImportService;

    @Autowired
    private ElectricityPriceRepository electricityPriceRepository;

    @Autowired
    private PriceImportRepository priceImportRepository;

    @BeforeEach
    void cleanDatabase(){
        priceImportRepository.deleteAll();
        electricityPriceRepository.deleteAll();
    }

    @Test
    void importZone_shouldSavePricesAndLogSuccess() {
        when(priceProvider.fetchDayAhead(BiddingZone.EE, DATE)).thenReturn(slots("0.050000", "0.060000"));

        PriceImport result = priceImportService.importZone("EE", DATE);

        assertThat(result.getStatus()).isEqualTo(PriceImportStatus.SUCCESS);
        assertThat(result.getRecordsImported()).isEqualTo(2);
        assertThat(electricityPriceRepository.findAll()).hasSize(2);
        assertThat(priceImportRepository.findTop50ByOrderByStartedAtDesc()).hasSize(1);
    }

    @Test
    void importZone_twice_shouldUpdatePricesWithoutDuplicates(){
        when(priceProvider.fetchDayAhead(BiddingZone.EE, DATE)).thenReturn(slots("0.050000", "0.060000")).thenReturn(slots("0.070000", "0.080000"));

        priceImportService.importZone("EE", DATE);
        priceImportService.importZone("EE", DATE);

        List<ElectricityPrice> prices = electricityPriceRepository.findAll();
        assertThat(prices).hasSize(2);
        assertThat(prices).extracting(ElectricityPrice::getPriceEurKwh)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("0.07"), new BigDecimal("0.08"));
        assertThat(priceImportRepository.findAll()).hasSize(2);
    }

    @Test
    void importZone_whenProviderFails_shouldLogFailedAndSaveNoPrices(){
        when(priceProvider.fetchDayAhead(BiddingZone.EE, DATE)).thenThrow(new PriceProviderException("Elering API unavailable"));

        priceImportService.importZone("EE", DATE);

        assertThat(electricityPriceRepository.findAll()).isEmpty();
        PriceImport saved = priceImportRepository.findTop50ByOrderByStartedAtDesc().get(0);
        assertThat(saved.getStatus()).isEqualTo(PriceImportStatus.FAILED);
        assertThat(saved.getErrorMessage()).contains("Elering API unavailable");
        assertThat(saved.getFinishedAt()).isNotNull();
    }

    private static List<PricePoint> slots(String... prices){
        return IntStream.range(0, prices.length)
                .mapToObj(i -> new PricePoint(FIRST_SLOT
                        .plusMinutes(15L * i), new BigDecimal(prices[i])))
                .toList();
    }

}
