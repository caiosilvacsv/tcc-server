package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.services.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço agendador em segundo plano (Daemon) responsável por garantir a consistência
 * eventual do banco de dados (Failsafe/Contingência) com a API do PagTesouro.
 * <p>
 * **Conceito no TCC (Portal de Débitos):**
 * Caso o webhook da STN sofra alguma instabilidade de rede ou queda operacional, este
 * daemon faz a conciliação ativa ("pull") buscando guias pendentes locais e verificando
 * suas situações em PagTesouro.
 * Também gerencia o ciclo de expiração automática de pedidos abandonados por inatividade.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@Service
public class PaymentDaemonService {

  private final PaymentRepository paymentRepository;
  private final PaymentSyncService paymentSyncService;
  private final OrderService orderService;

  @Value("${api.order.abandoned-expiration-days:10}")
  private int abandonedExpirationDays;

  public PaymentDaemonService(
      PaymentRepository paymentRepository,
      PaymentSyncService paymentSyncService,
      OrderService orderService) {
    this.paymentRepository = paymentRepository;
    this.paymentSyncService = paymentSyncService;
    this.orderService = orderService;
  }

  /**
   * Executa a conciliação failsafe periodicamente (a cada 1 hora por padrão, customizável por properties).
   * Varre pagamentos pendentes criados há mais de 10 minutos (dando tempo para a interação inicial do usuário)
   * e há menos de 48 horas (limite máximo operacional de guias).
   */
  @Scheduled(cron = "${api.pagtesouro.daemon.cron:0 0 * * * *}")
  public void runPaymentFailsafeSync() {
    log.info("[DAEMON] Iniciando rotina automática de conciliação ativa Failsafe...");

    // 1. Varredura e expiração automática de pedidos abandonados
    try {
      int cancelados = orderService.cancelAbandonedOrders(abandonedExpirationDays);
      if (cancelados > 0) {
        log.info("[DAEMON] Sucesso: {} pedidos abandonados foram cancelados automaticamente por inatividade (> {} dias).",
            cancelados, abandonedExpirationDays);
      }
    } catch (Exception ex) {
      log.error("[DAEMON] Falha ao processar expiração de pedidos abandonados: {}", ex.getMessage());
    }

    // 2. Conciliação ativa de pagamentos pendentes com a STN
    Instant dataMinima = Instant.now().minusSeconds(48 * 3600L);
    Instant dataMaxima = Instant.now().minusSeconds(10 * 60L);

    List<PaymentStatus> statusPendentes = List.of(
        PaymentStatus.CREATED,
        PaymentStatus.STARTED,
        PaymentStatus.SUBMITTED
    );

    List<Payment> pagamentosPendentes = new ArrayList<>();

    // Busca os pagamentos em estado pendente que atendem ao critério de tempo
    for (PaymentStatus status : statusPendentes) {
      List<Payment> encontrados = paymentRepository.findAllByStatusAndCreatedAtBefore(status, dataMaxima);
      for (Payment p : encontrados) {
        if (p.getCreatedAt().isAfter(dataMinima) && p.getPagtesouroPaymentId() != null) {
          pagamentosPendentes.add(p);
        }
      }
    }

    if (pagamentosPendentes.isEmpty()) {
      log.debug("[DAEMON] Nenhum pagamento pendente elegível localizado para conciliação.");
      return;
    }

    log.info("[DAEMON] Localizados {} pagamentos pendentes elegíveis. Iniciando sincronização ativa...",
        pagamentosPendentes.size());

    //Executa no máximo duas buscas simultâneas por vez no PagTesouro, evitando sobrecarga.
    Flux.fromIterable(pagamentosPendentes)
        .flatMap(payment -> paymentSyncService
            .syncPaymentStatus(
                payment.getPagtesouroPaymentId()
            )
            .doOnSuccess(success ->
                log.info("[DAEMON] Sincronizado: {}. Novo Status: {}",
                    payment.getPagtesouroPaymentId(), success.getStatus()))
                .doOnError(error ->
                    log.error("[DAEMON] Erro ao sincronizar {}: {}",
                        payment.getPagtesouroPaymentId(), error.getMessage())),
        2)
        .subscribe();
  }
}
