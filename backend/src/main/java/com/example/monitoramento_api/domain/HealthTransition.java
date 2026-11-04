package com.example.monitoramento_api.domain;

public record HealthTransition(HealthStatus from, HealthStatus to) {
}
