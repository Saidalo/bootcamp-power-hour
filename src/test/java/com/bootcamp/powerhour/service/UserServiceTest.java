package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.dto.RegisterRequest;
import com.bootcamp.powerhour.dto.UserResponse;
import com.bootcamp.powerhour.entity.Role;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.exception.UserAlreadyExistsException;
import com.bootcamp.powerhour.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest createRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("anna");
        request.setEmail("anna@example.com");
        request.setPassword("password123");
        return request;
    }

    @Test
    void register_shouldSaveUserWithHashedPasswordAndUserRole() {
        when(userRepository.existsByUsernameIgnoreCase("anna")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("anna@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = userService.register(createRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isActive()).isTrue();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("anna");
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsernameIgnoreCase("anna")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(createRequest()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("anna");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        when(userRepository.existsByUsernameIgnoreCase("anna")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("anna@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(createRequest()))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("anna@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldKeepUsernameCaseAndStoreEmailInLowerCase() {
        RegisterRequest request = createRequest();
        request.setUsername(" Anna ");
        request.setEmail(" Anna@Example.COM ");
        when(userRepository.existsByUsernameIgnoreCase("Anna")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("anna@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("Anna");
        assertThat(captor.getValue().getEmail()).isEqualTo("anna@example.com");
    }
}
