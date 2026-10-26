package br.edu.ifnmg.pagtesouro.infra.pagtesouro;

import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroRequestDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroQueryResponseDTO;
import br.edu.ifnmg.pagtesouro.services.config.SystemConfigurationService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cliente HTTP responsável por encapsular a comunicação direta com a API do PagTesouro da STN.
 * Utiliza o {@link WebClient} reativo para realizar requisições não-bloqueantes.
 *
 * @author Caio da Silva Viana
 */
@Component
public class PagTesouroClient {

  @Getter
  private final PagTesouroProperties properties;
  private final WebClient webClient;
  private SystemConfigurationService systemConfigurationService;

  @Autowired
  public PagTesouroClient(
      PagTesouroProperties properties,
      WebClient.Builder webClientBuilder,
      @Autowired(required = false) SystemConfigurationService systemConfigurationService) {
    this(properties, webClientBuilder);
    this.systemConfigurationService = systemConfigurationService;
  }

  public PagTesouroClient(PagTesouroProperties properties, WebClient.Builder webClientBuilder) {
    this.properties = properties;
    String base = this.properties.base_url() != null ? this.properties.base_url().trim() : "";
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (base.endsWith("/api/gru")) {
      base = base.substring(0, base.length() - "/api/gru".length());
    }
    this.webClient = webClientBuilder
        .baseUrl(base + "/api/gru/")
        .build();
  }

  private String resolveBearerToken() {
    if (systemConfigurationService != null) {
      String dynamicToken = systemConfigurationService.getPagTesouroToken();
      if (dynamicToken != null && !dynamicToken.isBlank()) {
        return dynamicToken;
      }
    }
    return properties.token();
  }

  /**
   * Envia uma solicitação de pagamento (criação de transação/GRU) para a API do PagTesouro.
   */
  public Mono<PagTesouroResponseDTO> createPayment(PagTesouroRequestDTO request) {
    return webClient
        .post()
        .uri("solicitacao-pagamento")
        .headers(headers -> headers.setBearerAuth(resolveBearerToken()))
        .bodyValue(request)
        .retrieve()
        .bodyToMono(PagTesouroResponseDTO.class);
  }

  /**
   * Consulta ativamente o status atual de uma transação específica no PagTesouro.
   */
  public Mono<PagTesouroQueryResponseDTO> getPaymentStatus(String idPayment) {
    return webClient
        .get()
        .uri("pagamentos/{id}", idPayment)
        .headers(headers -> headers.setBearerAuth(resolveBearerToken()))
        .retrieve()
        .bodyToMono(PagTesouroQueryResponseDTO.class);
  }
}
