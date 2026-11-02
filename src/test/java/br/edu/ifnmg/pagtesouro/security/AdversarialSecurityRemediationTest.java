package br.edu.ifnmg.pagtesouro.security;

import br.edu.ifnmg.pagtesouro.controllers.OrderController;
import br.edu.ifnmg.pagtesouro.domain.order.BuyerType;
import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentMethod;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.PaymentHistoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.user.CpfValidator;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.OrderItemRepository;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import br.edu.ifnmg.pagtesouro.services.order.OrderService;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Suite de Testes Adversariais Empíricos para Validação das Remediações de Segurança (Milestone 1).
 * <p>
 * Abrange:
 * 1. Mitigação e Fechamento Rigoroso de IDOR em OrderService e OrderController (getItemById).
 * 2. Segurança de Consulta de Tíquetes por CPF (getAvailableTickets) e Desativação da Rota Guest (/orders/guest).
 * 3. Validação Adversarial do Mascaramento Universal LGPD em CpfValidator e nos DTOs de Resposta.
 * 4. Escopamento de Consulta de Histórico de Pagamentos ao ID do Usuário Autenticado (getMyPayments).
 * </p>
 */
@DisplayName("Testes Adversariais de Segurança — Remediações Milestone 1")
class AdversarialSecurityRemediationTest {

    // =========================================================================
    // 1. ADVERSARIAL IDOR VERIFICATION
    // =========================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("1. Testes Adversariais de IDOR (OrderService & OrderController)")
    class IdorAdversarialTests {

        @Mock
        private OrderRepository orderRepository;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private OrderItemRepository orderItemRepository;
        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private OrderService orderService;

        private User victimUserA;
        private User attackerUserB;
        private User adminUser;
        private Product product;
        private Order orderA;
        private OrderItem itemA;
        private UUID itemIdA;

        @BeforeEach
        void setUp() {
            victimUserA = new User(
                UUID.randomUUID(),
                "victim@ifnmg.edu.br",
                "Vitima",
                "A",
                "hashA",
                "11144477735",
                UserRoles.USER,
                Instant.now(),
                Instant.now()
            );

            attackerUserB = new User(
                UUID.randomUUID(),
                "attacker@ifnmg.edu.br",
                "Atacante",
                "B",
                "hashB",
                "52998224725",
                UserRoles.USER,
                Instant.now(),
                Instant.now()
            );

            adminUser = new User(
                UUID.randomUUID(),
                "admin@ifnmg.edu.br",
                "Administrador",
                "IFNMG",
                "hashAdmin",
                "99988877766",
                UserRoles.ADMIN,
                Instant.now(),
                Instant.now()
            );

            product = new Product();
            product.setId(UUID.randomUUID());
            product.setTitle("Refeição Universitária");
            product.setPrice(new BigDecimal("3.50"));
            product.setActive(true);

            orderA = new Order();
            orderA.setId(UUID.randomUUID());
            orderA.setUser(victimUserA);
            orderA.setTotalAmount(new BigDecimal("3.50"));
            orderA.setCreateAt(Instant.now());
            orderA.setOrderItems(new ArrayList<>());

            itemIdA = UUID.randomUUID();
            itemA = new OrderItem();
            itemA.setId(itemIdA);
            itemA.setOrder(orderA);
            itemA.setProduct(product);
            itemA.setQuantity(1);
            itemA.setTotalAmount(new BigDecimal("3.50"));
            itemA.setStatus(OrderItemStatus.PAID);
            itemA.setPaidAt(Instant.now());
            orderA.getOrderItems().add(itemA);
        }

        @Test
        @DisplayName("IDOR: Usuário A (proprietário) DEVE conseguir acessar seu próprio item de pedido")
        void ownerCanAccessOwnItem() {
            when(orderItemRepository.findById(itemIdA)).thenReturn(Optional.of(itemA));

            TicketReadyResponseDTO response = orderService.getItemById(itemIdA, victimUserA);

            assertNotNull(response);
            assertEquals(itemIdA, response.itemId());
            assertEquals("Vitima A", response.buyerName());
            assertEquals("111.***.***-35", response.buyerCpf());
            verify(orderItemRepository, times(1)).findById(itemIdA);
        }

        @Test
        @DisplayName("IDOR: Usuário B (não-admin) NÃO DEVE conseguir acessar item de Usuário A (lança SecurityException)")
        void attackerCannotAccessOtherUserItem() {
            when(orderItemRepository.findById(itemIdA)).thenReturn(Optional.of(itemA));

            SecurityException secEx = assertThrows(
                SecurityException.class,
                () -> orderService.getItemById(itemIdA, attackerUserB)
            );

            assertTrue(secEx.getMessage().toLowerCase().contains("acesso negado"));
            verify(orderItemRepository, times(1)).findById(itemIdA);
        }

        @Test
        @DisplayName("IDOR: Usuário ADMIN DEVE conseguir acessar item de Usuário A no balcão de atendimento")
        void adminCanAccessAnyUserItem() {
            when(orderItemRepository.findById(itemIdA)).thenReturn(Optional.of(itemA));

            TicketReadyResponseDTO response = orderService.getItemById(itemIdA, adminUser);

            assertNotNull(response);
            assertEquals(itemIdA, response.itemId());
            assertEquals("Vitima A", response.buyerName());
            verify(orderItemRepository, times(1)).findById(itemIdA);
        }

        @Test
        @DisplayName("IDOR Edge-case: Item de pedido Guest (sem usuário registrado) é bloqueado para estudante comum")
        void guestItemForbiddenForRegularStudent() {
            Order guestOrder = new Order();
            guestOrder.setId(UUID.randomUUID());
            guestOrder.setUser(null); // Pedido de visitante sem cadastro
            guestOrder.setGuestName("Visitante Desconhecido");
            guestOrder.setGuestCpf("11122233344");

            OrderItem guestItem = new OrderItem();
            guestItem.setId(UUID.randomUUID());
            guestItem.setOrder(guestOrder);
            guestItem.setProduct(product);
            guestItem.setStatus(OrderItemStatus.PAID);

            when(orderItemRepository.findById(guestItem.getId())).thenReturn(Optional.of(guestItem));

            assertThrows(
                SecurityException.class,
                () -> orderService.getItemById(guestItem.getId(), attackerUserB)
            );
        }

        @Test
        @DisplayName("IDOR Edge-case: Item de pedido Guest PODE ser consultado por ADMIN")
        void guestItemAllowedForAdmin() {
            Order guestOrder = new Order();
            guestOrder.setId(UUID.randomUUID());
            guestOrder.setUser(null);
            guestOrder.setGuestName("Visitante Desconhecido");
            guestOrder.setGuestCpf("11122233344");

            OrderItem guestItem = new OrderItem();
            guestItem.setId(UUID.randomUUID());
            guestItem.setOrder(guestOrder);
            guestItem.setProduct(product);
            guestItem.setStatus(OrderItemStatus.PAID);

            when(orderItemRepository.findById(guestItem.getId())).thenReturn(Optional.of(guestItem));

            TicketReadyResponseDTO response = orderService.getItemById(guestItem.getId(), adminUser);
            assertNotNull(response);
            assertEquals(guestItem.getId(), response.itemId());
        }

        @Test
        @DisplayName("IDOR Edge-case: Solicitação com usuário nulo é sumariamente bloqueada com SecurityException")
        void nullUserThrowsSecurityException() {
            when(orderItemRepository.findById(itemIdA)).thenReturn(Optional.of(itemA));

            assertThrows(
                SecurityException.class,
                () -> orderService.getItemById(itemIdA, null)
            );
        }

        @Test
        @DisplayName("IDOR Controller: OrderController repassa User e propaga SecurityException como HTTP 403")
        void orderControllerForwardsPrincipalAndProtects() {
            OrderService mockService = mock(OrderService.class);
            OrderController controller = new OrderController(mockService);

            // Chamada legítima do dono
            TicketReadyResponseDTO mockTicket = new TicketReadyResponseDTO(
                itemIdA, orderA.getId(), "Refeição", 1, new BigDecimal("3.50"),
                "Vitima A", "11144477735", BuyerType.STUDENT, Instant.now()
            );
            when(mockService.getItemById(itemIdA, victimUserA)).thenReturn(mockTicket);
            ResponseEntity<TicketReadyResponseDTO> okResp = controller.getItemById(itemIdA, victimUserA);
            assertEquals(HttpStatus.OK, okResp.getStatusCode());
            assertEquals(itemIdA, okResp.getBody().itemId());

            // Chamada maliciosa do atacante
            when(mockService.getItemById(itemIdA, attackerUserB))
                .thenThrow(new SecurityException("Acesso negado"));
            assertThrows(
                SecurityException.class,
                () -> controller.getItemById(itemIdA, attackerUserB)
            );
        }
    }

    // =========================================================================
    // 2. ADVERSARIAL TICKET QUERYING & GUEST ROUTE TESTS
    // =========================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("2. Testes Adversariais de Consulta de Tíquetes e Rota Guest")
    class TicketQueryingAdversarialTests {

        @Mock
        private OrderService orderService;

        @InjectMocks
        private OrderController orderController;

        private User regularUser;
        private User adminUser;

        @BeforeEach
        void setUp() {
            regularUser = new User(
                UUID.randomUUID(),
                "aluno@ifnmg.edu.br",
                "Aluno",
                "Federal",
                "hash",
                "11144477735", // Raw 11-digit
                UserRoles.USER,
                Instant.now(),
                Instant.now()
            );

            adminUser = new User(
                UUID.randomUUID(),
                "admin@ifnmg.edu.br",
                "Admin",
                "Federal",
                "hash",
                "99988877766",
                UserRoles.ADMIN,
                Instant.now(),
                Instant.now()
            );
        }

        @Test
        @DisplayName("Consulta de Tíquetes: Usuário com CPF formatado ('111.444.777-35') retorna 200 OK")
        void selfQueryFormattedCpfReturns200() {
            when(orderService.getTicketsAvailableByCpf("111.444.777-35")).thenReturn(List.of());

            ResponseEntity<List<TicketReadyResponseDTO>> resp =
                orderController.getAvailableTickets("111.444.777-35", regularUser);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(orderService, times(1)).getTicketsAvailableByCpf("111.444.777-35");
        }

        @Test
        @DisplayName("Consulta de Tíquetes: Usuário com CPF sem pontuação ('11144477735') retorna 200 OK")
        void selfQueryRawCpfReturns200() {
            when(orderService.getTicketsAvailableByCpf("11144477735")).thenReturn(List.of());

            ResponseEntity<List<TicketReadyResponseDTO>> resp =
                orderController.getAvailableTickets("11144477735", regularUser);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(orderService, times(1)).getTicketsAvailableByCpf("11144477735");
        }

        @Test
        @DisplayName("Consulta de Tíquetes: CPF com espaços periféricos (' 111.444.777-35 ') é normalizado e retorna 200 OK")
        void selfQueryCpfWithSpacesReturns200() {
            when(orderService.getTicketsAvailableByCpf(" 111.444.777-35 ")).thenReturn(List.of());

            ResponseEntity<List<TicketReadyResponseDTO>> resp =
                orderController.getAvailableTickets(" 111.444.777-35 ", regularUser);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(orderService, times(1)).getTicketsAvailableByCpf(" 111.444.777-35 ");
        }

        @Test
        @DisplayName("Consulta de Tíquetes: Sem parâmetro cpf (null ou vazio) faz fallback seguro para o CPF do usuário")
        void queryOmittedCpfFallsBackToUserCpf() {
            when(orderService.getTicketsAvailableByCpf(regularUser.getCpf())).thenReturn(List.of());

            ResponseEntity<List<TicketReadyResponseDTO>> respNull =
                orderController.getAvailableTickets(null, regularUser);
            assertEquals(HttpStatus.OK, respNull.getStatusCode());

            ResponseEntity<List<TicketReadyResponseDTO>> respEmpty =
                orderController.getAvailableTickets("   ", regularUser);
            assertEquals(HttpStatus.OK, respEmpty.getStatusCode());

            verify(orderService, times(2)).getTicketsAvailableByCpf(regularUser.getCpf());
        }

        @Test
        @DisplayName("Consulta de Tíquetes: Aluno comum consultando CPF de terceiro DEVE ser rejeitado com SecurityException (403)")
        void queryOtherUserCpfForbiddenForStudent() {
            String thirdPartyCpf = "52998224725";

            SecurityException secEx = assertThrows(
                SecurityException.class,
                () -> orderController.getAvailableTickets(thirdPartyCpf, regularUser)
            );

            assertTrue(secEx.getMessage().contains("apenas administradores"));
            verify(orderService, never()).getTicketsAvailableByCpf(any());
        }

        @Test
        @DisplayName("Consulta de Tíquetes: Administrador consultando CPF de terceiro DEVE ser autorizado com 200 OK")
        void queryOtherUserCpfAllowedForAdmin() {
            String thirdPartyCpf = "52998224725";
            when(orderService.getTicketsAvailableByCpf(thirdPartyCpf)).thenReturn(List.of());

            ResponseEntity<List<TicketReadyResponseDTO>> resp =
                orderController.getAvailableTickets(thirdPartyCpf, adminUser);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            verify(orderService, times(1)).getTicketsAvailableByCpf(thirdPartyCpf);
        }

        @Test
        @DisplayName("Rota Guest: Invocação de /orders/guest DEVE SEMPRE lançar SecurityException para qualquer usuário")
        void guestRouteAlwaysBlocked() {
            SecurityException ex = assertThrows(
                SecurityException.class,
                () -> orderController.getGuestTickets("11144477735", "Visitante")
            );

            assertTrue(ex.getMessage().contains("desativada por motivos de segurança e LGPD"));
            verifyNoInteractions(orderService);
        }
    }

    // =========================================================================
    // 3. ADVERSARIAL CPF MASKING & DTO LGPD LEAKAGE TESTS
    // =========================================================================
    @Nested
    @DisplayName("3. Testes Adversariais de Mascaramento LGPD e Prevenção de Vazamento de PII")
    class CpfMaskingAdversarialTests {

        @Test
        @DisplayName("CpfValidator.mask: Entradas nulas e em branco devem retornar null sem lançar exceções")
        void maskNullOrBlankSafe() {
            assertNull(CpfValidator.mask(null));
            assertNull(CpfValidator.mask(""));
            assertNull(CpfValidator.mask("   "));
            assertNull(CpfValidator.mask("\t\n\r"));
        }

        @Test
        @DisplayName("CpfValidator.mask: Entradas curtas ('1', '12', '123', '1234567890') retornam máscara genérica segura sem estourar limites")
        void maskShortInputsSafe() {
            assertEquals("***.***.***-**", CpfValidator.mask("1"));
            assertEquals("***.***.***-**", CpfValidator.mask("12"));
            assertEquals("***.***.***-**", CpfValidator.mask("123"));
            assertEquals("***.***.***-**", CpfValidator.mask("1234567890")); // 10 dígitos
        }

        @Test
        @DisplayName("CpfValidator.mask: CPFs de 11 dígitos legítimos (raw ou formatados) exibem apenas 3 primeiros e 2 últimos dígitos")
        void maskValid11DigitsCorrectFormat() {
            // Raw 11 dígitos
            assertEquals("123.***.***-01", CpfValidator.mask("12345678901"));
            // Formatado clássico
            assertEquals("123.***.***-01", CpfValidator.mask("123.456.789-01"));
            // Com espaços e separadores anômalos
            assertEquals("123.***.***-01", CpfValidator.mask(" 123-456 789/01 "));
            // CPFs específicos
            assertEquals("111.***.***-35", CpfValidator.mask("111.444.777-35"));
            assertEquals("529.***.***-25", CpfValidator.mask("52998224725"));
            assertEquals("000.***.***-00", CpfValidator.mask("00000000000"));
        }

        @Test
        @DisplayName("CpfValidator.mask: CNPJs de 14 dígitos (raw e formatados) retornam máscara genérica sem vazar dígitos intermediários")
        void maskCnpj14DigitsSafe() {
            assertEquals("***.***.***-**", CpfValidator.mask("12345678000190"));
            assertEquals("***.***.***-**", CpfValidator.mask("12.345.678/0001-90"));
            assertEquals("***.***.***-**", CpfValidator.mask(" 12.345.678/0001-90 "));
        }

        @Test
        @DisplayName("CpfValidator.mask: Entradas extremas de 100 caracteres ou strings maliciosas não causam IndexOutOfBoundsException nem vazam PII")
        void maskExtremeInputsSafe() {
            // 100 dígitos numéricos consecutivos
            String hundredDigits = "9".repeat(100);
            assertEquals("***.***.***-**", CpfValidator.mask(hundredDigits));

            // 100 caracteres de caracteres especiais / SQL injection payloads
            String sqlInjection = "' OR 1=1; DROP TABLE users; -- " + "X".repeat(70);
            assertEquals("***.***.***-**", CpfValidator.mask(sqlInjection));

            // Strings com caracteres Unicode e emojis
            String unicodeString = "123.456.789-01 🚀🎉⚠️";
            // Normaliza para 11 dígitos e mascara com precisão
            assertEquals("123.***.***-01", CpfValidator.mask(unicodeString));
        }

        @Test
        @DisplayName("LGPD: TicketReadyResponseDTO NUNCA vaza CPF não mascarado em nenhum construtor")
        void ticketReadyResponseDtoNeverLeaksUnmaskedCpf() {
            // Construtor canônico com CPF desprotegido
            TicketReadyResponseDTO dto1 = new TicketReadyResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Produto", 1, BigDecimal.TEN,
                "Comprador", "12345678901", BuyerType.STUDENT, Instant.now()
            );
            assertEquals("123.***.***-01", dto1.buyerCpf());
            assertFalse(dto1.buyerCpf().contains("456"), "Dígitos do meio não devem ser expostos!");

            // Construtor a partir de entidade OrderItem
            Order testOrder = new Order();
            testOrder.setId(UUID.randomUUID());
            testOrder.setGuestCpf("11144477735");
            testOrder.setGuestName("Visitante");

            Product testProd = new Product();
            testProd.setTitle("Almoço");

            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID());
            item.setOrder(testOrder);
            item.setProduct(testProd);
            item.setQuantity(2);
            item.setTotalAmount(BigDecimal.valueOf(7.00));
            item.setPaidAt(Instant.now());

            TicketReadyResponseDTO dtoFromEntity = new TicketReadyResponseDTO(item);
            assertEquals("111.***.***-35", dtoFromEntity.buyerCpf());
        }

        @Test
        @DisplayName("LGPD: PaymentHistoryResponseDTO NUNCA vaza CPF/CNPJ não mascarado em nenhum construtor")
        void paymentHistoryResponseDtoNeverLeaksUnmaskedCpf() {
            // Construtor de conveniência de 11 parâmetros
            PaymentHistoryResponseDTO dto1 = new PaymentHistoryResponseDTO(
                UUID.randomUUID(), 100L, "PT1", BigDecimal.TEN, PaymentStatus.COMPLETED,
                PaymentMethod.PIX, "Nome", "12345678901", "https://next", Instant.now(), Instant.now()
            );
            assertEquals("123.***.***-01", dto1.contributorCpfCnpj());

            // Construtor a partir de entidade Payment
            Payment paymentEntity = new Payment();
            paymentEntity.setId(UUID.randomUUID());
            paymentEntity.setContributorCpfCnpj("52998224725");
            paymentEntity.setContributorName("Contribuinte");
            paymentEntity.setStatus(PaymentStatus.COMPLETED);

            PaymentHistoryResponseDTO dtoFromEntity = new PaymentHistoryResponseDTO(paymentEntity);
            assertEquals("529.***.***-25", dtoFromEntity.contributorCpfCnpj());
        }

        @Test
        @DisplayName("LGPD: OrderResponseDTO mantém buyerCpf mascarado quando presente e nulo quando ausente")
        void orderResponseDtoPreservesMaskedOrNull() {
            OrderResponseDTO dtoWithCpf = new OrderResponseDTO(
                UUID.randomUUID(), BigDecimal.TEN, OrderStatus.CREATED, Instant.now(),
                List.of(), "Nome", "12345678901", BuyerType.STUDENT
            );
            assertEquals("123.***.***-01", dtoWithCpf.buyerCpf());

            OrderResponseDTO dtoNullCpf = new OrderResponseDTO(
                UUID.randomUUID(), BigDecimal.TEN, OrderStatus.CREATED, Instant.now(),
                List.of(), "Nome", null, BuyerType.STUDENT
            );
            assertNull(dtoNullCpf.buyerCpf());

            // Conversão de entidade Order (por padrão, buyerCpf no construtor de entidade é null)
            Order orderEntity = new Order();
            orderEntity.setId(UUID.randomUUID());
            orderEntity.setTotalAmount(BigDecimal.ONE);
            orderEntity.setCreateAt(Instant.now());

            OrderResponseDTO dtoFromEntity = new OrderResponseDTO(orderEntity);
            assertNull(dtoFromEntity.buyerCpf());
        }
    }

    // =========================================================================
    // 4. SCOPED PAYMENT HISTORY TESTS
    // =========================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("4. Testes de Escopamento do Histórico de Pagamentos (PaymentService.getMyPayments)")
    class ScopedPaymentHistoryAdversarialTests {

        @Mock
        private PaymentRepository paymentRepository;

        @InjectMocks
        private PaymentService paymentService;

        private User studentUser;
        private Pageable pageable;

        @BeforeEach
        void setUp() {
            studentUser = new User(
                UUID.randomUUID(),
                "estudante@ifnmg.edu.br",
                "Estudante",
                "IFNMG",
                "hash",
                "11144477735",
                UserRoles.USER,
                Instant.now(),
                Instant.now()
            );

            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("Histórico de Pagamentos: DEVE chamar findByUserId com o ID do usuário e NÃO por CPF/CNPJ")
        void getMyPaymentsUsesUserIdAndNotCpf() {
            Payment payment = new Payment();
            payment.setId(UUID.randomUUID());
            payment.setAmount(new BigDecimal("10.00"));
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setContributorCpfCnpj("99988877766"); // CPF de um terceiro pagador (ex: pai/mãe)
            payment.setContributorName("Pai do Aluno");

            Page<Payment> page = new PageImpl<>(List.of(payment));

            when(paymentRepository.findByUserId(studentUser.getId(), pageable)).thenReturn(page);

            var result = paymentService.getMyPayments(studentUser, pageable);

            assertNotNull(result);
            assertEquals(1, result.totalElements());
            // Verifica que o CPF do pagador de terceiro está mascarado na resposta
            assertEquals("999.***.***-66", result.content().get(0).contributorCpfCnpj());

            // Assegura que a busca ocorreu estritamente por userId
            verify(paymentRepository, times(1)).findByUserId(studentUser.getId(), pageable);

            // Garante que os métodos legados baseados em CPF NUNCA foram invocados
            verify(paymentRepository, never()).findAllByContributorCpfCnpj(anyString(), any());
            verify(paymentRepository, never()).findByContributorCpfCnpj(anyString(), any());
        }

        @Test
        @DisplayName("Histórico de Pagamentos: Repositório PaymentRepository possui anotação @Query unindo Order e User ID")
        void verifyPaymentRepositoryQueryAnnotation() throws NoSuchMethodException {
            Method findByUserIdMethod = PaymentRepository.class.getMethod("findByUserId", UUID.class, Pageable.class);
            assertNotNull(findByUserIdMethod, "Método findByUserId(UUID, Pageable) deve existir em PaymentRepository");

            Query queryAnnotation = findByUserIdMethod.getAnnotation(Query.class);
            assertNotNull(queryAnnotation, "findByUserId deve ser anotado com @Query");

            String jpql = queryAnnotation.value();
            assertTrue(jpql.contains("JOIN p.orderItems oi"), "JPQL deve realizar JOIN com orderItems");
            assertTrue(jpql.contains("JOIN oi.order o"), "JPQL deve realizar JOIN com order");
            assertTrue(jpql.contains("o.user.id = :userId"), "JPQL deve filtrar por o.user.id = :userId");
        }
    }
}
