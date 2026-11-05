package com.bootcamp.powerhour.repository;

import com.bootcamp.powerhour.entity.PriceImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceImportRepository extends JpaRepository<PriceImport, Long> {
    List<PriceImport> findTop50ByOrderByStartedAtDesc();
}