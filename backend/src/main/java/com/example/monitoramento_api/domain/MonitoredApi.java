package com.example.monitoramento_api.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

public record MonitoredApi(
		Long id,
		String name,
		String url,
		int intervalSeconds,
		int expectedStatus,
		boolean active,
		Instant createdAt) {

	public static final int MIN_INTERVAL_SECONDS = 10;
	public static final int MAX_NAME_LENGTH = 120;

	public MonitoredApi {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("O nome da API e obrigatorio");
		}
		if (name.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("O nome da API deve ter no maximo " + MAX_NAME_LENGTH + " caracteres");
		}
		validarUrl(url);
		if (intervalSeconds < MIN_INTERVAL_SECONDS) {
			throw new IllegalArgumentException("O intervalo minimo e de " + MIN_INTERVAL_SECONDS + " segundos");
		}
		if (expectedStatus < 100 || expectedStatus > 599) {
			throw new IllegalArgumentException("O status HTTP esperado deve estar entre 100 e 599");
		}
	}

	private static void validarUrl(String url) {
		if (url == null) {
			throw new IllegalArgumentException("A URL da API e obrigatoria");
		}
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			boolean protocoloValido = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
			if (!protocoloValido || uri.getHost() == null) {
				throw new IllegalArgumentException("A URL deve ser http ou https e conter um host");
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("URL invalida: " + url, e);
		}
	}

}
