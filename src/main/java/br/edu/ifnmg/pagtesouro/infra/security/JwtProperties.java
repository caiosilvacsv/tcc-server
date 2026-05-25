package br.edu.ifnmg.pagtesouro.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Record de configuração responsável por mapear de forma tipada as chaves de segurança
 * declaradas sob o prefixo "api.security.token" no application.properties.
 * <p>
 * **Conceito no TCC (Segurança e Configuração Desacoplada):**
 * Captura dados sensíveis como o segredo da chave HMAC256, emissor (issuer), audiência (audience)
 * e o tempo limite de expiração do JWT (em minutos), viabilizando o desacoplamento e a injeção
 * segura de segredos via variáveis de ambiente.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@ConfigurationProperties(prefix = "api.security.token")
public record JwtProperties (
    /**
     * O segredo utilizado na assinatura criptográfica HMAC256 do JWT.
     */
    String secret,
    
    /**
     * O emissor responsável pela validação do token (issuer).
     */
    String issuer,
    
    /**
     * O destinatário/aplicação alvo do token (audience).
     */
    String audience,
    
    /**
     * O tempo limite em minutos de validade antes da expiração do token.
     */
    long expirationTimeInMinutes
) {
}
