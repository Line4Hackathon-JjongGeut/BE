package com.jjonggeut.domain.application.dto;

import com.jjonggeut.domain.activity.entity.ApplicationStatus;
import java.util.List;

public record StatusHistoryResponse(Long applicationId, ApplicationStatus currentStatus,
		List<StatusHistoryItem> items) {
}
