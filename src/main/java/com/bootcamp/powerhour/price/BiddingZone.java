package com.bootcamp.powerhour.price;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Entity
@Table(name = "bidding_zones")
@Getter
@Setter
public class BiddingZone {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 2)
    private String code;

    @Column (nullable = false)
    private String name;

    @Column (nullable = false)
    private String timezone;

    @Column (nullable = false)
    private boolean active = true;

}
