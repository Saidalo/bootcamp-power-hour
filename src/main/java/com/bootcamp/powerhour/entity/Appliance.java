package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(
        name = "appliances",
        check = @CheckConstraint(
                constraint = "allowed_from < allowed_to"
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Appliance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private BiddingZone biddingZone;

    @Column(
            nullable = false,
            length = 100,
            check = @CheckConstraint(constraint = "LENGTH(TRIM(name)) > 0")
    )
    private String name;

    @Column(
            nullable = false,
            precision = 8,
            scale = 3,
            check = @CheckConstraint(constraint = "power_kw > 0")
    )
    private BigDecimal powerKw;

    @Column(
            nullable = false,
            check = @CheckConstraint(constraint = "duration_minutes > 0 AND duration_minutes <= 1440")
    )
    private Integer durationMinutes;

    @Column(nullable = false)
    private LocalTime allowedFrom;

    @Column(nullable = false)
    private LocalTime allowedTo;
}
