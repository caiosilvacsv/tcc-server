package br.edu.ifnmg.pagtesouro.domain.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Status de Pagamento (PaymentStatus)")
class PaymentStatusTest {

    @Test
    @DisplayName("Deve converter string correspondente para o Enum PaymentStatus de forma segura")
    void fromStatus_Success() {
        assertEquals(PaymentStatus.CREATED, PaymentStatus.fromStatus("CRIADO"));
        assertEquals(PaymentStatus.STARTED, PaymentStatus.fromStatus("INICIADO"));
        assertEquals(PaymentStatus.SUBMITTED, PaymentStatus.fromStatus("SUBMETIDO"));
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.fromStatus("CONCLUIDO"));
        assertEquals(PaymentStatus.REJECTED, PaymentStatus.fromStatus("REJEITADO"));
        assertEquals(PaymentStatus.CANCELLED, PaymentStatus.fromStatus("CANCELADO"));
        
        // Com letras minúsculas e espaços
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.fromStatus("  concluido  "));
        assertEquals(PaymentStatus.CREATED, PaymentStatus.fromStatus("criado"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valores nulos ou inválidos")
    void fromStatus_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.fromStatus(null));
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.fromStatus("invalido"));
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.fromStatus(""));
    }
}
