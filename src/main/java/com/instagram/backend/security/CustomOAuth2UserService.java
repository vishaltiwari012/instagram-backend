package com.instagram.backend.security;

import com.instagram.backend.entity.Role;
import com.instagram.backend.entity.User;
import com.instagram.backend.repository.RoleRepository;
import com.instagram.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        log.debug("🌐 OAuth2 user fetched. Email: {}, Name: {}", email, name);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.info("🆕 Registering new OAuth user: {}", email);

            Role userRole = roleRepository.findByName("USER")
                    .orElseGet(() -> {
                        log.info("🔧 'USER' role not found. Creating new one.");
                        return roleRepository.save(new Role(null, "USER"));
                    });

            user = User.builder()
                    .email(email)
                    .username(name)
                    .enabled(true)
                    .password("") // OAuth users don't have passwords
                    .roles(Set.of(userRole))
                    .build();

            user = userRepository.save(user);
            log.info("✅ User registered successfully with email: {}", user.getEmail());
        } else {
            log.info("✅ Existing user logged in with OAuth2: {}", user.getEmail());
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        log.debug("🔐 Assigned authorities: {}", authorities);

        return new DefaultOAuth2User(
                authorities,
                oauthUser.getAttributes(),
                "email"
        );
    }
}
