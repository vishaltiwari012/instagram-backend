package com.instagram.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class AuthenticationFacadeImpl implements AuthenticationFacade{
    @Override
    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("No authentication found in security context.");
        } else {
            log.debug("Retrieved authentication: {}", authentication.getName());
        }
        return authentication;
    }
}
