package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "price_imports")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PriceImport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PriceImportStatus status;

    @Column(
            check = @CheckConstraint(
                    constraint = "records_imported >= 0"
            )
    )
    private Integer recordsImported;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
