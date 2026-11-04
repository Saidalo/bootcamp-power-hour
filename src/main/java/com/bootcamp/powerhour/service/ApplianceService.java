package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.ApplianceResponse;
import com.bootcamp.powerhour.entity.Appliance;
import com.bootcamp.powerhour.exception.ApplianceNotFoundException;
import com.bootcamp.powerhour.repository.ApplianceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplianceService {
    private final ApplianceRepository applianceRepository;

    public ApplianceService(ApplianceRepository applianceRepository) {
        this.applianceRepository = applianceRepository;
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
