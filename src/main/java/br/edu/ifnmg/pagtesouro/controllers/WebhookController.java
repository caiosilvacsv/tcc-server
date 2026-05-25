package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.WebhookRequestDTO;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controlador REST responsável por receber as notificações assíncronas de webhook (notificação de pagamento)
 * enviadas pelo PagTesouro.
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/payment")
public class WebhookController {

  private final PaymentSyncService paymentSyncService;

  public WebhookController(PaymentSyncService paymentSyncService) {
    this.paymentSyncService = paymentSyncService;
  }

  /**
   * Endpoint de Webhook que escuta as atualizações de transações enviadas pelo PagTesouro.
   * Dispara a sincronização ativa em background de forma reativa e responde imediatamente com 200 OK.
   *
   * @param request O DTO contendo o ID do pagamento alterado.
   * @return Um Mono com resposta HTTP 200 OK vazia.
   */
  @PostMapping("/webhook")
  public Mono<ResponseEntity<Void>> handleWebhook(@RequestBody WebhookRequestDTO request) {
    // Dispara a sincronização de forma reativa em background (sem travar a resposta)
    paymentSyncService.syncPaymentStatus(request.idPayment())
        .subscribe(
            success -> System.out.println("Sincronização de pagamento realizada com sucesso para o ID: " + request.idPayment()),
            error -> System.err.println("Erro ao processar sincronização de webhook: " + error.getMessage())
        );

    // Retorna imediatamente o status 200 OK exigido pelo governo para evitar retransmissões redundantes
    return Mono.just(ResponseEntity.ok().build());
  }
}
