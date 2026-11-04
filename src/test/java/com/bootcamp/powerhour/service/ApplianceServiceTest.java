package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.ApplianceResponse;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.ApplianceNotFoundException;
import com.bootcamp.powerhour.repository.ApplianceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApplianceServiceTest {
    @Mock
    private ApplianceRepository repository;

    @InjectMocks
    private ApplianceService service;

    @Test
    void getAllAppliances_shouldReturnAllAppliances() {
        User user = new User();
        user.setId(1L);

        Appliance appliance1 = createAppliance(2L, user, "Dishwasher");
        Appliance appliance2 = createAppliance(3L, user, "Washing machine");

        when(repository.findAll()).thenReturn(List.of(appliance1, appliance2));

        List<ApplianceResponse> result = service.getAllAppliances();

        assertThat(result)
                .hasSize(2)
                .extracting(ApplianceResponse::id)
                .containsExactly(2L, 3L);
        assertThat(result)
                .extracting(ApplianceResponse::name)
                .containsExactly("Dishwasher", "Washing machine");

        verify(repository).findAll();
    }

    @Test
    void getAllAppliances_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<ApplianceResponse> result = service.getAllAppliances();

        assertThat(result).isEmpty();
        verify(repository).findAll();
    }

    @Test
    void getAppliancesByUserId_shouldReturnUserAppliances() {
        User user = new User();
        user.setId(1L);

        Appliance appliance1 = createAppliance(2L, user, "Dishwasher");
        Appliance appliance2 = createAppliance(3L, user, "Washing machine");

        when(repository.findAllByUserId(1L)).thenReturn(List.of(appliance1, appliance2));

        List<ApplianceResponse> result = service.getAppliancesByUserId(1L);

        assertThat(result)
                .hasSize(2)
                .extracting(ApplianceResponse::id)
                .containsExactly(2L, 3L);
        assertThat(result)
                .extracting(ApplianceResponse::name)
                .containsExactly("Dishwasher", "Washing machine");
        assertThat(result)
                .extracting(ApplianceResponse::userId)
                .containsOnly(1L);

        verify(repository).findAllByUserId(1L);
    }

    @Test
    void getAppliancesByUserId_shouldReturnEmptyList() {
        when(repository.findAllByUserId(1L)).thenReturn(List.of());

        List<ApplianceResponse> result = service.getAppliancesByUserId(1L);

        assertThat(result).isEmpty();
        verify(repository).findAllByUserId(1L);
    }

    @Test
    void getApplianceById_shouldReturnAppliance() {
        User user = new User();
        user.setId(1L);

        Appliance appliance = createAppliance(2L, user, "Dishwasher");

        when(repository.findById(2L)).thenReturn(Optional.of(appliance));

        ApplianceResponse result = service.getApplianceById(2L);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Dishwasher");

        verify(repository).findById(2L);
    }

    @Test
    void getApplianceById_shouldThrowWhenApplianceNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getApplianceById(1L))
                .isInstanceOf(ApplianceNotFoundException.class)
                .hasMessage("Appliance with id 1 not found.");
    }

    private Appliance createAppliance(Long id, User user, String name) {
        return new Appliance(
                id,
                user,
                BiddingZone.LV,
                name,
                new BigDecimal("2.0"),
                90,
                LocalTime.of(17, 0),
                LocalTime.of(23, 0)
        );
    }
}
