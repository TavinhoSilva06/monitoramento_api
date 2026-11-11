package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.application.ApiStatus;
import com.example.monitoramento_api.domain.HealthStatus;

public record ApiStatusResponse(
		Long id,
		String name,
		String url,
		boolean active,
		HealthStatus estado,
		ChecagemResponse ultimaChecagem) {

	public static ApiStatusResponse from(ApiStatus status) {
		return new ApiStatusResponse(
				status.api().id(),
				status.api().name(),
				status.api().url(),
				status.api().active(),
				status.estado(),
				status.ultimaChecagem() == null ? null : ChecagemResponse.from(status.ultimaChecagem()));
	}

}
