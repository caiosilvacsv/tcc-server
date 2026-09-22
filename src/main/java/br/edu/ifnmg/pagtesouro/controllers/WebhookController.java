package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroProperties;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.WebhookRequestDTO;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Controlador REST responsável por receber as notificações assíncronas de webhook (notificação de pagamento)
 * enviadas pelo PagTesouro.
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@RestController
@RequestMapping("/payment")
public class WebhookController {

  private final PaymentSyncService paymentSyncService;
  private final PagTesouroProperties properties;

  public WebhookController(PaymentSyncService paymentSyncService, PagTesouroProperties properties) {
    this.paymentSyncService = paymentSyncService;
      this.properties = properties;
  }

  /**
   * Endpoint de Webhook que escuta as atualizações de transações enviadas pelo PagTesouro.
   * Dispara a sincronização ativa em background de forma reativa e responde imediatamente com 200 OK.
   *
   * @param request O DTO contendo o ID do pagamento alterado.
   * @return Um Mono com resposta HTTP 200 OK vazia.
   */
  @PostMapping("/webhook")
  public Mono<ResponseEntity<Void>> handleWebhook(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody WebhookRequestDTO request
  ) {

    //Valida a existência do token e assinatura do cabeçalho Bearer do governo.
    if (auth == null || !auth.startsWith("Bearer "))
      return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

    String token = auth.replace("Bearer ", "");
    if(!token.equals(properties.token()))
      return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());

    // Dispara a sincronização de forma reativa em background (sem travar a resposta)
    paymentSyncService.syncPaymentStatus(request.idPayment())
        .publishOn(Schedulers.boundedElastic())
        .subscribe(
            success -> log.info("Sincronização de pagamento realizada com sucesso para o ID: {}", request.idPayment()),
            error -> log.error("Erro ao processar sincronização de webhook: {}", error.getMessage())
        );

    // Retorna imediatamente o status 200 OK exigido pelo governo para evitar retransmissões redundantes
    return Mono.just(ResponseEntity.ok().build());
  }
}
