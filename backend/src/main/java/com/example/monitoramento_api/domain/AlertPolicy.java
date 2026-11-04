package com.example.monitoramento_api.domain;

/**
 * Limite de falhas consecutivas para considerar a API fora do ar.
 * Uma falha deixa a API DEGRADED; ao atingir este limite, ela passa a DOWN.
 */
public record AlertPolicy(int failuresToDown) {

	public AlertPolicy {
		if (failuresToDown < 1) {
			throw new IllegalArgumentException("O limite de falhas deve ser maior que zero");
		}
	}

}
