package com.example.monitoramento_api.domain;

public class ApiNotFoundException extends RuntimeException {

	public ApiNotFoundException(Long id) {
		super("API monitorada nao encontrada: " + id);
	}

}
