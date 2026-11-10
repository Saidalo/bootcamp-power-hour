package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.ApplianceForm;
import com.bootcamp.powerhour.dto.ApplianceResponse;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.ApplianceNotFoundException;
import com.bootcamp.powerhour.repository.ApplianceRepository;
import com.bootcamp.powerhour.repository.PlannedRunRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplianceServiceTest {
    @Mock
    private ApplianceRepository applianceRepository;

    @Mock
    private PlannedRunRepository plannedRunRepository;

    @InjectMocks
    private ApplianceService service;

    @Test
    void getAppliancesByUserId_shouldReturnUserAppliances() {
        User user = new User();
        user.setId(1L);

        Appliance appliance1 = createAppliance(2L, user, "Dishwasher");
        Appliance appliance2 = createAppliance(3L, user, "Washing machine");

        when(applianceRepository.findAllByUserId(1L)).thenReturn(List.of(appliance1, appliance2));

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

        verify(applianceRepository).findAllByUserId(1L);
    }

    @Test
    void getAppliancesByUserId_shouldReturnEmptyList() {
        when(applianceRepository.findAllByUserId(1L)).thenReturn(List.of());

        List<ApplianceResponse> result = service.getAppliancesByUserId(1L);

        assertThat(result).isEmpty();
        verify(applianceRepository).findAllByUserId(1L);
    }

    @Test
    void getOwned_shouldReturnApplianceWhenOwnerMatches() {
        User user = new User();
        user.setId(1L);

        Appliance appliance = createAppliance(2L, user, "Dishwasher");

        when(applianceRepository.findById(2L)).thenReturn(Optional.of(appliance));

        Appliance result = service.getOwned(2L, 1L);

        assertThat(result).isSameAs(appliance);
        verify(applianceRepository).findById(2L);
    }

    @Test
    void getOwned_shouldThrowWhenApplianceNotFound() {
        when(applianceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwned(99L, 1L))
                .isInstanceOf(ApplianceNotFoundException.class)
                .hasMessage("Appliance with id 99 not found.");
        verify(applianceRepository).findById(99L);
    }

    @Test
    void getOwned_shouldThrowWhenOwnerDoesNotMatch() {
        User user = new User();
        user.setId(1L);

        Appliance appliance = createAppliance(10L, user, "Dishwasher");

        when(applianceRepository.findById(10L)).thenReturn(Optional.of(appliance));

        assertThatThrownBy(() -> service.getOwned(10L, 2L))
                .isInstanceOf(ApplianceNotFoundException.class)
                .hasMessage("Appliance with id 10 not found.");

        verify(applianceRepository).findById(10L);
    }

    @Test
    void toForm_shouldMapAllFields() {
        User user = new User();
        user.setId(1L);

        Appliance appliance = createAppliance(2L, user, "Dishwasher");

        ApplianceForm result = service.toForm(appliance);

        assertThat(result.getBiddingZone()).isEqualTo(BiddingZone.LV);
        assertThat(result.getName()).isEqualTo("Dishwasher");
        assertThat(result.getPowerKw()).isEqualByComparingTo("2.0");
        assertThat(result.getDurationMinutes()).isEqualTo(90);
        assertThat(result.getAllowedFrom()).isEqualTo(LocalTime.of(17, 0));
        assertThat(result.getAllowedTo()).isEqualTo(LocalTime.of(23, 0));
    }

    @Test
    void save_shouldCreateNewAppliance() {
        User user = new User();
        user.setId(1L);

        ApplianceForm form = new ApplianceForm();
        form.setBiddingZone(BiddingZone.LV);
        form.setName("  Dishwasher   ");
        form.setPowerKw(new BigDecimal("2.0"));
        form.setDurationMinutes(90);
        form.setAllowedFrom(LocalTime.of(17, 0));
        form.setAllowedTo(LocalTime.of(23, 0));

        when(applianceRepository.save(any(Appliance.class)))
                .thenAnswer(invocation -> {
                    Appliance appliance = invocation.getArgument(0);
                    appliance.setId(10);
                    return appliance;
                });

        Appliance result = service.save(null, form, user);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getBiddingZone()).isEqualTo(BiddingZone.LV);
        assertThat(result.getName()).isEqualTo("Dishwasher");
        assertThat(result.getPowerKw()).isEqualByComparingTo("2.0");
        assertThat(result.getDurationMinutes()).isEqualTo(90);
        assertThat(result.getAllowedFrom()).isEqualTo(LocalTime.of(17, 0));
        assertThat(result.getAllowedTo()).isEqualTo(LocalTime.of(23, 0));
    }

    @Test
    void save_shouldUpdateExistingAppliance() {
        User user = new User();
        user.setId(1L);

        Appliance appliance = createAppliance(10L, user, "Dishwasher");

        ApplianceForm form = new ApplianceForm();
        form.setBiddingZone(BiddingZone.EE);
        form.setName("Washing machine");
        form.setPowerKw(new BigDecimal("3.0"));
        form.setDurationMinutes(120);
        form.setAllowedFrom(LocalTime.of(22, 0));
        form.setAllowedTo(LocalTime.of(5, 0));

        when(applianceRepository.findById(10L)).thenReturn(Optional.of(appliance));
        when(applianceRepository.save(appliance)).thenReturn(appliance);

        Appliance result = service.save(10L, form, user);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getBiddingZone()).isEqualTo(BiddingZone.EE);
        assertThat(result.getName()).isEqualTo("Washing machine");
        assertThat(result.getPowerKw()).isEqualByComparingTo("3.0");
        assertThat(result.getDurationMinutes()).isEqualTo(120);
        assertThat(result.getAllowedFrom()).isEqualTo(LocalTime.of(22, 0));
        assertThat(result.getAllowedTo()).isEqualTo(LocalTime.of(5, 0));
        verify(applianceRepository).findById(10L);
        verify(applianceRepository).save(appliance);
    }

    @Test
    void save_shouldThrowWhenUpdatingAnotherUsersAppliance() {
        User owner = new User();
        owner.setId(2L);

        User currentUser = new User();
        currentUser.setId(1L);

        Appliance appliance = createAppliance(10L, owner, "Dishwasher");

        ApplianceForm form = new ApplianceForm();
        form.setBiddingZone(BiddingZone.EE);
        form.setName("Washing machine");
        form.setPowerKw(new BigDecimal("3.0"));
        form.setDurationMinutes(120);
        form.setAllowedFrom(LocalTime.of(22, 0));
        form.setAllowedTo(LocalTime.of(5, 0));

        when(applianceRepository.findById(10L)).thenReturn(Optional.of(appliance));

        assertThatThrownBy(() -> service.save(10L, form, currentUser))
                .isInstanceOf(ApplianceNotFoundException.class)
                .hasMessage("Appliance with id 10 not found.");

        verify(applianceRepository).findById(10L);
        verify(applianceRepository, never()).save(any(Appliance.class));
        assertThat(appliance.getName()).isEqualTo("Dishwasher");
    }

    @Test
    void delete_shouldDeleteOwnedApplianceAndPlannedRuns() {
        User user = new User();
        user.setId(1L);

        Appliance appliance = createAppliance(10L, user, "Dishwasher");

        when(applianceRepository.findById(10L)).thenReturn(Optional.of(appliance));

        service.delete(10L, 1L);

        verify(applianceRepository).findById(10L);
        verify(plannedRunRepository).deleteByApplianceId(10L);
        verify(applianceRepository).delete(appliance);
    }

    @Test
    void delete_shouldThrowWhenOwnerDoesNotMatch() {
        User user = new User();
        user.setId(2L);

        Appliance appliance = createAppliance(10L, user, "Dishwasher");

        when(applianceRepository.findById(10L)).thenReturn(Optional.of(appliance));

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(ApplianceNotFoundException.class)
                .hasMessage("Appliance with id 10 not found.");

        verify(applianceRepository).findById(10L);
        verify(applianceRepository, never()).delete(any(Appliance.class));
        verifyNoInteractions(plannedRunRepository);
    }

    @Test
    void delete_shouldThrowWhenApplianceNotFound() {
        when(applianceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L, 1L))
                .isInstanceOf(ApplianceNotFoundException.class)
                .hasMessage("Appliance with id 99 not found.");

        verify(applianceRepository).findById(99L);
        verify(applianceRepository, never()).delete(any(Appliance.class));
        verifyNoInteractions(plannedRunRepository);
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
