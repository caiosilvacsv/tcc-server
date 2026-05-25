package br.edu.ifnmg.pagtesouro.domain.order;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Modelo de Domínio Rico - Pedido (Order)")
class OrderTest {

    private Order order;
    private List<OrderItem> items;

    @BeforeEach
    void setUp() {
        order = new Order();
        items = new ArrayList<>();
        order.setOrderItems(items);
        order.setTotalAmount(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar CREATED quando o pedido não possui itens")
    void getStatus_NoItems() {
        order.setOrderItems(null);
        assertEquals(OrderStatus.CREATED, order.getStatus());

        order.setOrderItems(new ArrayList<>());
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    @DisplayName("Deve retornar PENDING_PAYMENT quando todos os itens estão pendentes")
    void getStatus_AllPending() {
        OrderItem item1 = new OrderItem();
        item1.setStatus(OrderItemStatus.PENDING);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setStatus(OrderItemStatus.PENDING);
        items.add(item2);

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    @Test
    @DisplayName("Deve retornar COMPLETED quando todos os itens estão pagos")
    void getStatus_AllPaid() {
        OrderItem item1 = new OrderItem();
        item1.setStatus(OrderItemStatus.PAID);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setStatus(OrderItemStatus.PAID);
        items.add(item2);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    @DisplayName("Deve retornar CANCELLED quando todos os itens estão cancelados")
    void getStatus_AllCancelled() {
        OrderItem item1 = new OrderItem();
        item1.setStatus(OrderItemStatus.CANCELLED);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setStatus(OrderItemStatus.CANCELLED);
        items.add(item2);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("Deve retornar PARTIALLY_PAID quando há itens pagos e itens pendentes")
    void getStatus_PartiallyPaid() {
        OrderItem item1 = new OrderItem();
        item1.setStatus(OrderItemStatus.PAID);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setStatus(OrderItemStatus.PENDING);
        items.add(item2);

        assertEquals(OrderStatus.PARTIALLY_PAID, order.getStatus());
    }

    @Test
    @DisplayName("Deve retornar PENDING_PAYMENT se houver itens pendentes e cancelados, mas nenhum pago")
    void getStatus_PendingAndCancelled() {
        OrderItem item1 = new OrderItem();
        item1.setStatus(OrderItemStatus.PENDING);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        item2.setStatus(OrderItemStatus.CANCELLED);
        items.add(item2);

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }
}
