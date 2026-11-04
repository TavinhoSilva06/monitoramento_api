package com.example.monitoramento_api.application;

/**
 * Dados para cadastrar ou substituir uma API monitorada.
 */
public record ApiCommand(
		String name,
		String url,
		int intervalSeconds,
		int expectedStatus,
		boolean active) {
}
