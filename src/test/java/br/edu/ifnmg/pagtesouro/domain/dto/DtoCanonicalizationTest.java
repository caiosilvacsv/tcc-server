package br.edu.ifnmg.pagtesouro.domain.dto;

import br.edu.ifnmg.pagtesouro.domain.order.BuyerType;
import br.edu.ifnmg.pagtesouro.domain.order.dto.BatchExchangeRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentMethod;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.DirectCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.OrderCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.PaymentHistoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Canonicalização de DTOs e Mascaramento LGPD")
class DtoCanonicalizationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    @DisplayName("Reflexão: Nenhuma anotação @JsonAlias deve existir nos DTOs alvos")
    void verifyNoJsonAliasAnnotationsPresent() {
        List<Class<?>> targetClasses = List.of(
            BatchExchangeRequestDTO.class,
            OrderRequestDTO.class,
            OrderRequestDTO.items.class,
            DirectCheckoutRequestDTO.class,
            OrderCheckoutRequestDTO.class,
            RegisterDTO.class
        );

        for (Class<?> clazz : targetClasses) {
            // Verifica nos record components
            RecordComponent[] components = clazz.getRecordComponents();
            if (components != null) {
                for (RecordComponent component : components) {
                    assertFalse(
                        component.isAnnotationPresent(JsonAlias.class),
                        "RecordComponent '" + component.getName() + "' em " + clazz.getSimpleName() + " não deve ter @JsonAlias"
                    );
                }
            }

            // Verifica nos fields
            for (Field field : clazz.getDeclaredFields()) {
                assertFalse(
                    field.isAnnotationPresent(JsonAlias.class),
                    "Campo '" + field.getName() + "' em " + clazz.getSimpleName() + " não deve ter @JsonAlias"
                );
            }
        }
    }

    @Test
    @DisplayName("BatchExchangeRequestDTO: Aceita camelCase 'itemIds' e ignora snake_case 'item_ids'")
    void batchExchangeRequestDtoStrictCamelCase() throws Exception {
        UUID id = UUID.randomUUID();
        String camelCaseJson = "{\"itemIds\": [\"" + id + "\"]}";
        BatchExchangeRequestDTO validDto = objectMapper.readValue(camelCaseJson, BatchExchangeRequestDTO.class);
        assertNotNull(validDto.itemIds());
        assertEquals(1, validDto.itemIds().size());
        assertEquals(id, validDto.itemIds().get(0));

        String snakeCaseJson = "{\"item_ids\": [\"" + id + "\"]}";
        BatchExchangeRequestDTO invalidDto = objectMapper.readValue(snakeCaseJson, BatchExchangeRequestDTO.class);
        assertNull(invalidDto.itemIds(), "snake_case 'item_ids' não deve ser mapeado");
    }

    @Test
    @DisplayName("OrderRequestDTO: Aceita camelCase 'itemsList' e 'productId', ignora snake_case")
    void orderRequestDtoStrictCamelCase() throws Exception {
        UUID prodId = UUID.randomUUID();
        String camelCaseJson = "{\"itemsList\": [{\"productId\": \"" + prodId + "\", \"quantity\": 3}]}";
        OrderRequestDTO validDto = objectMapper.readValue(camelCaseJson, OrderRequestDTO.class);
        assertNotNull(validDto.itemsList());
        assertEquals(1, validDto.itemsList().size());
        assertEquals(prodId, validDto.itemsList().get(0).productId());
        assertEquals(3, validDto.itemsList().get(0).quantity());

        String snakeCaseJson = "{\"items_list\": [{\"product_id\": \"" + prodId + "\", \"quantity\": 3}]}";
        OrderRequestDTO invalidDto = objectMapper.readValue(snakeCaseJson, OrderRequestDTO.class);
        assertNull(invalidDto.itemsList(), "snake_case 'items_list' não deve ser mapeado");
    }

    @Test
    @DisplayName("DirectCheckoutRequestDTO: Aceita estritamente propriedades camelCase")
    void directCheckoutRequestDtoStrictCamelCase() throws Exception {
        UUID prodId = UUID.randomUUID();
        String camelCaseJson = """
            {
                "productId": "%s",
                "quantity": 2,
                "contributorCpfCnpj": "11144477735",
                "contributorName": "Carlos Silva",
                "isMobile": true
            }
            """.formatted(prodId);

        DirectCheckoutRequestDTO validDto = objectMapper.readValue(camelCaseJson, DirectCheckoutRequestDTO.class);
        assertEquals(prodId, validDto.productId());
        assertEquals(2, validDto.quantity());
        assertEquals("11144477735", validDto.contributorCpfCnpj());
        assertEquals("Carlos Silva", validDto.contributorName());
        assertTrue(validDto.isMobile());

        String snakeCaseJson = """
            {
                "product_id": "%s",
                "quantity": 2,
                "contributor_cpf_cnpj": "11144477735",
                "contributor_name": "Carlos Silva",
                "is_mobile": true
            }
            """.formatted(prodId);

        DirectCheckoutRequestDTO invalidDto = objectMapper.readValue(snakeCaseJson, DirectCheckoutRequestDTO.class);
        assertNull(invalidDto.productId(), "snake_case 'product_id' não deve ser mapeado");
        assertNull(invalidDto.contributorCpfCnpj(), "snake_case 'contributor_cpf_cnpj' não deve ser mapeado");
        assertNull(invalidDto.contributorName(), "snake_case 'contributor_name' não deve ser mapeado");
        assertFalse(invalidDto.isMobile());
    }

    @Test
    @DisplayName("OrderCheckoutRequestDTO: Aceita estritamente camelCase e ignora aliases snake_case")
    void orderCheckoutRequestDtoStrictCamelCase() throws Exception {
        String camelCaseJson = """
            {
                "contributorCpfCnpj": "11144477735",
                "contributorName": "Ana Maria",
                "isMobile": true
            }
            """;
        OrderCheckoutRequestDTO validDto = objectMapper.readValue(camelCaseJson, OrderCheckoutRequestDTO.class);
        assertEquals("11144477735", validDto.contributorCpfCnpj());
        assertEquals("Ana Maria", validDto.contributorName());
        assertTrue(validDto.isMobile());

        String snakeCaseJson = """
            {
                "contributor_cpf_cnpj": "11144477735",
                "contributor_name": "Ana Maria",
                "is_mobile": true
            }
            """;
        OrderCheckoutRequestDTO invalidDto = objectMapper.readValue(snakeCaseJson, OrderCheckoutRequestDTO.class);
        assertNull(invalidDto.contributorCpfCnpj());
        assertNull(invalidDto.contributorName());
        assertFalse(invalidDto.isMobile());
    }

    @Test
    @DisplayName("RegisterDTO: Aceita 'lastName' e rejeita/ignora 'last_name'")
    void registerDtoStrictCamelCase() throws Exception {
        String camelCaseJson = """
            {
                "email": "aluno@ifnmg.edu.br",
                "password": "secretPassword123",
                "name": "Maria",
                "lastName": "Santos",
                "cpf": "11144477735"
            }
            """;
        RegisterDTO validDto = objectMapper.readValue(camelCaseJson, RegisterDTO.class);
        assertEquals("Santos", validDto.lastName());

        String snakeCaseJson = """
            {
                "email": "aluno@ifnmg.edu.br",
                "password": "secretPassword123",
                "name": "Maria",
                "last_name": "Santos",
                "cpf": "11144477735"
            }
            """;
        RegisterDTO invalidDto = objectMapper.readValue(snakeCaseJson, RegisterDTO.class);
        assertNull(invalidDto.lastName(), "snake_case 'last_name' não deve ser populado");
    }

    @Test
    @DisplayName("LGPD: TicketReadyResponseDTO mascara o buyerCpf")
    void ticketReadyResponseDtoMasksCpf() {
        TicketReadyResponseDTO ticket = new TicketReadyResponseDTO(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Refeição RU",
            1,
            new BigDecimal("2.50"),
            "Aluno Teste",
            "11144477735",
            BuyerType.STUDENT,
            Instant.now()
        );
        assertEquals("111.***.***-35", ticket.buyerCpf());
    }

    @Test
    @DisplayName("LGPD: PaymentHistoryResponseDTO mascara o contributorCpfCnpj")
    void paymentHistoryResponseDtoMasksCpf() {
        PaymentHistoryResponseDTO history = new PaymentHistoryResponseDTO(
            UUID.randomUUID(),
            123456L,
            "PAG123",
            new BigDecimal("10.00"),
            PaymentStatus.COMPLETED,
            PaymentMethod.PIX,
            "Contribuinte Teste",
            "52998224725",
            "https://pagtesouro.tesouro.gov.br",
            Instant.now(),
            Instant.now()
        );
        assertEquals("529.***.***-25", history.contributorCpfCnpj());
    }

    @Test
    @DisplayName("LGPD: OrderResponseDTO preserva buyerCpf nulo ou mascarado")
    void orderResponseDtoMasksOrNullBuyerCpf() {
        OrderResponseDTO orderWithCpf = new OrderResponseDTO(
            UUID.randomUUID(),
            new BigDecimal("5.00"),
            null,
            Instant.now(),
            List.of(),
            "Comprador",
            "12345678901",
            BuyerType.STUDENT
        );
        assertEquals("123.***.***-01", orderWithCpf.buyerCpf());

        OrderResponseDTO orderNullCpf = new OrderResponseDTO(
            UUID.randomUUID(),
            new BigDecimal("5.00"),
            null,
            Instant.now(),
            List.of(),
            "Comprador",
            null,
            BuyerType.STUDENT
        );
        assertNull(orderNullCpf.buyerCpf());
    }
}
