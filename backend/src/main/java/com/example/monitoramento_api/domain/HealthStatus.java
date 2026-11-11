package com.example.monitoramento_api.domain;

public enum HealthStatus {
	/** Nenhuma checagem foi feita ainda. */
	UNKNOWN,
	UP,
	DEGRADED,
	DOWN
}
