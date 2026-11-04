package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.application.MonitoredApiService;
import com.example.monitoramento_api.domain.MonitoredApi;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/apis")
public class MonitoredApiController {

	private final MonitoredApiService servico;

	public MonitoredApiController(MonitoredApiService servico) {
		this.servico = servico;
	}

	@PostMapping
	public ResponseEntity<ApiResponse> criar(@Valid @RequestBody ApiRequest request, UriComponentsBuilder uri) {
		MonitoredApi api = servico.criar(request.paraComando());
		return ResponseEntity
				.created(uri.path("/apis/{id}").buildAndExpand(api.id()).toUri())
				.body(ApiResponse.from(api));
	}

	@GetMapping
	public List<ApiResponse> listar() {
		return servico.listar().stream().map(ApiResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ApiResponse buscar(@PathVariable Long id) {
		return ApiResponse.from(servico.buscar(id));
	}

	@PutMapping("/{id}")
	public ApiResponse atualizar(@PathVariable Long id, @Valid @RequestBody ApiRequest request) {
		return ApiResponse.from(servico.atualizar(id, request.paraComando()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		servico.remover(id);
		return ResponseEntity.noContent().build();
	}

}
