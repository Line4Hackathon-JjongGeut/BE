package com.jjonggeut.domain.application.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.time.LocalDateTime;

public record ApplicationCancelResponse(Long applicationId, ApplicationStatus status, LocalDateTime cancelledAt) {
}
