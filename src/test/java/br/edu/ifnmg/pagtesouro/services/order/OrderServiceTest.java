package br.edu.ifnmg.pagtesouro.services.order;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
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

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Order order;
    private OrderRequestDTO orderRequestDTO;

    @BeforeEach
    void setUp() {
        user = new User("caio@ifnmg.edu.br", "hashed_password", "Caio", "12345678901");
        
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setTitle("Tíquete Estudante");
        product.setPrice(new BigDecimal("2.50"));
        product.setCodeService("23");
        product.setCategory(ProductCategory.TICKET);

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
}
