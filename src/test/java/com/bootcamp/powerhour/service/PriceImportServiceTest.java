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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PriceImportServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 5);
    private static final OffsetDateTime SLOT = OffsetDateTime.of(2026, 11, 4, 22, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private PriceProvider priceProvider;

    @Mock
    private ElectricityPriceRepository priceRepository;

    @Mock
    private PriceImportRepository importRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Captor
    private ArgumentCaptor<List<ElectricityPrice>> savedPrices;

    private PriceImportService service;

    @BeforeEach
    void setUp() {
        service = new PriceImportService(priceProvider, importRepository, priceRepository, transactionManager);
        lenient().when(importRepository.save(any(PriceImport.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void importZone_shouldUpdateExistingPriceInsteadOfCreatingDuplicate() {
        ElectricityPrice existing = new ElectricityPrice();
        existing.setBiddingZone(BiddingZone.EE);
        existing.setStartsAt(SLOT);
        existing.setPriceEurKwh(new BigDecimal("0.010000"));

        when(priceProvider.fetchDayAhead(BiddingZone.EE, DATE))
                .thenReturn(List.of(new PricePoint(SLOT, new BigDecimal("0.042000"))));
        when(priceRepository.findByBiddingZoneAndStartsAtBetween(BiddingZone.EE, SLOT, SLOT))
                .thenReturn(List.of(existing));

        PriceImport result = service.importZone("ee", DATE);

        verify(priceRepository).saveAll(savedPrices.capture());
        assertThat(savedPrices.getValue()).containsExactly(existing);
        assertThat(existing.getPriceEurKwh()).isEqualByComparingTo("0.042");
        assertThat(result.getStatus()).isEqualTo(PriceImportStatus.SUCCESS);
        assertThat(result.getRecordsImported()).isEqualTo(1);
    }

    @Test
    void importZone_shouldLogFailedWhenProviderFails() {
        when(priceProvider.fetchDayAhead(BiddingZone.EE, DATE))
                .thenThrow(new PriceProviderException("Elering API unavailable"));

        PriceImport result = service.importZone("EE", DATE);

        assertThat(result.getStatus()).isEqualTo(PriceImportStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("Elering API unavailable");
        assertThat(result.getFinishedAt()).isNotNull();
        verify(priceRepository, never()).saveAll(any());
    }

    @Test
    void importAllZones_shouldContinueWhenOneZoneFails() {
        PricePoint point = new PricePoint(SLOT, new BigDecimal("0.050000"));
        when(priceProvider.fetchDayAhead(BiddingZone.EE, DATE)).thenThrow(new PriceProviderException("timeout"));
        when(priceProvider.fetchDayAhead(BiddingZone.LV, DATE)).thenReturn(List.of(point));
        when(priceProvider.fetchDayAhead(BiddingZone.LT, DATE)).thenReturn(List.of(point));

        PriceImport result = service.importAllZones(DATE);

        verify(priceRepository, times(2)).saveAll(any());
        assertThat(result.getStatus()).isEqualTo(PriceImportStatus.FAILED);
        assertThat(result.getRecordsImported()).isEqualTo(2);
        assertThat(result.getErrorMessage()).startsWith("EE:");
    }

    @Test
    void importZone_shouldThrowWhenZoneUnknown() {
        assertThatThrownBy(() -> service.importZone("XX", DATE))
                .isInstanceOf(ZoneNotFoundException.class);
    }
}
