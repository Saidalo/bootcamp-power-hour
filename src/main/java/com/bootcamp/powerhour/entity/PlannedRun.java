package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "planned_runs",
        check = @CheckConstraint(
                constraint = "starts_at < ends_at"
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlannedRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appliance_id", nullable = false)
    private Appliance appliance;
    /**
     * The bidding zone the appliance was in when the run was planned.
     * Kept here so a later move to another zone cannot change past runs.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private BiddingZone biddingZone;

    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Column(nullable = false)
    private OffsetDateTime endsAt;

    /**
     * How much the run was expected to use, in kWh, as calculated at planning
     * time. Not derived from the appliance on read: editing an appliance must
     * not change what past months cost.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 4,
            check = @CheckConstraint(constraint = "energy_kwh > 0")
    )
    private BigDecimal energyKwh;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlannedRunStatus status;
}
