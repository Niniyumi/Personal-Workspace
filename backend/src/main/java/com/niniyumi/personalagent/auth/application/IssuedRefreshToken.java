package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.RefreshSession;

public record IssuedRefreshToken(String value, RefreshSession session) {
}
