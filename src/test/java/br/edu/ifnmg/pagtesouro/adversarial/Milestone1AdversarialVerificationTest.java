package br.edu.ifnmg.pagtesouro.adversarial;

import br.edu.ifnmg.pagtesouro.controllers.PaymentController;
import br.edu.ifnmg.pagtesouro.domain.order.dto.BatchExchangeRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.DirectCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.OrderCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.infra.security.SecurityConfiguration;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentNotificationService;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Verificação Empírica e Adversarial: Milestone 1 (Canonicalização de Rotas e DTOs)")
class Milestone1AdversarialVerificationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static Validator validator;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentNotificationService paymentNotificationService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentController paymentController;

    private MockMvc mockMvc;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    // =========================================================================
    // 1. ADVERSARIAL DTO VERIFICATION: Exhaustive @JsonAlias Scan
    // =========================================================================

    @Test
    @DisplayName("Adversarial: Varredura exaustiva por reflexão de @JsonAlias em classes, componentes, campos, construtores e métodos")
    void testNoJsonAliasAnywhereInTargetDTOs() {
        List<Class<?>> targetClasses = List.of(
            BatchExchangeRequestDTO.class,
            OrderRequestDTO.class,
            OrderRequestDTO.items.class,
            DirectCheckoutRequestDTO.class,
            OrderCheckoutRequestDTO.class,
            RegisterDTO.class
        );

        for (Class<?> clazz : targetClasses) {
            // 1. Classe
            assertFalse(
                clazz.isAnnotationPresent(JsonAlias.class),
                "Classe " + clazz.getName() + " possui @JsonAlias não permitido!"
            );

            // 2. Record Components
            RecordComponent[] components = clazz.getRecordComponents();
            if (components != null) {
                for (RecordComponent component : components) {
                    assertFalse(
                        component.isAnnotationPresent(JsonAlias.class),
                        "RecordComponent '" + component.getName() + "' em " + clazz.getSimpleName() + " possui @JsonAlias!"
                    );
                }
            }

            // 3. Campos declarados
            for (Field field : clazz.getDeclaredFields()) {
                assertFalse(
                    field.isAnnotationPresent(JsonAlias.class),
                    "Campo '" + field.getName() + "' em " + clazz.getSimpleName() + " possui @JsonAlias!"
                );
            }

            // 4. Construtores e seus parâmetros
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                assertFalse(
                    constructor.isAnnotationPresent(JsonAlias.class),
                    "Construtor em " + clazz.getSimpleName() + " possui @JsonAlias!"
                );
                for (Parameter parameter : constructor.getParameters()) {
                    assertFalse(
                        parameter.isAnnotationPresent(JsonAlias.class),
                        "Parâmetro '" + parameter.getName() + "' de construtor em " + clazz.getSimpleName() + " possui @JsonAlias!"
                    );
                }
            }

            // 5. Métodos e seus parâmetros
            for (Method method : clazz.getDeclaredMethods()) {
                assertFalse(
                    method.isAnnotationPresent(JsonAlias.class),
                    "Método '" + method.getName() + "' em " + clazz.getSimpleName() + " possui @JsonAlias!"
                );
                for (Parameter parameter : method.getParameters()) {
                    assertFalse(
                        parameter.isAnnotationPresent(JsonAlias.class),
                        "Parâmetro '" + parameter.getName() + "' do método '" + method.getName() + "' em " + clazz.getSimpleName() + " possui @JsonAlias!"
                    );
                }
            }
        }
    }

    // =========================================================================
    // 2. ADVERSARIAL DTO VERIFICATION: Snake_case unmapped and rejected
    // =========================================================================

    @Test
    @DisplayName("BatchExchangeRequestDTO: snake_case 'item_ids' e 'items' são estritamente nulos e violam Bean Validation")
    void testBatchExchangeRequestDtoSnakeCasePayloads() throws Exception {
        UUID testId = UUID.randomUUID();

        // Adversarial Payload 1: 'item_ids'
        String json1 = "{\"item_ids\": [\"" + testId + "\"]}";
        BatchExchangeRequestDTO dto1 = objectMapper.readValue(json1, BatchExchangeRequestDTO.class);
        assertNull(dto1.itemIds(), "Propriedade snake_case 'item_ids' não deve ser associada a 'itemIds'");
        Set<ConstraintViolation<BatchExchangeRequestDTO>> violations1 = validator.validate(dto1);
        assertFalse(violations1.isEmpty(), "Payload com 'item_ids' nulo deve ser rejeitado pela validação (@NotEmpty)");

        // Adversarial Payload 2: 'items'
        String json2 = "{\"items\": [\"" + testId + "\"]}";
        BatchExchangeRequestDTO dto2 = objectMapper.readValue(json2, BatchExchangeRequestDTO.class);
        assertNull(dto2.itemIds(), "Propriedade legada 'items' não deve ser associada a 'itemIds'");

        // Positive Payload: camelCase 'itemIds'
        String jsonValid = "{\"itemIds\": [\"" + testId + "\"]}";
        BatchExchangeRequestDTO validDto = objectMapper.readValue(jsonValid, BatchExchangeRequestDTO.class);
        assertNotNull(validDto.itemIds());
        assertEquals(1, validDto.itemIds().size());
        assertEquals(testId, validDto.itemIds().get(0));
        assertTrue(validator.validate(validDto).isEmpty(), "DTO com camelCase válido deve passar na validação");
    }

    @Test
    @DisplayName("OrderRequestDTO: snake_case 'items_list' e 'product_id' são estritamente ignorados")
    void testOrderRequestDtoSnakeCasePayloads() throws Exception {
        UUID prodId = UUID.randomUUID();

        // Adversarial Payload 1: 'items_list'
        String json1 = "{\"items_list\": [{\"productId\": \"" + prodId + "\", \"quantity\": 2}]}";
        OrderRequestDTO dto1 = objectMapper.readValue(json1, OrderRequestDTO.class);
        assertNull(dto1.itemsList(), "Propriedade snake_case 'items_list' não deve ser mapeada");

        // Adversarial Payload 2: 'product_id' no item interno
        String json2 = "{\"itemsList\": [{\"product_id\": \"" + prodId + "\", \"quantity\": 2}]}";
        OrderRequestDTO dto2 = objectMapper.readValue(json2, OrderRequestDTO.class);
        assertNotNull(dto2.itemsList());
        assertEquals(1, dto2.itemsList().size());
        assertNull(dto2.itemsList().get(0).productId(), "Propriedade interna 'product_id' não deve ser mapeada para 'productId'");
        assertEquals(2, dto2.itemsList().get(0).quantity());

        // Positive Payload: camelCase
        String jsonValid = "{\"itemsList\": [{\"productId\": \"" + prodId + "\", \"quantity\": 5}]}";
        OrderRequestDTO validDto = objectMapper.readValue(jsonValid, OrderRequestDTO.class);
        assertNotNull(validDto.itemsList());
        assertEquals(prodId, validDto.itemsList().get(0).productId());
        assertEquals(5, validDto.itemsList().get(0).quantity());
    }

    @Test
    @DisplayName("DirectCheckoutRequestDTO: payload snake_case completo é desassociado e rejeitado por @Valid")
    void testDirectCheckoutRequestDtoSnakeCasePayloads() throws Exception {
        UUID prodId = UUID.randomUUID();

        // Adversarial Payload com snake_case completo
        String snakeJson = """
            {
                "product_id": "%s",
                "quantity": 2,
                "contributor_cpf_cnpj": "11144477735",
                "contributor_name": "Carlos Silva",
                "is_mobile": true
            }
            """.formatted(prodId);

        DirectCheckoutRequestDTO dto = objectMapper.readValue(snakeJson, DirectCheckoutRequestDTO.class);
        assertNull(dto.productId(), "'product_id' não deve ser associado");
        assertNull(dto.contributorCpfCnpj(), "'contributor_cpf_cnpj' não deve ser associado");
        assertNull(dto.contributorName(), "'contributor_name' não deve ser associado");
        assertFalse(dto.isMobile(), "'is_mobile' não deve ser associado");

        // Validação Bean Validation DEVE falhar (rejeição)
        Set<ConstraintViolation<DirectCheckoutRequestDTO>> violations = validator.validate(dto);
        assertEquals(3, violations.size(), "Devem existir 3 violações (productId, contributorCpfCnpj, contributorName)");

        // Positive Payload
        String camelJson = """
            {
                "productId": "%s",
                "quantity": 2,
                "contributorCpfCnpj": "11144477735",
                "contributorName": "Carlos Silva",
                "isMobile": true
            }
            """.formatted(prodId);

        DirectCheckoutRequestDTO validDto = objectMapper.readValue(camelJson, DirectCheckoutRequestDTO.class);
        assertEquals(prodId, validDto.productId());
        assertEquals(2, validDto.quantity());
        assertEquals("11144477735", validDto.contributorCpfCnpj());
        assertEquals("Carlos Silva", validDto.contributorName());
        assertTrue(validDto.isMobile());
        assertTrue(validator.validate(validDto).isEmpty());
    }

    @Test
    @DisplayName("OrderCheckoutRequestDTO: payload snake_case tem campos fiscais nulos e isMobile padrão falso")
    void testOrderCheckoutRequestDtoSnakeCasePayloads() throws Exception {
        String snakeJson = """
            {
                "contributor_cpf_cnpj": "11144477735",
                "contributor_name": "Carlos Silva",
                "is_mobile": true
            }
            """;

        OrderCheckoutRequestDTO dto = objectMapper.readValue(snakeJson, OrderCheckoutRequestDTO.class);
        assertNull(dto.contributorCpfCnpj());
        assertNull(dto.contributorName());
        assertFalse(dto.isMobile());

        // Positive Payload
        String camelJson = """
            {
                "contributorCpfCnpj": "11144477735",
                "contributorName": "Carlos Silva",
                "isMobile": true
            }
            """;
        OrderCheckoutRequestDTO validDto = objectMapper.readValue(camelJson, OrderCheckoutRequestDTO.class);
        assertEquals("11144477735", validDto.contributorCpfCnpj());
        assertEquals("Carlos Silva", validDto.contributorName());
        assertTrue(validDto.isMobile());
    }

    @Test
    @DisplayName("RegisterDTO: snake_case 'last_name' resulta em lastName nulo e rejeição por validação @NotBlank")
    void testRegisterDtoSnakeCasePayloads() throws Exception {
        String snakeJson = """
            {
                "email": "aluno@ifnmg.edu.br",
                "password": "MinhaSenha123",
                "name": "Maria",
                "last_name": "Santos",
                "cpf": "11144477735"
            }
            """;

        RegisterDTO dto = objectMapper.readValue(snakeJson, RegisterDTO.class);
        assertNull(dto.lastName(), "'last_name' não deve ser populado");

        Set<ConstraintViolation<RegisterDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size(), "Deve conter 1 violação por lastName nulo/em branco");
        assertEquals("lastName", violations.iterator().next().getPropertyPath().toString());

        // Positive Payload
        String camelJson = """
            {
                "email": "aluno@ifnmg.edu.br",
                "password": "MinhaSenha123",
                "name": "Maria",
                "lastName": "Santos",
                "cpf": "11144477735"
            }
            """;

        RegisterDTO validDto = objectMapper.readValue(camelJson, RegisterDTO.class);
        assertEquals("Santos", validDto.lastName());
        assertTrue(validator.validate(validDto).isEmpty());
    }

    // =========================================================================
    // 3. ADVERSARIAL ROUTE VERIFICATION: PaymentController
    // =========================================================================

    @Test
    @DisplayName("PaymentController: Reflexão garante ausência de rotas obsoletas /anonymous e /checkout/order/{orderId}")
    void testPaymentControllerRoutesReflection() {
        RequestMapping classMapping = PaymentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        List<String> basePaths = Arrays.asList(classMapping.value().length > 0 ? classMapping.value() : classMapping.path());
        assertTrue(basePaths.contains("/payments"));

        // Percorrer todos os métodos e garantir ausência de rotas obsoletas
        for (Method method : PaymentController.class.getDeclaredMethods()) {
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                String[] paths = switch (annotation) {
                    case PostMapping m -> m.value().length > 0 ? m.value() : m.path();
                    case GetMapping m -> m.value().length > 0 ? m.value() : m.path();
                    case PutMapping m -> m.value().length > 0 ? m.value() : m.path();
                    case DeleteMapping m -> m.value().length > 0 ? m.value() : m.path();
                    case RequestMapping m -> m.value().length > 0 ? m.value() : m.path();
                    default -> new String[0];
                };

                for (String path : paths) {
                    assertFalse(
                        path.equals("/anonymous") || path.contains("anonymous"),
                        "Método " + method.getName() + " mapeia rota obsoleta: " + path
                    );
                    assertFalse(
                        path.contains("/checkout/order/"),
                        "Método " + method.getName() + " mapeia rota obsoleta: " + path
                    );
                }
            }
        }
    }

    @Test
    @DisplayName("MockMvc: Rotas legadas retornam 404 Not Found e rotas canônicas respondem corretamente")
    void testMockMvcPaymentRoutesAdversarial() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();

        CheckoutResponseDTO responseDTO = new CheckoutResponseDTO(
            UUID.randomUUID(),
            "PT123456",
            new BigDecimal("10.00"),
            "CREATED",
            "https://pagtesouro.tesouro.gov.br/checkout/123"
        );

        // 1. POST /payments/anonymous DEVE retornar 404 (Rota removida)
        mockMvc.perform(post("/payments/anonymous")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());

        // 2. POST /payments/checkout/order/{orderId} DEVE retornar 404 (Rota removida)
        mockMvc.perform(post("/payments/checkout/order/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());

        // 3. POST /payments/checkout/direct com snake_case DEVE retornar 400 Bad Request (@Valid falha)
        String snakeBody = """
            {
                "product_id": "%s",
                "quantity": 1,
                "contributor_cpf_cnpj": "11144477735",
                "contributor_name": "Carlos Silva"
            }
            """.formatted(prodId);

        mockMvc.perform(post("/payments/checkout/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(snakeBody))
                .andExpect(status().isBadRequest());

        // 4. POST /payments/checkout/direct com camelCase válido DEVE retornar 201 Created
        when(paymentService.checkoutDirect(any(DirectCheckoutRequestDTO.class))).thenReturn(responseDTO);

        String validDirectBody = """
            {
                "productId": "%s",
                "quantity": 1,
                "contributorCpfCnpj": "11144477735",
                "contributorName": "Carlos Silva",
                "isMobile": false
            }
            """.formatted(prodId);

        mockMvc.perform(post("/payments/checkout/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validDirectBody))
                .andExpect(status().isCreated());

        // 5. POST /payments/checkout/{orderId} DEVE retornar 201 Created
        when(paymentService.checkoutOrder(eq(orderId), any(OrderCheckoutRequestDTO.class), any())).thenReturn(List.of(responseDTO));

        mockMvc.perform(post("/payments/checkout/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isMobile\": false}"))
                .andExpect(status().isCreated());

        // 6. POST /payments/checkout/{orderId} sem corpo (null body) DEVE usar fallback padrão e retornar 201 Created
        mockMvc.perform(post("/payments/checkout/" + orderId))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // 4. SECURITY CONFIGURATION EMPIRICAL AUDIT
    // =========================================================================

    @Test
    @DisplayName("SecurityConfiguration: Verificação empírica das regras de rota permitAll e remoção de rotas obsoletas")
    void testSecurityConfigurationPermitAllDirectRoute() throws Exception {
        // Inspeciona o código-fonte de SecurityConfiguration.java para garantir a configuração correta
        Path securityConfigPath = Path.of("src/main/java/br/edu/ifnmg/pagtesouro/infra/security/SecurityConfiguration.java");
        assertTrue(Files.exists(securityConfigPath), "Arquivo SecurityConfiguration.java deve existir");

        String content = Files.readString(securityConfigPath);

        // 1. /payments/checkout/direct DEVE ser permitAll
        assertTrue(
            content.contains(".requestMatchers(HttpMethod.POST, \"/payments/checkout/direct\").permitAll()"),
            "SecurityConfiguration deve conter regra explícita .permitAll() para POST /payments/checkout/direct"
        );

        // 2. /payments/anonymous NÃO DEVE constar como permitAll
        assertFalse(
            content.contains("/payments/anonymous"),
            "SecurityConfiguration não deve conter referência a /payments/anonymous"
        );

        // 3. /orders/guest NÃO DEVE constar como permitAll
        assertFalse(
            content.contains("/orders/guest"),
            "SecurityConfiguration não deve conceder permitAll a /orders/guest"
        );
    }
}
