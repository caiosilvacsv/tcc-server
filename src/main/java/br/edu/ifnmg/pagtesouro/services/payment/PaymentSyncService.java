package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.repository.OrderItemRepository;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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
  private final OrderItemRepository orderItemRepository;
  private final OrderRepository orderRepository;
  private final PagTesouroClient pagTesouroClient;
  private final PaymentNotificationService paymentNotificationService;
  private final TransactionTemplate transactionTemplate;

  public PaymentSyncService(
      PaymentRepository paymentRepository,
      OrderItemRepository orderItemRepository,
      OrderRepository orderRepository,
      PagTesouroClient pagTesouroClient,
      PaymentNotificationService paymentNotificationService,
      TransactionTemplate transactionTemplate) {
    this.paymentRepository = paymentRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderRepository = orderRepository;
    this.pagTesouroClient = pagTesouroClient;
    this.paymentNotificationService = paymentNotificationService;
    this.transactionTemplate = transactionTemplate;
  }

  /**
   * Sincroniza ativamente o status de um pagamento local com a situação real na API da STN.
   * Ao confirmar a liquidação (COMPLETED), realiza a atualização granular de cada item do pedido associado.
   * Utiliza {@link TransactionTemplate} para garantir que as operações de escrita no banco de dados
   * sejam executadas atomicamente dentro de uma transação ativa na thread de background.
   *
   * @param pagtesouroPaymentId ID externo do pagamento gerado no PagTesouro.
   * @return Um Mono contendo o pagamento atualizado no banco de dados.
   */
  public Mono<Payment> syncPaymentStatus(String pagtesouroPaymentId) {
    return pagTesouroClient.getPaymentStatus(pagtesouroPaymentId)
        .publishOn(Schedulers.boundedElastic()) // Aloca as chamadas bloqueantes abaixo para a thread pool correta
        .flatMap(ptResponse -> Mono.fromCallable(() ->
            transactionTemplate.execute(status -> {
              // Localiza o pagamento correspondente na nossa base pelo ID externo do PagTesouro (eager fetch de itens)
              Payment payment = paymentRepository.findByPagtesouroPaymentId(pagtesouroPaymentId)
                  .orElseThrow(() -> new IllegalArgumentException("Pagamento com ID externo " + pagtesouroPaymentId + " não localizado!"));

              // Atualiza o status do pagamento baseado na situação real do governo
              payment.setStatus(ptResponse.situation().code());
              payment.setPaidAt(ptResponse.situation().date());
              payment.setPaymentMethod(ptResponse.typePayment());
              payment.setPspName(ptResponse.paymentServiceProviderName());
              payment.setPspTransactionId(ptResponse.pspTransactionId());

              // Business Rule: Se a transação foi paga com sucesso na STN, liquida granularmente seus itens de serviço
              if (payment.getStatus() == PaymentStatus.COMPLETED) {
                if (payment.getOrderItems() != null && !payment.getOrderItems().isEmpty()) {
                  for (OrderItem item : payment.getOrderItems()) {
                    // Prevenção de ativação tardia: itens previamente cancelados não são liquidados
                    if (item.getStatus() != OrderItemStatus.CANCELLED) {
                      item.setStatus(OrderItemStatus.PAID);
                      item.setPaidAt(payment.getPaidAt());
                    }
                  }
                  orderItemRepository.saveAll(payment.getOrderItems());
                }
                // Dispara a notificação de Server-Sent Events (SSE) para o frontend em tempo real
                paymentNotificationService.notifyPaymentPaid(payment.getId(), "COMPLETED");
              } else if (payment.getStatus() == PaymentStatus.CANCELLED || payment.getStatus() == PaymentStatus.REJECTED) {
                // Se a STN reportou cancelamento ou rejeição (ex: prazo de 2 dias expirado), propaga o cancelamento
                if (payment.getOrderItems() != null && !payment.getOrderItems().isEmpty()) {
                  for (OrderItem item : payment.getOrderItems()) {
                    if (item.getStatus() == OrderItemStatus.PENDING) {
                      item.setStatus(OrderItemStatus.CANCELLED);
                    }
                    Order parentOrder = item.getOrder();
                    if (parentOrder != null && parentOrder.getCancelledAt() == null) {
                      try {
                        parentOrder.cancel();
                        orderRepository.save(parentOrder);
                      } catch (Exception ignored) {
                      }
                    }
                  }
                  orderItemRepository.saveAll(payment.getOrderItems());
                }
              }

              // Persiste as informações de liquidação no Postgres
              return paymentRepository.save(payment);
            })
        ));
  }
}
