package com.example.monitoramento_api.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.Instant;

@Entity
@Table(name = "monitored_api")
@Check(constraints = "interval_seconds >= 10 AND expected_status BETWEEN 100 AND 599")
@Getter
@Setter
@NoArgsConstructor
public class MonitoredApiEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String name;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(name = "interval_seconds", nullable = false)
	private int intervalSeconds;

	@Column(name = "expected_status", nullable = false)
	private int expectedStatus;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
