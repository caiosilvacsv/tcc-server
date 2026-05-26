package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.services.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST responsável por expor as rotas de gerenciamento de pedidos (carrinho de compras).
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Rota autenticada para o aluno criar um novo pedido (carrinho com tíquetes ou taxas).
     *
     * @param request O DTO contendo os produtos e quantidades.
     * @param user O usuário autenticado injetado pelo Spring Security.
     * @return O DTO do pedido criado com status 201 Created.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestBody @Valid OrderRequestDTO request,
            @AuthenticationPrincipal User user) {
        OrderResponseDTO response = orderService.createOrder(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Rota autenticada para listar o histórico de pedidos do próprio estudante autenticado.
     *
     * @param user O usuário autenticado injetado pelo Spring Security.
     * @return Lista com o histórico de pedidos e status 200 OK.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(@AuthenticationPrincipal User user) {
        List<OrderResponseDTO> orders = orderService.getUserOrders(user);
        return ResponseEntity.ok(orders);
    }

    /**
     * Rota autenticada para consultar os detalhes de um pedido específico do estudante.
     *
     * @param id O ID único do pedido.
     * @param user O usuário autenticado.
     * @return Os detalhes do pedido ou erro de permissão (segurança) com status 200 OK.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        OrderResponseDTO order = orderService.getOrderById(id, user);
        return ResponseEntity.ok(order);
    }

    /**
     * Rota de baixa e troca física de um item de pedido (tíquete ou taxa de biblioteca) restrita a administradores.
     *
     * @param itemId O ID do item a ser trocado.
     * @param user O usuário logado (deve possuir privilégio de ADMIN).
     * @return O DTO do item atualizado com o status de trocado com status 200 OK.
     */
    @PutMapping("/items/{itemId}/exchange")
    public ResponseEntity<OrderResponseDTO.OrderItemResponseDTO> exchangeItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User user) {
        
        // Proteção de segurança: Apenas administradores podem fazer a baixa física
        if (!user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("Acesso negado: Apenas administradores podem efetuar a baixa de itens.");
        }

        OrderResponseDTO.OrderItemResponseDTO response = orderService.exchangeItem(itemId, user);
        return ResponseEntity.ok(response);
    }
}
