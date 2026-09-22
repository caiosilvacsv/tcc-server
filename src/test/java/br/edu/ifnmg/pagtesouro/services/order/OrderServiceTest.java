package br.edu.ifnmg.pagtesouro.services.order;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.OrderItemRepository;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Pedidos (OrderService)")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private br.edu.ifnmg.pagtesouro.repository.UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Order order;
    private OrderRequestDTO orderRequestDTO;

    @BeforeEach
    void setUp() {
        user = new User(
            UUID.randomUUID(),
            "caio@ifnmg.edu.br",
            "Caio",
            "Viana",
            "hashed_password",
            "12345678901",
            br.edu.ifnmg.pagtesouro.domain.user.UserRoles.USER,
            Instant.now(),
            Instant.now()
        );
        
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setTitle("Tíquete Estudante");
        product.setPrice(new BigDecimal("2.50"));
        product.setCodeService("23");
        product.setCategory(ProductCategory.TICKET);
        product.setActive(true);

        orderRequestDTO = new OrderRequestDTO(
            List.of(new OrderRequestDTO.items(product.getId(), 5))
        );

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("12.50"));
        order.setCreateAt(Instant.now());
        order.setOrderItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(5);
        item.setTotalAmount(new BigDecimal("12.50"));
        order.getOrderItems().add(item);
    }

    @Test
    @DisplayName("Deve criar um pedido com sucesso")
    void createOrderSuccess() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponseDTO response = orderService.createOrder(orderRequestDTO, user);

        assertNotNull(response);
        assertEquals(new BigDecimal("12.50"), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals("Tíquete Estudante", response.items().get(0).productTitle());
        verify(productRepository, times(1)).findById(product.getId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar criar pedido com produto inexistente")
    void createOrderProductNotFound() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.empty());

        assertThrows(FindException.class, () -> orderService.createOrder(orderRequestDTO, user));
        verify(productRepository, times(1)).findById(product.getId());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Deve buscar os pedidos de um usuário com sucesso")
    void getUserOrdersSuccess() {
        when(orderRepository.findAllByUser(user)).thenReturn(List.of(order));

        List<OrderResponseDTO> response = orderService.getUserOrders(user);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(order.getId(), response.get(0).id());
        verify(orderRepository, times(1)).findAllByUser(user);
    }

    @Test
    @DisplayName("Deve efetuar a baixa/troca física de um item pago com sucesso")
    void exchangeItemSuccess() {
        OrderItem item = order.getOrderItems().get(0);
        item.setStatus(OrderItemStatus.PAID);
        item.setPaidAt(Instant.now());

        User admin = new User("admin@ifnmg.edu.br", "hashed_pwd", "Admin", "98765432100");

        when(orderItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO.OrderItemResponseDTO response = orderService.exchangeItem(item.getId(), admin);

        assertNotNull(response);
        assertEquals("EXCHANGED", response.status());
        assertEquals(5, response.quantity());
        verify(orderItemRepository, times(1)).findById(item.getId());
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Deve falhar ao tentar trocar um item que não foi pago")
    void exchangeItemNotPaid() {
        OrderItem item = order.getOrderItems().get(0);
        item.setStatus(OrderItemStatus.PENDING); // Ainda não foi pago

        User admin = new User("admin@ifnmg.edu.br", "hashed_pwd", "Admin", "98765432100");

        when(orderItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            orderService.exchangeItem(item.getId(), admin)
        );

        assertTrue(exception.getMessage().contains("não está em estado PAGO"));
        verify(orderItemRepository, times(1)).findById(item.getId());
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Deve falhar ao tentar trocar um item que já foi trocado anteriormente")
    void exchangeItemAlreadyExchanged() {
        OrderItem item = order.getOrderItems().get(0);
        item.setStatus(OrderItemStatus.EXCHANGED); // Já foi consumido

        User admin = new User("admin@ifnmg.edu.br", "hashed_pwd", "Admin", "98765432100");

        when(orderItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            orderService.exchangeItem(item.getId(), admin)
        );

        assertTrue(exception.getMessage().contains("já foi trocado anteriormente"));
        verify(orderItemRepository, times(1)).findById(item.getId());
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Deve buscar tíquetes disponíveis por CPF organizados em fila FIFO (mais antigo primeiro)")
    void getTicketsAvailableByCpfSuccess() {
        OrderItem itemPaid1 = new OrderItem();
        itemPaid1.setId(UUID.randomUUID());
        itemPaid1.setOrder(order);
        itemPaid1.setProduct(product);
        itemPaid1.setQuantity(2);
        itemPaid1.setTotalAmount(new BigDecimal("5.00"));
        itemPaid1.setStatus(OrderItemStatus.PAID);
        itemPaid1.setPaidAt(Instant.now().minusSeconds(3600)); // Pago há 1 hora

        OrderItem itemPaid2 = new OrderItem();
        itemPaid2.setId(UUID.randomUUID());
        itemPaid2.setOrder(order);
        itemPaid2.setProduct(product);
        itemPaid2.setQuantity(1);
        itemPaid2.setTotalAmount(new BigDecimal("2.50"));
        itemPaid2.setStatus(OrderItemStatus.PAID);
        itemPaid2.setPaidAt(Instant.now()); // Pago agora

        OrderItem itemPending = new OrderItem();
        itemPending.setId(UUID.randomUUID());
        itemPending.setOrder(order);
        itemPending.setProduct(product);
        itemPending.setQuantity(1);
        itemPending.setStatus(OrderItemStatus.PENDING); // Não deve aparecer

        order.setOrderItems(new ArrayList<>(List.of(itemPaid2, itemPending, itemPaid1)));

        when(orderRepository.findByGuestCpf(user.getCpf())).thenReturn(List.of());
        when(userRepository.findByCpf(user.getCpf())).thenReturn(user);
        when(orderRepository.findAllByUser(user)).thenReturn(List.of(order));

        var tickets = orderService.getTicketsAvailableByCpf(user.getCpf());

        assertNotNull(tickets);
        assertEquals(2, tickets.size());
        // Deve vir o mais antigo primeiro (itemPaid1 antes de itemPaid2)
        assertEquals(itemPaid1.getId(), tickets.get(0).itemId());
        assertEquals(itemPaid2.getId(), tickets.get(1).itemId());
        assertEquals(2, tickets.get(0).quantity());
        assertEquals(br.edu.ifnmg.pagtesouro.domain.order.BuyerType.STUDENT, tickets.get(0).buyerType());

        verify(orderRepository, times(1)).findByGuestCpf(user.getCpf());
        verify(userRepository, times(1)).findByCpf(user.getCpf());
        verify(orderRepository, times(1)).findAllByUser(user);
    }

    @Test
    @DisplayName("Deve buscar tíquetes de convidado por CPF e Nome com sucesso")
    void getGuestTicketsSuccess() {
        Order guestOrder = new Order();
        guestOrder.setId(UUID.randomUUID());
        guestOrder.setUser(null);
        guestOrder.setGuestCpf("11122233344");
        guestOrder.setGuestName("João Visitante");

        OrderItem guestItem = new OrderItem();
        guestItem.setId(UUID.randomUUID());
        guestItem.setOrder(guestOrder);
        guestItem.setProduct(product);
        guestItem.setQuantity(3);
        guestItem.setTotalAmount(new BigDecimal("7.50"));
        guestItem.setStatus(OrderItemStatus.PAID);
        guestItem.setPaidAt(Instant.now());
        guestOrder.setOrderItems(List.of(guestItem));

        when(orderRepository.findByGuestCpf("11122233344")).thenReturn(List.of(guestOrder));

        var tickets = orderService.getGuestTickets("11122233344", "João");

        assertNotNull(tickets);
        assertEquals(1, tickets.size());
        assertEquals("João Visitante", tickets.get(0).buyerName());
        assertEquals(br.edu.ifnmg.pagtesouro.domain.order.BuyerType.GUEST, tickets.get(0).buyerType());
        verify(orderRepository, times(1)).findByGuestCpf("11122233344");
    }

    @Test
    @DisplayName("Deve falhar ao buscar tíquetes de convidado com nome divergente")
    void getGuestTicketsNameMismatch() {
        Order guestOrder = new Order();
        guestOrder.setId(UUID.randomUUID());
        guestOrder.setUser(null);
        guestOrder.setGuestCpf("11122233344");
        guestOrder.setGuestName("João Visitante");

        when(orderRepository.findByGuestCpf("11122233344")).thenReturn(List.of(guestOrder));

        assertThrows(SecurityException.class, () ->
            orderService.getGuestTickets("11122233344", "Maria")
        );
        verify(orderRepository, times(1)).findByGuestCpf("11122233344");
    }

    @Test
    @DisplayName("Deve efetuar baixa em lote de múltiplos tíquetes de forma atômica com sucesso")
    void exchangeItemsBatchSuccess() {
        OrderItem item1 = new OrderItem();
        item1.setId(UUID.randomUUID());
        item1.setOrder(order);
        item1.setProduct(product);
        item1.setQuantity(1);
        item1.setStatus(OrderItemStatus.PAID);

        OrderItem item2 = new OrderItem();
        item2.setId(UUID.randomUUID());
        item2.setOrder(order);
        item2.setProduct(product);
        item2.setQuantity(2);
        item2.setStatus(OrderItemStatus.PAID);

        User admin = new User("admin@ifnmg.edu.br", "hashed_pwd", "Admin", "98765432100");
        List<UUID> itemIds = List.of(item1.getId(), item2.getId());

        when(orderItemRepository.findAllById(itemIds)).thenReturn(List.of(item1, item2));
        when(orderItemRepository.saveAll(any())).thenReturn(List.of(item1, item2));

        var response = orderService.exchangeItemsBatch(itemIds, admin);

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("EXCHANGED", item1.getStatus().name());
        assertEquals("EXCHANGED", item2.getStatus().name());
        assertNotNull(item1.getExchangedAt());
        assertNotNull(item2.getExchangedAt());
        verify(orderItemRepository, times(1)).findAllById(itemIds);
        verify(orderItemRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Deve falhar e abortar baixa em lote se algum item não estiver pago")
    void exchangeItemsBatchFailureWhenNotPaid() {
        OrderItem itemPaid = new OrderItem();
        itemPaid.setId(UUID.randomUUID());
        itemPaid.setOrder(order);
        itemPaid.setProduct(product);
        itemPaid.setStatus(OrderItemStatus.PAID);

        OrderItem itemPending = new OrderItem();
        itemPending.setId(UUID.randomUUID());
        itemPending.setOrder(order);
        itemPending.setProduct(product);
        itemPending.setStatus(OrderItemStatus.PENDING); // Não pago

        User admin = new User("admin@ifnmg.edu.br", "hashed_pwd", "Admin", "98765432100");
        List<UUID> itemIds = List.of(itemPaid.getId(), itemPending.getId());

        when(orderItemRepository.findAllById(itemIds)).thenReturn(List.of(itemPaid, itemPending));

        assertThrows(IllegalStateException.class, () ->
            orderService.exchangeItemsBatch(itemIds, admin)
        );

        verify(orderItemRepository, times(1)).findAllById(itemIds);
        verify(orderItemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve cancelar pedido pendente com sucesso pelo próprio aluno")
    void cancelOrderSuccess() {
        UUID orderId = UUID.randomUUID();
        OrderItem pendingItem = new OrderItem();
        pendingItem.setId(UUID.randomUUID());
        pendingItem.setStatus(OrderItemStatus.PENDING);
        pendingItem.setProduct(product);
        pendingItem.setTotalAmount(new BigDecimal("3.50"));
        pendingItem.setQuantity(1);

        Order studentOrder = new Order();
        studentOrder.setId(orderId);
        studentOrder.setUser(user);
        studentOrder.setOrderItems(List.of(pendingItem));
        studentOrder.setTotalAmount(new BigDecimal("3.50"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(studentOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.cancelOrder(orderId, user);

        assertNotNull(response);
        assertEquals("CANCELLED", response.status().name());
        assertNotNull(studentOrder.getCancelledAt());
        assertEquals(OrderItemStatus.CANCELLED, pendingItem.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, times(1)).save(studentOrder);
    }

    @Test
    @DisplayName("Deve impedir cancelamento de pedido de outro usuário com SecurityException")
    void cancelOrderForbidden() {
        UUID orderId = UUID.randomUUID();
        User anotherUser = new User(
            UUID.randomUUID(),
            "outro@ifnmg.edu.br",
            "Outro",
            "Aluno",
            "hash",
            "99988877766",
            br.edu.ifnmg.pagtesouro.domain.user.UserRoles.USER,
            Instant.now(),
            Instant.now()
        );

        Order otherOrder = new Order();
        otherOrder.setId(orderId);
        otherOrder.setUser(anotherUser);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(otherOrder));

        SecurityException ex = assertThrows(SecurityException.class, () ->
            orderService.cancelOrder(orderId, user)
        );

        assertEquals("Acesso negado: você só pode cancelar os seus próprios pedidos.", ex.getMessage());
        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao tentar cancelar pedido inexistente com FindException")
    void cancelOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(FindException.class, () ->
            orderService.cancelOrder(orderId, user)
        );

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar ao tentar cancelar pedido que já possui itens pagos")
    void cancelOrderAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        OrderItem paidItem = new OrderItem();
        paidItem.setStatus(OrderItemStatus.PAID);
        paidItem.setProduct(product);

        Order paidOrder = new Order();
        paidOrder.setId(orderId);
        paidOrder.setUser(user);
        paidOrder.setOrderItems(List.of(paidItem));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(paidOrder));

        assertThrows(IllegalStateException.class, () ->
            orderService.cancelOrder(orderId, user)
        );

        verify(orderRepository, times(1)).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve executar cancelamento de pedidos abandonados elegíveis pelo Daemon")
    void cancelAbandonedOrdersSuccess() {
        OrderItem item1 = new OrderItem();
        item1.setStatus(OrderItemStatus.PENDING);
        item1.setProduct(product);

        Order abandonedOrder = new Order();
        abandonedOrder.setId(UUID.randomUUID());
        abandonedOrder.setUser(user);
        abandonedOrder.setOrderItems(List.of(item1));

        when(orderRepository.findByCancelledAtIsNullAndCreateAtBefore(any(Instant.class)))
            .thenReturn(List.of(abandonedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int cancelled = orderService.cancelAbandonedOrders(10);

        assertEquals(1, cancelled);
        assertNotNull(abandonedOrder.getCancelledAt());
        assertEquals(OrderItemStatus.CANCELLED, item1.getStatus());
        verify(orderRepository, times(1)).findByCancelledAtIsNullAndCreateAtBefore(any(Instant.class));
        verify(orderRepository, times(1)).save(abandonedOrder);
    }
}
