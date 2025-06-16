package com.instagram.backend.dtos.response;

import com.instagram.backend.entity.Role;
import lombok.Data;

import java.util.Set;

@Data
public class AdminUserListDto {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private Set<Role> roles;
}
