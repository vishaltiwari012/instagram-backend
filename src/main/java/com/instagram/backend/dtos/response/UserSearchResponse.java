package com.instagram.backend.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserSearchResponse {
    private List<UserDto> users;
    private int count;
}
