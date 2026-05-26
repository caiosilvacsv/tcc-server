package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Serviço responsável por realizar a sincronização e conciliação ativa do status dos pagamentos
 * locais com a API oficial do PagTesouro.
 *
 * @author Caio da Silva Viana
 */
@Service
public class PaymentSyncService {

  private final PaymentRepository paymentRepository;
  private final PagTesouroClient pagTesouroClient;

  public PaymentSyncService(PaymentRepository paymentRepository, PagTesouroClient pagTesouroClient) {
    this.paymentRepository = paymentRepository;
    this.pagTesouroClient = pagTesouroClient;
  }

  /**
   * Sincroniza ativamente o status de um pagamento local com a situação real na API da STN.
   * Ao confirmar a liquidação (COMPLETED), realiza a atualização granular de cada item do pedido associado.
   *
   * @param pagtesouroPaymentId ID externo do pagamento gerado no PagTesouro.
   * @return Um Mono contendo o pagamento atualizado no banco de dados.
   */
  @Transactional
  public Mono<Payment> syncPaymentStatus(String pagtesouroPaymentId) {
    return pagTesouroClient.getPaymentStatus(pagtesouroPaymentId)
        .publishOn(Schedulers.boundedElastic()) // Aloca as chamadas bloqueantes abaixo para a thread pool correta
        .flatMap(ptResponse -> {
          // Localiza o pagamento correspondente na nossa base pelo ID externo do PagTesouro
          Payment payment = paymentRepository.findByPagtesouroPaymentId(pagtesouroPaymentId)
              .orElseThrow(() -> new IllegalArgumentException("Pagamento com ID externo " + pagtesouroPaymentId + " não localizado!"));

          // Atualiza o status do pagamento baseado na situação real do governo
          payment.setStatus(ptResponse.situation().code());
          payment.setPaidAt(ptResponse.situation().date());
          payment.setPaymentMethod(ptResponse.typePayment());
          payment.setPspName(ptResponse.paymentServiceProviderName());
          payment.setPspTransactionId(ptResponse.pspTransactionId());

          // TCC Business Rule: Se a transação foi paga com sucesso na STN, liquida granularmente seus itens de serviço
          if (payment.getStatus() == PaymentStatus.COMPLETED) {
            if (payment.getOrderItems() != null) {
              for (OrderItem item : payment.getOrderItems()) {
                item.setStatus(OrderItemStatus.PAID);
                item.setPaidAt(payment.getPaidAt());
              }
            }
          }

          // Persiste as informações de liquidação no Postgres
        return Mono.fromCallable(() -> paymentRepository.save(payment));
        });
  }
}
