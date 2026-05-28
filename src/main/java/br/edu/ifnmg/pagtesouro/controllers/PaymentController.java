package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentService;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentNotificationService;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.PaymentHistoryResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.UUID;

/**
 * Controlador REST responsável por expor as rotas de checkout de pagamento da aplicação.
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentNotificationService paymentNotificationService;
    private final PaymentRepository paymentRepository;

    public PaymentController(
            PaymentService paymentService,
            PaymentNotificationService paymentNotificationService,
            PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentNotificationService = paymentNotificationService;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Rota pública para criação de pagamento simplificado/direto para usuários anônimos ou cidadãos externos.
     *
     * @param request O DTO contendo o ID do produto, quantidade e dados cadastrais do doador/pagador.
     * @return Um Mono contendo a resposta com a URL de redirecionamento do PagTesouro e status 201 Created.
     */
    @PostMapping("/anonymous")
    public Mono<ResponseEntity<CheckoutResponseDTO>> createPaymentAnonymous(@RequestBody @Valid CheckoutRequestDTO request) {
        return paymentService.checkoutDirect(request)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Rota privada autenticada para o aluno realizar o checkout de um Pedido (carrinho) pendente no portal.
     *
     * @param orderId O ID único do pedido cadastrado.
     * @param request O DTO contendo os dados cadastrais do pagador da guia.
     * @param user O usuário estudante autenticado injetado pelo Spring Security.
     * @return Um Mono contendo a resposta com a URL de redirecionamento do PagTesouro e status 201 Created.
     */
    @PostMapping("/checkout/{orderId}")
    public Mono<ResponseEntity<CheckoutResponseDTO>> createPaymentCheckout(
            @PathVariable UUID orderId,
            @RequestBody @Valid CheckoutRequestDTO request,
            @AuthenticationPrincipal User user) {
        System.out.println(orderId.toString());
        System.out.println( request.toString());
        System.out.println(user.toString());
        return paymentService.checkoutOrder(orderId, request, user)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Rota pública que estabelece um fluxo contínuo de eventos Server-Sent Events (SSE) para o cliente.
     * <p>
     * O frontend se conecta a esta rota e aguarda em tempo real o sinalizador de compensação da fatura.
     * Se a fatura já estiver compensada (COMPLETED) no momento da conexão, envia o sinal imediatamente e encerra
     * o canal para evitar consumo de conexões síncronas.
     * </p>
     *
     * @param paymentId ID interno da fatura de pagamento a ser escutada.
     * @return Um {@link SseEmitter} que emitirá os eventos reativos.
     */
    @GetMapping(value = "/{paymentId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter listenPaymentStatusSSE(@PathVariable UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new FindException("Fatura de pagamento com ID " + paymentId + " não localizada no portal."));

        // Otimização de Escuta Tardia: Se já estiver liquidado, notifica imediatamente e fecha
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            SseEmitter instantEmitter = new SseEmitter(10_000L); // Timeout curto de 10s
            try {
                instantEmitter.send(SseEmitter.event()
                        .name("PAYMENT_PAID")
                        .data("COMPLETED"));
                instantEmitter.complete();
            } catch (IOException e) {
                instantEmitter.completeWithError(e);
            }
            return instantEmitter;
        }

        // Caso contrário, registra a conexão de escuta persistente na memória concorrente
        return paymentNotificationService.registerEmitter(paymentId);
    }

    /**
     * Rota privada autenticada que retorna de forma paginada o histórico de todas as faturas
     * e tentativas de pagamento geradas pelo contribuinte logado com base em seu CPF.
     *
     * @param user O usuário estudante autenticado injetado pelo Spring Security.
     * @param pageable Configuração de paginação padrão (10 registros por página, ordenados por data de criação de forma decrescente).
     * @return Um Mono contendo a página de respostas do histórico de pagamentos.
     */
    @GetMapping
    public Mono<ResponseEntity<Page<PaymentHistoryResponseDTO>>> getMyPayments(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return paymentService.getMyPayments(user, pageable)
                .map(ResponseEntity::ok);
    }
}
