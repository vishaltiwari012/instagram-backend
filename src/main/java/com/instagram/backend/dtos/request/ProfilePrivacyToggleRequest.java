package com.instagram.backend.dtos.request;

import lombok.Data;

@Data
public class ProfilePrivacyToggleRequest {
    private boolean privateProfile;
}