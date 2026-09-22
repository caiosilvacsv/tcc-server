package br.edu.ifnmg.pagtesouro.services.order;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.OrderItemRepository;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerenciar as regras de negócio e o ciclo de vida da entidade {@link Order}.
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    /**
     * Construtor responsável por inicializar as dependências de persistência do serviço de pedidos.
     *
     * @param orderRepository      Repositório para consulta e gravação física dos pedidos e seus status.
     * @param productRepository    Repositório para validar a existência, preço e disponibilidade dos itens.
     * @param orderItemRepository  Repositório para persistir os itens individuais associados ao pedido.
     * @param userRepository       Repositório para consulta direta indexada de usuários por CPF.
     */
    public OrderService(
        OrderRepository orderRepository,
        ProductRepository productRepository,
        OrderItemRepository orderItemRepository,
        UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Cria um novo pedido consolidando os itens selecionados e calculando os valores totais.
     * <p>
     * Valida se os produtos solicitados existem e estão ativos no catálogo antes de persistir.
     * </p>
     * @param dto Os itens e quantidades de produtos que o usuário selecionou.
     * @param user O usuário autenticado solicitante.
     * @return O DTO do pedido salvo e consolidado com cálculos seguros do banco.
     * @throws FindException    Caso algum dos produtos não seja localizado.
     * @throws ConflictException Se algum dos produtos não estiver ativo no catálogo de vendas
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setCreateAt(Instant.now());
        order.setOrderItems(new ArrayList<>());

        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        // Varre a lista de itens recebidos no DTO
        for (OrderRequestDTO.items itemDto : dto.itemsList()) {
            Product product = productRepository.findById(itemDto.productId())
                .orElseThrow(() -> new FindException("Produto com ID " + itemDto.productId() + " não localizado."));

            if(!product.isActive())
                throw new ConflictException("Produto com ID " + itemDto.productId() + " não está disponível.");

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());

            // Calcula o valor total do item de forma segura (preço real * quantidade)
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.quantity()));
            item.setTotalAmount(itemTotal);
            item.setStatus(OrderItemStatus.PENDING); // Nasce pendente de pagamento

            order.getOrderItems().add(item);
            totalOrderAmount = totalOrderAmount.add(itemTotal);
        }

        order.setTotalAmount(totalOrderAmount);
        Order savedOrder = orderRepository.save(order);
        return new OrderResponseDTO(savedOrder);
    }

    /**
     * Retorna o histórico de pedidos completo efetuados pelo estudante logado.
     *
     * @param user O usuário autenticado.
     * @return Lista contendo os DTOs dos pedidos correspondentes.
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(User user) {
        return orderRepository.findAllByUser(user).stream()
            .map(OrderResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Busca um pedido específico pelo seu ID único.
     * Realiza a validação de segurança para garantir que apenas o proprietário do pedido ou administradores acessem.
     *
     * @param orderId O ID único do pedido buscado.
     * @param user O usuário solicitante (para validação de segurança).
     * @return O DTO do pedido localizado.
     * @throws SecurityException se o usuário não for o dono do pedido ou ADMIN.
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new FindException("Pedido não encontrado!"));

        // Proteção de segurança do IFNMG: apenas o dono ou um ADMIN podem ler
        if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("Acesso negado a este pedido.");
        }

        return new OrderResponseDTO(order);
    }

    /**
     * Efetua a baixa e troca física de um item de pedido (tíquete ou serviço) que já foi pago.
     * Restrito para validações de negócios governamentais e controle contra duplo consumo.
     *
     * @param itemId O identificador único global (UUID) do item de pedido a ser trocado.
     * @param admin O usuário administrador que está realizando o atendimento e efetuando a baixa.
     * @return O DTO representativo do item de pedido atualizado com o status de trocado.
     * @throws FindException se o item de pedido não for localizado no banco de dados.
     * @throws IllegalStateException se o item não estiver no status PAID (não pago ou já trocado).
     */
    @Transactional
    public OrderResponseDTO.OrderItemResponseDTO exchangeItem(UUID itemId, User admin) {
        OrderItem item = orderItemRepository.findById(itemId)
            .orElseThrow(() -> new FindException("Item de pedido com ID " + itemId + " não localizado."));

        if (item.getStatus() != OrderItemStatus.PAID) {
            if (item.getStatus() == OrderItemStatus.EXCHANGED) {
                throw new IllegalStateException("Falha na baixa física: Este tíquete/serviço já foi trocado anteriormente.");
            }
            throw new IllegalStateException("Falha na baixa física: O item do pedido não está em estado PAGO (Status atual: " + item.getStatus() + ").");
        }

        item.setStatus(OrderItemStatus.EXCHANGED);
        item.setExchangedAt(Instant.now());
        item.setExchangedBy(admin.getId());

        OrderItem savedItem = orderItemRepository.save(item);
        return new OrderResponseDTO.OrderItemResponseDTO(savedItem);
    }

    /**
     * Resgata a lista unificada de tíquetes quitados e disponíveis para retirada vinculados a um CPF (estudante ou visitante).
     * <p>
     * Os tíquetes são organizados em fila cronológica <b>FIFO (First-In, First-Out)</b>, ordenados do mais
     * antigo para o mais novo com base na data de liquidação ({@code paidAt}), garantindo que os créditos
     * adquiridos há mais tempo sejam consumidos primeiro.
     * </p>
     *
     * @param cpf O CPF do comprador a ser pesquisado.
     * @return Lista de {@link TicketReadyResponseDTO} contendo apenas itens com status {@link OrderItemStatus#PAID}.
     */
    @Transactional(readOnly = true)
    public List<TicketReadyResponseDTO> getTicketsAvailableByCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            throw new IllegalArgumentException("O CPF é obrigatório para a consulta de tíquetes.");
        }

        String cleanedCpf = cpf.trim();
        List<Order> orders = new ArrayList<>(orderRepository.findByGuestCpf(cleanedCpf));
        UserDetails userDetails = userRepository.findByCpf(cleanedCpf);
        if (userDetails instanceof User registeredUser) {
            orders.addAll(orderRepository.findAllByUser(registeredUser));
        }

        return orders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .filter(item -> item.getStatus() == OrderItemStatus.PAID)
            .sorted(java.util.Comparator.comparing(
                OrderItem::getPaidAt,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
            ))
            .map(TicketReadyResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Resgata os tíquetes quitados de um comprador visitante/anônimo mediante validação de CPF e Nome.
     * <p>
     * A validação combinada de CPF e Nome protege a privacidade do comprador, impedindo que terceiros
     * visualizem os tíquetes conhecendo apenas o CPF.
     * </p>
     *
     * @param cpf O CPF informado no checkout direto.
     * @param name O Nome completo informado no checkout direto.
     * @return Lista de {@link TicketReadyResponseDTO} dos itens pagos em ordem cronológica (FIFO).
     * @throws SecurityException se o nome informado não corresponder ao nome registrado no pedido.
     */
    @Transactional(readOnly = true)
    public List<TicketReadyResponseDTO> getGuestTickets(String cpf, String name) {
        if (cpf == null || cpf.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("CPF e Nome são obrigatórios para a consulta de tíquetes de visitante.");
        }

        String cleanedCpf = cpf.trim();
        String cleanedName = name.trim().toLowerCase();

        List<Order> orders = orderRepository.findByGuestCpf(cleanedCpf);

        boolean nameMatches = orders.stream()
            .anyMatch(order -> order.getGuestName() != null && order.getGuestName().trim().toLowerCase().contains(cleanedName));

        if (!orders.isEmpty() && !nameMatches) {
            throw new SecurityException("Os dados informados não conferem com o cadastro do pedido.");
        }

        return orders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .filter(item -> item.getStatus() == OrderItemStatus.PAID)
            .sorted(java.util.Comparator.comparing(
                OrderItem::getPaidAt,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
            ))
            .map(TicketReadyResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Efetua a baixa física em lote de múltiplos tíquetes de pedido de forma atômica.
     * <p>
     * Garante a integridade transacional: ou todos os tíquetes selecionados são baixados com sucesso,
     * ou a operação inteira sofre rollback caso algum item já tenha sido trocado ou não esteja quitado.
     * </p>
     *
     * @param itemIds Lista contendo os identificadores únicos (UUIDs) dos itens a serem trocados.
     * @param admin O usuário administrador responsável pelo atendimento no balcão.
     * @return Lista contendo a projeção {@link OrderResponseDTO.OrderItemResponseDTO} atualizada de cada item trocado.
     * @throws FindException se um ou mais itens não forem localizados no banco de dados.
     * @throws IllegalStateException se algum dos itens não estiver no status {@link OrderItemStatus#PAID}.
     */
    @Transactional
    public List<OrderResponseDTO.OrderItemResponseDTO> exchangeItemsBatch(List<UUID> itemIds, User admin) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("A lista de itens para baixa não pode ser nula ou vazia.");
        }

        List<OrderItem> items = orderItemRepository.findAllById(itemIds);

        if (items.size() != itemIds.size()) {
            throw new FindException("Um ou mais itens informados para baixa não foram localizados no sistema.");
        }

        Instant now = Instant.now();
        for (OrderItem item : items) {
            if (item.getStatus() != OrderItemStatus.PAID) {
                if (item.getStatus() == OrderItemStatus.EXCHANGED) {
                    throw new IllegalStateException("Falha na baixa física: O item '" + item.getProduct().getTitle() + "' (ID: " + item.getId() + ") já foi trocado anteriormente.");
                }
                throw new IllegalStateException("Falha na baixa física: O item '" + item.getProduct().getTitle() + "' (ID: " + item.getId() + ") não está em estado PAGO (Status: " + item.getStatus() + ").");
            }
            item.setStatus(OrderItemStatus.EXCHANGED);
            item.setExchangedAt(now);
            item.setExchangedBy(admin.getId());
        }

        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
        return savedItems.stream()
            .map(OrderResponseDTO.OrderItemResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Resgata a projeção detalhada de um tíquete específico através de seu identificador único global (UUID).
     *
     * @param itemId O identificador único do item de pedido.
     * @return A projeção {@link TicketReadyResponseDTO} do tíquete correspondente.
     * @throws FindException se o item não for localizado.
     */
    @Transactional(readOnly = true)
    public TicketReadyResponseDTO getItemById(UUID itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
            .orElseThrow(() -> new FindException("Item de pedido com ID " + itemId + " não localizado."));
        return new TicketReadyResponseDTO(item);
    }

    /**
     * Efetua o cancelamento de um pedido e de seus itens pendentes.
     * <p>
     * Se invocado por um usuário (aluno), valida se o pedido pertence a ele.
     * Executa o método de domínio {@link Order#cancel()} e persiste as alterações.
     * </p>
     *
     * @param orderId O identificador único do pedido.
     * @param user O usuário autenticado (ou null se invocado internamente pelo sistema).
     * @return O DTO {@link OrderResponseDTO} atualizado com status CANCELLED.
     * @throws FindException se o pedido não for localizado.
     * @throws SecurityException se o usuário tentar cancelar pedido de outro titular.
     * @throws IllegalStateException se o pedido já estiver pago ou previamente cancelado.
     */
    @Transactional
    public OrderResponseDTO cancelOrder(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new FindException("Pedido com ID " + orderId + " não localizado."));

        if (user != null && order.getUser() != null) {
            UUID orderUserId = order.getUser().getId();
            UUID requestingUserId = user.getId();
            if (orderUserId != null && requestingUserId != null && !orderUserId.equals(requestingUserId)) {
                throw new SecurityException("Acesso negado: você só pode cancelar os seus próprios pedidos.");
            }
        }

        order.cancel();
        Order savedOrder = orderRepository.save(order);
        return new OrderResponseDTO(savedOrder);
    }

    /**
     * Rotina executada periodicamente pelo Daemon para cancelamento automático de pedidos abandonados.
     * Localiza pedidos criados há mais de {@code expirationDays} dias que ainda não foram cancelados
     * e cancela aqueles que permanecem em estado pendente.
     *
     * @param expirationDays Prazo em dias para expiração de pedidos abandonados.
     * @return Quantidade de pedidos cancelados pelo processo.
     */
    @Transactional
    public int cancelAbandonedOrders(int expirationDays) {
        Instant cutoffDate = Instant.now().minusSeconds(expirationDays * 86400L);
        List<Order> eligibleOrders = orderRepository.findByCancelledAtIsNullAndCreateAtBefore(cutoffDate);

        int cancelledCount = 0;
        for (Order order : eligibleOrders) {
            try {
                OrderStatus status = order.getStatus();
                if (status == OrderStatus.CREATED || status == OrderStatus.PENDING_PAYMENT) {
                    order.cancel();
                    orderRepository.save(order);
                    cancelledCount++;
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
        return cancelledCount;
    }
}
