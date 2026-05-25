package br.edu.ifnmg.pagtesouro.infra.pagtesouro;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Classe de configuração do ecossistema do Spring encarregada de fornecer e instanciar
 * componentes de comunicação cliente-servidor para integração com APIs externas de pagamento.
 * <p>
 * **Conceito e Justificativa no TCC:**
 * Disponibiliza a infraestrutura de comunicação reativa e assíncrona baseada no Spring WebFlux
 * para interagir com a API REST da STN (PagTesouro). A escolha do {@link WebClient} em detrimento
 * de clientes síncronos clássicos (como {@code RestTemplate}) justifica-se pela arquitetura
 * não-bloqueante de threads de I/O, otimizando o consumo de recursos e aumentando a resiliência
 * em chamadas com latência de rede variável.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Configuration
public class PagTesouroConfig {

  /**
   * Instancia e registra o Bean gerenciador do construtor padrão do {@link WebClient}.
   * Este construtor permite criar instâncias customizadas e seguras de clientes reativos HTTP
   * em diferentes pontos de injeção de dependência na camada de serviço.
   *
   * @return Um builder {@link WebClient.Builder} configurado com os padrões do Spring
   */
  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }
}

