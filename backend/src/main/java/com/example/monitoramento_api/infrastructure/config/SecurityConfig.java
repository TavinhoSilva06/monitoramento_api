package com.example.monitoramento_api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Projeto local: o CRUD e o painel ficam abertos. Qualquer rota fora da lista e negada.
 * Para expor fora da maquina local, o CRUD precisa de autenticacao (secao 6 do checklist).
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/apis",
								"/apis/**",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/actuator/health",
								"/actuator/health/**",
								"/actuator/prometheus",
								"/error")
						.permitAll()
						.anyRequest().denyAll());
		return http.build();
	}

}
