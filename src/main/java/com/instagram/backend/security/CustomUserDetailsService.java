package com.instagram.backend.security;

import com.instagram.backend.entity.User;
import com.instagram.backend.exception.EmailNotVerifiedException;
import com.instagram.backend.exception.UserNotFoundException;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.info("Loading user for authentication: {}", usernameOrEmail);

//        User user = userRepository.findByEmail(usernameOrEmail)
//                .or(() -> userRepository.findByUsername(usernameOrEmail))
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);

        User user = userOpt.orElseThrow(() ->
                new UserNotFoundException("User not found with username/email: " + usernameOrEmail)
        );

        if (!user.isEnabled()) {
            throw new EmailNotVerifiedException("Email not verified. Please verify your email before logging in.");
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // username
                user.getPassword(),
                authorities
        );
    }
}
