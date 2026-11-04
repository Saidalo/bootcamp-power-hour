package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_alerts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private BiddingZone biddingZone;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal thresholdEurKwh;

    @Column(nullable = false)
    private boolean enabled;

    private LocalDate lastNotifiedForDate;
}
