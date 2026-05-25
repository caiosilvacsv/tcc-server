package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para capturar a solicitação de checkout de um pedido existente.
 *
 * @author Caio da Silva Viana
 */
public record CheckoutRequestDTO(
    @NotNull(message = "O ID do pedido é obrigatório")
    UUID orderId
) {}
