package com.bootcamp.powerhour.dto;

import com.bootcamp.powerhour.entity.Role;

public record UserResponse(Long id,
                           String username,
                           String email,
                           Role role) {
}