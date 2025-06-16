package com.instagram.backend.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PostRequestDto {
    @NotBlank(message = "Caption cannot be blank")
    private String caption;
    private MultipartFile image;
}