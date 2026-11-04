package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.domain.ApiNameAlreadyExistsException;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respostas de erro no formato RFC 7807 (ProblemDetail).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiNotFoundException.class)
	ProblemDetail naoEncontrada(ApiNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ApiNameAlreadyExistsException.class)
	ProblemDetail nomeDuplicado(ApiNameAlreadyExistsException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	// Corrida entre duas gravacoes com o mesmo nome: a checagem da aplicacao passou, o indice unico barrou.
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail conflitoDeDados(DataIntegrityViolationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Os dados conflitam com uma API ja cadastrada");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validacao(MethodArgumentNotValidException e) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados invalidos");
		Map<String, String> erros = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));
		problema.setProperty("errors", erros);
		return problema;
	}

}
