package com.example.monitoramento_api.infrastructure.scheduling;

import com.example.monitoramento_api.application.RetencaoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Roda a limpeza do historico todo dia (por padrao as 03:00). O horario fica em {@code monitoramento.retencao.cron}.
 */
@Component
class RetencaoScheduler {

	private final RetencaoService retencao;

	RetencaoScheduler(RetencaoService retencao) {
		this.retencao = retencao;
	}

	@Scheduled(cron = "${monitoramento.retencao.cron:0 0 3 * * *}")
	void executar() {
		retencao.executar();
	}

}
