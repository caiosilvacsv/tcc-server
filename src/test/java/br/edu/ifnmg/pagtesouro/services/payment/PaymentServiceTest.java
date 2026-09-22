package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.DirectCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.OrderCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.common.PageResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.PaymentHistoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroProperties;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Pagamentos (PaymentService)")
class PaymentServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PagTesouroClient pagTesouroClient;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Product product;
    private Order order;
    private Payment payment;
    private PagTesouroProperties properties;
    private PagTesouroResponseDTO ptResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        user = new User(
            userId,
            "caio@ifnmg.edu.br",
            "Caio",
            "hashed_password",
            "12345678901",
            br.edu.ifnmg.pagtesouro.domain.user.UserRoles.USER,
            java.time.Instant.now(),
            java.time.Instant.now()
        );

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setTitle("Tíquete Estudante");
        product.setPrice(new BigDecimal("2.50"));
        product.setCodeService("23");
        product.setCategory(ProductCategory.TICKET);
        product.setActive(true);

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
        item.setStatus(OrderItemStatus.PENDING);
        order.getOrderItems().add(item);

        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("2.50"));
        payment.setStatus(PaymentStatus.CREATED);
        payment.setContributorName("Caio Viana");
        payment.setContributorCpfCnpj("12345678901");
        payment.setOrderItems(order.getOrderItems());
        payment.setExpiredAt(LocalDate.now().plusDays(2));
        payment.setCompetence("202605");
        payment.setReferenceNumber(100200L);

        properties = new PagTesouroProperties(
            "https://valpagtesouro.tesouro.gov.br",
            "token_123",
            "http://localhost:8080/payment/webhook"
        );

        ptResponse = new PagTesouroResponseDTO(
            "pt-payment-id-123",
            Instant.now(),
            "https://valpagtesouro.tesouro.gov.br/iframe-url",
            new PagTesouroResponseDTO.Situacao(PaymentStatus.CREATED)
        );
    }

    @Test
    @DisplayName("Deve iniciar checkout direto com sucesso gerando a URL de redirecionamento")
    void checkoutDirectSuccess() {
        DirectCheckoutRequestDTO request = new DirectCheckoutRequestDTO(
            product.getId(),
            1,
            "12345678901",
            "Caio Viana",
            false
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(payment);
        when(pagTesouroClient.getProperties()).thenReturn(properties);
        when(pagTesouroClient.createPayment(any())).thenReturn(Mono.just(ptResponse));

        CheckoutResponseDTO response = paymentService.checkoutDirect(request);

        assertNotNull(response);
        assertEquals("pt-payment-id-123", response.pagtesouroPaymentId());
        assertEquals("https://valpagtesouro.tesouro.gov.br/iframe-url", response.nextUrl());
        assertEquals(new BigDecimal("2.50"), response.amount());

        verify(productRepository, times(1)).findById(product.getId());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(paymentRepository, times(1)).saveAndFlush(any(Payment.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(pagTesouroClient, times(1)).createPayment(any());
    }

    @Test
    @DisplayName("Deve iniciar checkout de pedido com sucesso gerando a URL de redirecionamento")
    void checkoutOrderSuccess() {
        OrderCheckoutRequestDTO request = new OrderCheckoutRequestDTO(
            "12345678901",
            "Caio Viana",
            false
        );

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(payment);
        when(pagTesouroClient.getProperties()).thenReturn(properties);
        when(pagTesouroClient.createPayment(any())).thenReturn(Mono.just(ptResponse));

        List<CheckoutResponseDTO> responses = paymentService.checkoutOrder(order.getId(), request, user);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        CheckoutResponseDTO response = responses.get(0);
        assertEquals("pt-payment-id-123", response.pagtesouroPaymentId());
        assertEquals("https://valpagtesouro.tesouro.gov.br/iframe-url", response.nextUrl());
        assertEquals("23", response.codeService());

        verify(orderRepository, times(1)).findById(order.getId());
        verify(paymentRepository, times(1)).saveAndFlush(any(Payment.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(pagTesouroClient, times(1)).createPayment(any());
    }

    @Test
    @DisplayName("Deve agrupar itens por código SISGRU e gerar múltiplos pagamentos quando o pedido tiver múltiplos serviços")
    void checkoutOrderWithMultipleServiceCodesSuccess() {
        // Criando segundo produto com outro código de serviço
        Product product2 = new Product();
        product2.setId(UUID.randomUUID());
        product2.setTitle("Taxa de Biblioteca");
        product2.setPrice(new BigDecimal("10.00"));
        product2.setCodeService("88");
        product2.setCategory(ProductCategory.FINE);
        product2.setActive(true);

        OrderItem item2 = new OrderItem();
        item2.setId(UUID.randomUUID());
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setTotalAmount(new BigDecimal("10.00"));
        item2.setStatus(OrderItemStatus.PENDING);
        order.getOrderItems().add(item2);

        OrderCheckoutRequestDTO request = new OrderCheckoutRequestDTO(false);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setReferenceNumber(100200L);
            return p;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pagTesouroClient.getProperties()).thenReturn(properties);
        when(pagTesouroClient.createPayment(any())).thenReturn(Mono.just(ptResponse));

        List<CheckoutResponseDTO> responses = paymentService.checkoutOrder(order.getId(), request, user);

        assertNotNull(responses);
        assertEquals(2, responses.size());

        List<String> serviceCodes = responses.stream().map(CheckoutResponseDTO::codeService).toList();
        assertTrue(serviceCodes.contains("23"));
        assertTrue(serviceCodes.contains("88"));

        verify(orderRepository, times(1)).findById(order.getId());
        verify(paymentRepository, times(2)).saveAndFlush(any(Payment.class));
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(pagTesouroClient, times(2)).createPayment(any());
    }

    @Test
    @DisplayName("Deve buscar o histórico paginado de pagamentos do estudante com sucesso")
    void getMyPaymentsSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(java.util.List.of(payment));

        when(paymentRepository.findByContributorCpfCnpj(user.getCpf(), pageable)).thenReturn(page);

        PageResponseDTO<PaymentHistoryResponseDTO> response = paymentService.getMyPayments(user, pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(0, response.page());
        assertEquals(1, response.totalPages());
        assertTrue(response.first());
        assertTrue(response.last());
        assertEquals(payment.getId(), response.content().get(0).id());
        assertEquals(payment.getAmount(), response.content().get(0).amount());

        verify(paymentRepository, times(1)).findByContributorCpfCnpj(user.getCpf(), pageable);
    }
}
