package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "appliances")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Appliance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BiddingZone biddingZone;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal powerKw;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private LocalTime allowedFrom;

    @Column(nullable = false)
    private LocalTime allowedTo;
}
