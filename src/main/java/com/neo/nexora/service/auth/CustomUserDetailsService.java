package com.neo.nexora.service.auth;

import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.service.user.lookUp.UserLookUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserLookUpService userLookUpService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check Bloom filter first to avoid unnecessary database queries
        if (!userLookUpService.shouldCheckDatabase(username)) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
