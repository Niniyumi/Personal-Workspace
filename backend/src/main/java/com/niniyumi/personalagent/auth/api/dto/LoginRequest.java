package com.niniyumi.personalagent.auth.api.dto;

import com.niniyumi.personalagent.common.api.validation.Utf8ByteLength;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String login, @NotBlank @Utf8ByteLength(max = 72) String password) {
}
