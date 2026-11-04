package com.example.monitoramento_api.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "health_check", indexes = @Index(name = "idx_health_check_api_checked_at", columnList = "api_id, checked_at"))
@Getter
@Setter
@NoArgsConstructor
public class HealthCheckEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "api_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private MonitoredApiEntity api;

	@Column(name = "status_code")
	private Integer statusCode;

	// Nulo quando nao houve resposta
	@Column(name = "response_time_ms")
	private Long responseTimeMs;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

}
