package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentMethod;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroQueryResponseDTO;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Sincronização de Pagamentos (PaymentSyncService)")
class PaymentSyncServiceTest {

    @Mock
    private PagTesouroClient pagTesouroClient;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentSyncService paymentSyncService;

    private Payment payment;
    private OrderItem orderItem;
    private PagTesouroQueryResponseDTO queryResponse;

    @BeforeEach
    void setUp() {
        orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setQuantity(1);
        orderItem.setTotalAmount(new BigDecimal("2.50"));
        orderItem.setStatus(OrderItemStatus.PENDING);

        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPagtesouroPaymentId("pt-payment-123");
        payment.setAmount(new BigDecimal("2.50"));
        payment.setStatus(PaymentStatus.CREATED);
        payment.setOrderItems(List.of(orderItem));

        queryResponse = new PagTesouroQueryResponseDTO(
            "pt-payment-123",
            PaymentMethod.PIX,
            new BigDecimal("2.50"),
            "Simulador PSP",
            "psp-tx-abc",
            new PagTesouroQueryResponseDTO.Situation(PaymentStatus.COMPLETED, Instant.now())
        );
    }

    @Test
    @DisplayName("Deve sincronizar status de pagamento COMPLETED e liquidar itens de pedido")
    void syncStatusCompleted() {
        when(pagTesouroClient.getPaymentStatus("pt-payment-123")).thenReturn(Mono.just(queryResponse));
        when(paymentRepository.findByPagtesouroPaymentId("pt-payment-123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        Mono<Payment> result = paymentSyncService.syncPaymentStatus("pt-payment-123");
        Payment updatedPayment = result.block();

        assertNotNull(updatedPayment);
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(PaymentMethod.PIX, payment.getPaymentMethod());
        assertEquals("Simulador PSP", payment.getPspName());
        assertEquals("psp-tx-abc", payment.getPspTransactionId());
        assertNotNull(payment.getPaidAt());

        // Verificação da liberação/quitação dos itens de pedido
        assertEquals(OrderItemStatus.PAID, orderItem.getStatus());
        assertNotNull(orderItem.getPaidAt());

        verify(pagTesouroClient, times(1)).getPaymentStatus("pt-payment-123");
        verify(paymentRepository, times(1)).findByPagtesouroPaymentId("pt-payment-123");
        verify(paymentRepository, times(1)).save(payment);
    }
}
