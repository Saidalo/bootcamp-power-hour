package com.bootcamp.powerhour.service;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.entity.User;
import com.bootcamp.powerhour.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public User get(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /** Users created before zones existed have none, so they fall back to LV. */
    public static BiddingZone zoneOf(User user) {
        return user.getBiddingZone() != null ? user.getBiddingZone() : BiddingZone.LV;
    }
}