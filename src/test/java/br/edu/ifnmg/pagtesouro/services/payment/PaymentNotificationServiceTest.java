package br.edu.ifnmg.pagtesouro.services.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da Infraestrutura de Server-Sent Events (PaymentNotificationService)")
class PaymentNotificationServiceTest {

    private PaymentNotificationService notificationService;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        notificationService = new PaymentNotificationService();
        paymentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve registrar emissor SSE com sucesso e inicializar com evento CONNECT")
    void registerEmitterSuccess() {
        SseEmitter emitter = notificationService.registerEmitter(paymentId);

        assertNotNull(emitter);
        assertEquals(1, notificationService.getActiveConnectionsCount());
    }

    @Test
    @DisplayName("Deve disparar evento de pagamento pago e remover emissor do mapa de controle")
    void notifyPaymentPaidSuccess() {
        notificationService.registerEmitter(paymentId);
        assertEquals(1, notificationService.getActiveConnectionsCount());

        notificationService.notifyPaymentPaid(paymentId, "COMPLETED");

        // Após disparar a notificação de encerramento do ciclo, a conexão é removida do mapa para evitar Memory Leak
        assertEquals(0, notificationService.getActiveConnectionsCount());
    }

    @Test
    @DisplayName("Deve ignorar chamadas de notificação para IDs inexistentes ou expirações anteriores")
    void notifyPaymentPaidNonExistent() {
        notificationService.notifyPaymentPaid(UUID.randomUUID(), "COMPLETED");
        assertEquals(0, notificationService.getActiveConnectionsCount());
    }
}
