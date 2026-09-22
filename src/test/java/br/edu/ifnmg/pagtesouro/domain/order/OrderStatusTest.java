package br.edu.ifnmg.pagtesouro.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Status do Pedido (OrderStatus)")
class OrderStatusTest {

    @Test
    @DisplayName("Deve converter string correspondente para o Enum OrderStatus de forma segura")
    void fromStatus_Success() {
        assertEquals(OrderStatus.CREATED, OrderStatus.fromStatus("criado"));
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.fromStatus("pagamento pendente"));
        assertEquals(OrderStatus.PARTIALLY_PAID, OrderStatus.fromStatus("parcialmente pago"));
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromStatus("concluido"));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromStatus("cancelado"));
        
        // Com letras maiúsculas e espaços
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromStatus("  CONCLUIDO  "));
        assertEquals(OrderStatus.PARTIALLY_PAID, OrderStatus.fromStatus("PARTIALLY_PAID"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valores nulos ou inválidos")
    void fromStatus_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromStatus(null));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromStatus("invalido"));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromStatus(""));
    }
}
