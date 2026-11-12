package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(
            nullable = false,
            unique = true,
            length = 50,
            check = @CheckConstraint(constraint = "LENGTH(TRIM(username)) >= 3")
    )
    private String username;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(
            nullable = false,
            check = @CheckConstraint(constraint = "LENGTH(TRIM(password_hash)) > 0")
    )
    private String passwordHash;

    @Column(
            nullable = false,
            unique = true,
            length = 100,
            check = @CheckConstraint(constraint = "LENGTH(TRIM(email)) > 0")
    )
    private String email;

    @Column(nullable = false)
    private boolean isActive;
}
