package com.instagram.backend.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendTokenRequest {
    @Email
    @NotBlank
    private String email;
}
