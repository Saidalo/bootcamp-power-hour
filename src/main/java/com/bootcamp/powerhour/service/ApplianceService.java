package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.ApplianceForm;
import com.bootcamp.powerhour.dto.ApplianceResponse;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.ApplianceNotFoundException;
import com.bootcamp.powerhour.repository.ApplianceRepository;
import com.bootcamp.powerhour.repository.PlannedRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ApplianceService {
    private final ApplianceRepository applianceRepository;
    private final PlannedRunRepository plannedRunRepository;

    public ApplianceService(ApplianceRepository applianceRepository, PlannedRunRepository plannedRunRepository) {
        this.applianceRepository = applianceRepository;
        this.plannedRunRepository = plannedRunRepository;
    }

    public List<ApplianceResponse> getAllAppliances() {
        return applianceRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ApplianceResponse> getAppliancesByUserId(Long userId) {
        return applianceRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ApplianceResponse getApplianceById(Long id) {
        Appliance appliance = applianceRepository.findById(id).orElseThrow(
                () -> new ApplianceNotFoundException("Appliance with id " + id + " not found.")
        );

        return mapToResponse(appliance);
    }

//  The appliance, but only if it belongs to the user; otherwise 404.
    public Appliance getOwned(Long id, Long userId) {
        Appliance appliance = applianceRepository.findById(id)
                .orElseThrow(() -> new ApplianceNotFoundException("Appliance with id " + id + " not found."));
        if (appliance.getUser().getId() != userId) {
            throw new ApplianceNotFoundException("Appliance with id " + id + " not found.");
        }
        return appliance;
    }

    public ApplianceForm toForm(Appliance appliance) {
        ApplianceForm form = new ApplianceForm();
        form.setName(appliance.getName());
        form.setPowerKw(appliance.getPowerKw());
        form.setDurationMinutes(appliance.getDurationMinutes());
        form.setAllowedFrom(appliance.getAllowedFrom());
        form.setAllowedTo(appliance.getAllowedTo());
        return form;
    }

//  Creates a new appliance when id is null, otherwise updates the user's own appliance.
    @Transactional
    public Appliance save(Long id, ApplianceForm form, User owner) {
        Appliance appliance = id == null ? new Appliance() : getOwned(id, owner.getId());
        if (id == null) {
            appliance.setUser(owner);
            appliance.setBiddingZone(CurrentUserService.zoneOf(owner));
        }
        appliance.setName(form.getName().trim());
        appliance.setPowerKw(form.getPowerKw());
        appliance.setDurationMinutes(form.getDurationMinutes());
        appliance.setAllowedFrom(form.getAllowedFrom());
        appliance.setAllowedTo(form.getAllowedTo());
        Appliance saved = applianceRepository.save(appliance);
        log.info("Appliance saved: id={} user={}", saved.getId(), owner.getUsername());
        return saved;
    }

    /** Deletes the appliance together with its planned runs. */
    @Transactional
    public void delete(Long id, Long userId) {
        Appliance appliance = getOwned(id, userId);
        plannedRunRepository.deleteByApplianceId(appliance.getId());
        applianceRepository.delete(appliance);
        log.info("Appliance deleted: id={}", id);
    }

    private ApplianceResponse mapToResponse(Appliance appliance) {
        return new ApplianceResponse(
                appliance.getId(),
                appliance.getUser().getId(),
                appliance.getBiddingZone(),
                appliance.getName(),
                appliance.getPowerKw(),
                appliance.getDurationMinutes(),
                appliance.getAllowedFrom(),
                appliance.getAllowedTo()
        );
    }

}