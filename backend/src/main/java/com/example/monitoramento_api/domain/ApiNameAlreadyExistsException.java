package com.example.monitoramento_api.domain;

public class ApiNameAlreadyExistsException extends RuntimeException {

	public ApiNameAlreadyExistsException(String name) {
		super("Ja existe uma API com o nome: " + name);
	}

}
