package br.edu.ifnmg.pagtesouro.domain.orderItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Status do Item do Pedido (OrderItemStatus)")
class OrderItemStatusTest {

    @Test
    @DisplayName("Deve converter string correspondente para o Enum OrderItemStatus de forma segura")
    void fromStatus_Success() {
        assertEquals(OrderItemStatus.PENDING, OrderItemStatus.fromStatus("PENDENTE"));
        assertEquals(OrderItemStatus.PAID, OrderItemStatus.fromStatus("PAGO"));
        assertEquals(OrderItemStatus.CANCELLED, OrderItemStatus.fromStatus("CANCELADO"));
        
        // Com letras minúsculas e espaços
        assertEquals(OrderItemStatus.PAID, OrderItemStatus.fromStatus("  pago  "));
        assertEquals(OrderItemStatus.PENDING, OrderItemStatus.fromStatus("PENDING"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valores nulos ou inválidos")
    void fromStatus_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> OrderItemStatus.fromStatus(null));
        assertThrows(IllegalArgumentException.class, () -> OrderItemStatus.fromStatus("invalido"));
        assertThrows(IllegalArgumentException.class, () -> OrderItemStatus.fromStatus(""));
    }
}
