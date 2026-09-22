package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.order.dto.BatchExchangeRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.services.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST responsável por expor as rotas de gerenciamento de pedidos (carrinho de compras),
 * consulta de tíquetes disponíveis (estudantes e convidados) e baixa física.
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Responsável para controle de conexão com os serviços.
     * <p>
     *   Geram as ordens e fazem a persistência dos dados.
     * </p>
     * @param orderService Componente responsável por operar a lógica da ordem.
     */
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
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
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
     * Rota autenticada para consulta de tíquetes disponíveis (pagos e ainda não resgatados) organizados em fila FIFO.
     * <p>
     * Se o parâmetro {@code cpf} for omitido, resgata automaticamente os tíquetes do próprio estudante autenticado.
     * Se o parâmetro {@code cpf} for fornecido, exige que o solicitante possua perfil de administrador
     * para consultar os tíquetes no balcão de atendimento.
     * </p>
     *
     * @param cpf Parâmetro opcional contendo o CPF a ser consultado (restrito a administradores).
     * @param user O usuário autenticado injetado pelo Spring Security.
     * @return Lista contendo os tíquetes disponíveis projetados em {@link TicketReadyResponseDTO} com status 200 OK.
     */
    @GetMapping("/tickets")
    public ResponseEntity<List<TicketReadyResponseDTO>> getAvailableTickets(
            @RequestParam(name = "cpf", required = false) String cpf,
            @AuthenticationPrincipal User user) {

        if (cpf != null && !cpf.trim().isEmpty()) {
            if (!user.getRole().name().equals("ADMIN")) {
                throw new SecurityException("Acesso negado: apenas administradores podem consultar tíquetes de terceiros por CPF.");
            }
            return ResponseEntity.ok(orderService.getTicketsAvailableByCpf(cpf));
        }

        return ResponseEntity.ok(orderService.getTicketsAvailableByCpf(user.getCpf()));
    }

    /**
     * Rota pública para consulta de tíquetes pagos de compradores visitantes/anônimos.
     * Exige a conferência conjunta de CPF e Nome para preservar a privacidade do comprador.
     *
     * @param cpf O CPF informado na compra direta.
     * @param name O Nome completo informado na compra direta.
     * @return Lista contendo os tíquetes disponíveis projetados em {@link TicketReadyResponseDTO} com status 200 OK.
     */
    @GetMapping("/guest")
    public ResponseEntity<List<TicketReadyResponseDTO>> getGuestTickets(
            @RequestParam("cpf") String cpf,
            @RequestParam("name") String name) {
        return ResponseEntity.ok(orderService.getGuestTickets(cpf, name));
    }

    /**
     * Rota autenticada para consultar os detalhes de um pedido específico do estudante.
     *
     * @param id O ID único do pedido.
     * @param user O usuário autenticado.
     * @return Os detalhes do pedido ou erro de permissão (segurança) com status 200 OK.
     */
    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        OrderResponseDTO order = orderService.getOrderById(id, user);
        return ResponseEntity.ok(order);
    }

    /**
     * Rota autenticada para inspeção prévia dos detalhes de um tíquete específico antes da baixa física.
     *
     * @param itemId O identificador único do item de pedido.
     * @param user O usuário autenticado.
     * @return Os dados do tíquete projetados em {@link TicketReadyResponseDTO} com status 200 OK.
     */
    @GetMapping("/items/{itemId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<TicketReadyResponseDTO> getItemById(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User user) {
        TicketReadyResponseDTO ticket = orderService.getItemById(itemId);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Rota de baixa física em lote de múltiplos tíquetes de pedido restrita a administradores.
     *
     * @param request DTO contendo a lista com os UUIDs dos itens a serem trocados.
     * @param user O usuário autenticado (deve possuir privilégio de ADMIN).
     * @return Lista dos itens de pedido atualizados com status EXCHANGED e carimbos de auditoria com status 200 OK.
     */
    @PutMapping("/items/exchange")
    public ResponseEntity<List<OrderResponseDTO.OrderItemResponseDTO>> exchangeItemsBatch(
            @RequestBody @Valid BatchExchangeRequestDTO request,
            @AuthenticationPrincipal User user) {
        List<OrderResponseDTO.OrderItemResponseDTO> response = orderService.exchangeItemsBatch(request.itemIds(), user);
        return ResponseEntity.ok(response);
    }

    /**
     * Rota de baixa e troca física unitária de um item de pedido restrita a administradores.
     *
     * @param itemId O ID do item a ser trocado.
     * @param user O usuário logado (deve possuir privilégio de ADMIN).
     * @return O DTO do item atualizado com o status de trocado com status 200 OK.
     */
    @PutMapping("/items/{itemId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/exchange")
    public ResponseEntity<OrderResponseDTO.OrderItemResponseDTO> exchangeItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User user) {

        if (!user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("Acesso negado: Apenas administradores podem efetuar a baixa de itens.");
        }

        OrderResponseDTO.OrderItemResponseDTO response = orderService.exchangeItem(itemId, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Rota autenticada para o próprio estudante cancelar um pedido pendente de pagamento.
     * Não exige nenhum corpo na requisição (zero payload do front-end).
     *
     * @param id O identificador único do pedido.
     * @param user O usuário autenticado (proprietário do pedido).
     * @return O DTO do pedido atualizado com o status CANCELLED e status 200 OK.
     */
    @PatchMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        OrderResponseDTO response = orderService.cancelOrder(id, user);
        return ResponseEntity.ok(response);
    }
}
