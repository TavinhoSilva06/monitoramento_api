package com.example.monitoramento_api.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Projeto local: o CRUD e o painel ficam abertos. Qualquer rota fora da lista e negada.
 * Para expor fora da maquina local, o CRUD precisa de autenticacao (secao 6 do checklist).
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
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

	/**
	 * Origens que podem chamar a API pelo navegador (o painel Angular). Configuravel em {@code monitoramento.cors.origens}.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${monitoramento.cors.origens:http://localhost:4200}") List<String> origens) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(origens);
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Content-Type", "Accept"));
		cors.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		fonte.registerCorsConfiguration("/apis/**", cors);
		fonte.registerCorsConfiguration("/apis", cors);
		return fonte;
	}

}
