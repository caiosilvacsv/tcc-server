package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço agendador em segundo plano (Daemon) responsável por garantir a consistência
 * eventual do banco de dados (Failsafe/Contingência) com a API do PagTesouro.
 * <p>
 * **Conceito no TCC (Portal de Débitos):**
 * Caso o webhook da STN sofra alguma instabilidade de rede ou queda operacional, este
 * daemon faz a conciliação ativa ("pull") buscando guias pendentes locais e verificando
 * suas situações em Salinas/PagTesouro.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Service
public class PaymentDaemonService {

  private final PaymentRepository paymentRepository;
  private final PaymentSyncService paymentSyncService;

  public PaymentDaemonService(PaymentRepository paymentRepository, PaymentSyncService paymentSyncService) {
    this.paymentRepository = paymentRepository;
    this.paymentSyncService = paymentSyncService;
  }

  /**
   * Executa a conciliação failsafe periodicamente (a cada 1 hora por padrão, customizável por properties).
   * Varre pagamentos pendentes criados há mais de 10 minutos (dando tempo para a interação inicial do usuário)
   * e há menos de 48 horas (limite máximo operacional de guias).
   */
  @Scheduled(cron = "${api.pagtesouro.daemon.cron:0 0 * * * *}")
  public void runPaymentFailsafeSync() {
    System.out.println("[DAEMON] Iniciando rotina automática de conciliação ativa Failsafe...");

    Instant dataMinima = Instant.now().minus(48, ChronoUnit.HOURS);
    Instant dataMaxima = Instant.now().minus(10, ChronoUnit.MINUTES);

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
      System.out.println("[DAEMON] Nenhum pagamento pendente elegível localizado para conciliação.");
      return;
    }

    System.out.printf("[DAEMON] Localizados %d pagamentos pendentes elegíveis. Iniciando sincronização ativa...%n", pagamentosPendentes.size());

    // Dispara reativamente a sincronização de cada pagamento pendente localizado
    for (Payment payment : pagamentosPendentes) {
      paymentSyncService.syncPaymentStatus(payment.getPagtesouroPaymentId())
          .subscribe(
              success -> System.out.printf("[DAEMON] Pagamento %s sincronizado com sucesso. Novo Status: %s%n",
                  payment.getPagtesouroPaymentId(), success.getStatus()),
              error -> System.err.printf("[DAEMON] Erro ao sincronizar pagamento %s: %s%n",
                  payment.getPagtesouroPaymentId(), error.getMessage())
          );
    }
  }
}
