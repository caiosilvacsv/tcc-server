package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Pagamentos (PaymentService)")
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PagTesouroClient pagTesouroClient;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Order order;
    private Payment payment;
    private CheckoutRequestDTO checkoutRequestDTO;
    private PagTesouroResponseDTO ptResponse;

    @BeforeEach
    void setUp() {
        user = new User("caio@ifnmg.edu.br", "hashed_password", "Caio", "12345678901");
        
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setTitle("Tíquete Estudante");
        product.setPrice(new BigDecimal("2.50"));
        product.setCodeService("23");

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("2.50"));
        order.setOrderItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setTotalAmount(new BigDecimal("2.50"));
        order.getOrderItems().add(item);

        checkoutRequestDTO = new CheckoutRequestDTO(order.getId());

        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("2.50"));
        payment.setStatus(PaymentStatus.CREATED);
        payment.setContributorName("Caio");
        payment.setContributorCpfCnpj("12345678901");
        payment.setOrderItems(order.getOrderItems());
        payment.setExpiredAt(LocalDate.now().plusDays(2));
        payment.setCompetence(202605);

        ptResponse = new PagTesouroResponseDTO(
            "pt-payment-id-123",
            Instant.now(),
            "https://valpagtesouro.tesouro.gov.br/iframe-url",
            new PagTesouroResponseDTO.Situacao(PaymentStatus.CREATED)
        );
    }

    @Test
    @DisplayName("Deve iniciar checkout com sucesso gerando a URL do PagTesouro")
    void checkoutSuccess() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(pagTesouroClient.createPayment(any())).thenReturn(Mono.just(ptResponse));

        Mono<CheckoutResponseDTO> result = paymentService.checkout(checkoutRequestDTO, "127.0.0.1");

        CheckoutResponseDTO response = result.block();

        assertNotNull(response);
        assertEquals("pt-payment-id-123", response.pagtesouroPaymentId());
        assertEquals("https://valpagtesouro.tesouro.gov.br/iframe-url", response.nextUrl());
        assertEquals(new BigDecimal("2.50"), response.amount());

        verify(orderRepository, times(1)).findById(order.getId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(pagTesouroClient, times(1)).createPayment(any());
    }
}
