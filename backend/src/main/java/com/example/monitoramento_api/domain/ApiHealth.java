package com.example.monitoramento_api.domain;

import java.util.Optional;

/**
 * Estado de saude de uma API, atualizado a cada checagem.
 */
public class ApiHealth {

	private final AlertPolicy policy;
	private HealthStatus status = HealthStatus.UP;
	private int consecutiveFailures;

	public ApiHealth(AlertPolicy policy) {
		this.policy = policy;
	}

	/**
	 * Registra o resultado de uma checagem.
	 *
	 * @return a transicao de estado, se houve mudanca; vazio caso contrario
	 */
	public Optional<HealthTransition> register(boolean success) {
		HealthStatus anterior = status;

		if (success) {
			consecutiveFailures = 0;
			status = HealthStatus.UP;
		} else {
			consecutiveFailures++;
			status = consecutiveFailures >= policy.failuresToDown() ? HealthStatus.DOWN : HealthStatus.DEGRADED;
		}

		if (anterior == status) {
			return Optional.empty();
		}
		return Optional.of(new HealthTransition(anterior, status));
	}

	public HealthStatus status() {
		return status;
	}

	public int consecutiveFailures() {
		return consecutiveFailures;
	}

}
