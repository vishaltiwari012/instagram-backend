package com.instagram.backend.dtos.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserListResponse {
    private List<AdminUserListDto> users;
}
