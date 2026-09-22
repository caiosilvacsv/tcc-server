package br.edu.ifnmg.pagtesouro.infra.pagtesouro;

import br.edu.ifnmg.pagtesouro.domain.payment.PaymentMethod;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroQueryResponseDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroRequestDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Cliente HTTP do PagTesouro (PagTesouroClient)")
class PagTesouroClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private PagTesouroProperties properties;
    private PagTesouroClient client;

    @BeforeEach
    void setUp() {
        properties = new PagTesouroProperties(
            "https://valpagtesouro.tesouro.gov.br",
            "token-ug-salinas",
            "https://meudominio.edu.br/api/payments/webhook"
        );
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        client = new PagTesouroClient(properties, webClientBuilder);
    }

    @Test
    @DisplayName("Deve inicializar e normalizar a URL base corretamente")
    void constructorNormalizesBaseUrl() {
        verify(webClientBuilder).baseUrl("https://valpagtesouro.tesouro.gov.br/api/gru/");

        // Teste com barra no final
        WebClient.Builder builder2 = mock(WebClient.Builder.class);
        when(builder2.baseUrl(anyString())).thenReturn(builder2);
        when(builder2.build()).thenReturn(webClient);
        PagTesouroProperties props2 = new PagTesouroProperties("https://valpagtesouro.tesouro.gov.br/", "token", "url");
        new PagTesouroClient(props2, builder2);
        verify(builder2).baseUrl("https://valpagtesouro.tesouro.gov.br/api/gru/");

        // Teste já contendo /api/gru
        WebClient.Builder builder3 = mock(WebClient.Builder.class);
        when(builder3.baseUrl(anyString())).thenReturn(builder3);
        when(builder3.build()).thenReturn(webClient);
        PagTesouroProperties props3 = new PagTesouroProperties("https://valpagtesouro.tesouro.gov.br/api/gru", "token", "url");
        new PagTesouroClient(props3, builder3);
        verify(builder3).baseUrl("https://valpagtesouro.tesouro.gov.br/api/gru/");
    }

    @Test
    @DisplayName("Deve enviar solicitação de pagamento com sucesso via WebClient mockado")
    @SuppressWarnings("unchecked")
    void createPaymentSuccess() {
        PagTesouroRequestDTO request = new PagTesouroRequestDTO(
            1, "REF123", "092026", "30092026", "12345678909",
            "Aluno Teste", new BigDecimal("10.00"), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            1, "http://localhost:8080/webhook"
        );
        PagTesouroResponseDTO expectedResponse = new PagTesouroResponseDTO(
            "PAY123",
            Instant.now(),
            "https://next.url",
            new PagTesouroResponseDTO.Situacao(PaymentStatus.CREATED)
        );

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("solicitacao-pagamento")).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(request)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PagTesouroResponseDTO.class)).thenReturn(Mono.just(expectedResponse));

        PagTesouroResponseDTO response = client.createPayment(request).block();

        assertNotNull(response);
        assertEquals("PAY123", response.idPayment());
        assertEquals("https://next.url", response.nextUrl());
        assertEquals(PaymentStatus.CREATED, response.situation().code());
        verify(webClient).post();
        verify(requestBodyUriSpec).uri("solicitacao-pagamento");
    }

    @Test
    @DisplayName("Deve consultar status do pagamento com sucesso via WebClient mockado")
    @SuppressWarnings("unchecked")
    void getPaymentStatusSuccess() {
        PagTesouroQueryResponseDTO expectedQuery = new PagTesouroQueryResponseDTO(
            "PAY123",
            PaymentMethod.PIX,
            new BigDecimal("10.00"),
            "Banco do Brasil PSP",
            "psp-tx-999",
            new PagTesouroQueryResponseDTO.Situation(PaymentStatus.COMPLETED, Instant.now())
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("pagamentos/{id}", "PAY123")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(PagTesouroQueryResponseDTO.class)).thenReturn(Mono.just(expectedQuery));

        PagTesouroQueryResponseDTO response = client.getPaymentStatus("PAY123").block();

        assertNotNull(response);
        assertEquals("PAY123", response.idPayment());
        assertEquals(PaymentMethod.PIX, response.typePayment());
        assertEquals(new BigDecimal("10.00"), response.amount());
        assertEquals("Banco do Brasil PSP", response.paymentServiceProviderName());
        assertEquals("psp-tx-999", response.pspTransactionId());
        assertEquals(PaymentStatus.COMPLETED, response.situation().code());
        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("pagamentos/{id}", "PAY123");
    }
}
