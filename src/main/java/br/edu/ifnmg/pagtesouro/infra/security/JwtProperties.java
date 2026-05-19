package br.edu.ifnmg.pagtesouro.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.security.token")
public record JwtProperties (
    String secret,
    String issuer,
    String audience,
    long expirationTimeInMinutes
) {
}
