package com.niniyumi.personalagent.auth.api.dto;

import com.niniyumi.personalagent.common.api.validation.Utf8ByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[^@]+$", message = "Username must not contain '@'") String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8) @Utf8ByteLength(max = 72) String password,
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String code) {
}
