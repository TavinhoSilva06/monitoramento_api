package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.application.ApiCommand;
import com.example.monitoramento_api.domain.MonitoredApi;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiRequest(
		@NotBlank @Size(max = MonitoredApi.MAX_NAME_LENGTH) String name,
		@NotBlank @Size(max = 2048) String url,
		@Min(MonitoredApi.MIN_INTERVAL_SECONDS) int intervalSeconds,
		@Min(100) @Max(599) int expectedStatus,
		Boolean active) {

	public ApiCommand paraComando() {
		return new ApiCommand(name, url, intervalSeconds, expectedStatus, active == null || active);
	}

}
