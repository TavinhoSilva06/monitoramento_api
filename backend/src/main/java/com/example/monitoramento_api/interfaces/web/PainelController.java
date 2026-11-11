package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.application.ConsultaPainelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Leitura para o painel: situacao atual, historico e resumo. As datas usam ISO-8601 em UTC (ex.: 2026-01-01T00:00:00Z).
 */
@RestController
@RequestMapping("/apis")
public class PainelController {

	private final ConsultaPainelService consultas;

	public PainelController(ConsultaPainelService consultas) {
		this.consultas = consultas;
	}

	@GetMapping("/status")
	public List<ApiStatusResponse> status() {
		return consultas.status().stream().map(ApiStatusResponse::from).toList();
	}

	@GetMapping("/{id}/checks")
	public List<ChecagemResponse> checagens(
			@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		return consultas.checagens(id, from, to).stream().map(ChecagemResponse::from).toList();
	}

	@GetMapping("/{id}/summary")
	public ResumoResponse resumo(
			@PathVariable Long id,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		return ResumoResponse.from(consultas.resumo(id, from, to));
	}

}
