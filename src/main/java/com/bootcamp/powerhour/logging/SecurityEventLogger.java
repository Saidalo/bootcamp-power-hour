package com.bootcamp.powerhour.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityEventLogger {

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        log.info("Login success: user={}", event.getAuthentication().getName());
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Login failed: user={} reason={}", event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        log.info("Logout: user={}", event.getAuthentication().getName());
    }

    @EventListener
    public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
        log.warn("Access denied: user={}", event.getAuthentication().get().getName());
    }
}
