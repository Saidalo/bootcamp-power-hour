package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailService userDetailService;

    private User createUser(boolean active) {
        User user = new User();
        user.setUsername("anna");
        user.setEmail("anna@example.com");
        user.setPasswordHash("$2a$10$hash");
        user.setRole(Role.USER);
        user.setActive(active);
        return user;
    }

    @Test
    void loadUserByUsername_shouldReturnUserWithRole() {
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(createUser(true)));

        UserDetails details = userDetailService.loadUserByUsername("anna");

        assertThat(details.getUsername()).isEqualTo("anna");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_shouldDisableInactiveUser() {
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(createUser(false)));

        UserDetails details = userDetailService.loadUserByUsername("anna");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
