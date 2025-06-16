package com.instagram.backend.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String bio;
    private String profileImageUrl;
    private int postsCount;
    private int followersCount;
    private int followingCount;
}
