package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
        return paymentService.checkoutOrder(orderId, request, user)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
