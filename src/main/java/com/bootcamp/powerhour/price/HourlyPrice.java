package com.bootcamp.powerhour.price;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "hourly_prices", uniqueConstraints = @UniqueConstraint(columnNames = {"zone_id", "hour_start"}))
@Getter
@Setter
public class HourlyPrice {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private BiddingZone zone;

    @Column(name = "hour_start", nullable = false)
    private Instant hourStart;

    @Column(name = "price_eur_mwh", nullable = false, precision = 10, scale = 3)
    private BigDecimal priceEurMwh;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;


}
