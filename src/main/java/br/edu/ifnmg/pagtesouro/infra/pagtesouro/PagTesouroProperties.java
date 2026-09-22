package br.edu.ifnmg.pagtesouro.infra.pagtesouro;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Record responsável por capturar de forma segura as propriedades de configuração do PagTesouro
 * mapeadas sob o prefixo "api.pagtesouro" no arquivo application.properties.
 *
 * @author Caio da Silva Viana
 */
@ConfigurationProperties(prefix = "api.pagtesouro")
public record PagTesouroProperties(
    String base_url,
    String token,
    String url_notificacao
) {
  public PagTesouroProperties {
    if (base_url != null) {
      base_url = base_url.replace("\"", "").replace("'", "").trim();
    }
    if (token != null) {
      token = token.replace("\"", "").replace("'", "").trim();
    }
    if (url_notificacao != null) {
      url_notificacao = url_notificacao.replace("\"", "").replace("'", "").trim();
    }
  }
}
