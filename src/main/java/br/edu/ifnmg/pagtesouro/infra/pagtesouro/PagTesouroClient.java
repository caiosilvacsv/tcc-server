package br.edu.ifnmg.pagtesouro.infra.pagtesouro;

import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroRequestDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroQueryResponseDTO;
import lombok.Getter;
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

  /**
   * -- GETTER --
   *  Retorna as propriedades de configuração do PagTesouro.
   */
  @Getter
  private final PagTesouroProperties properties;
  private final WebClient webClient;

  /**
   * Construtor que recebe as propriedades do PagTesouro e o construtor do WebClient.
   * Utiliza injeção de dependência pelo Spring para reaproveitar o builder global.
   */
  public PagTesouroClient(PagTesouroProperties properties, WebClient.Builder webClientBuilder) {
    this.properties = properties;
    this.webClient = webClientBuilder
        .baseUrl(this.properties.base_url()+"/api/gru/")
        .build();
  }

  /**
   * Envia uma solicitação de pagamento (criação de transação/GRU) para a API do PagTesouro.
   *
   * @param request DTO com as informações fiscais e de valor da guia de pagamento.
   * @return Um {@link Mono} contendo os detalhes do pagamento criado e a URL de redirecionamento.
   */
  public Mono<PagTesouroResponseDTO> createPayment(PagTesouroRequestDTO request) {
    return webClient
        .post()
        .uri("solicitacao-pagamento")
        .headers(headers -> headers.setBearerAuth(properties.token_salinas()))
        .bodyValue(request)
        .retrieve()
        .bodyToMono(PagTesouroResponseDTO.class);
  }

  /**
   * Consulta ativamente o status atual de uma transação específica no PagTesouro.
   * Método essencial para auditoria, retorno de tela e conciliação bancária ativa.
   *
   * @param idPayment ID único da cobrança gerado pelo PagTesouro.
   * @return Um {@link Mono} com os detalhes consolidados da transação, incluindo meio e status de pagamento.
   */
  public Mono<PagTesouroQueryResponseDTO> getPaymentStatus(String idPayment) {
    return webClient
        .get()
        .uri("pagamentos/{id}", idPayment)
        .headers(headers -> headers.setBearerAuth(properties.token_salinas()))
        .retrieve()
        .bodyToMono(PagTesouroQueryResponseDTO.class);
  }
}
