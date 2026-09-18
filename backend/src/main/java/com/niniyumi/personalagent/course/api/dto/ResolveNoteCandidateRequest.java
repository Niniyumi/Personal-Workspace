package com.niniyumi.personalagent.course.api.dto;

import com.niniyumi.personalagent.course.application.NoteCandidateAction;
import jakarta.validation.constraints.NotNull;

public record ResolveNoteCandidateRequest(@NotNull NoteCandidateAction action) { }
