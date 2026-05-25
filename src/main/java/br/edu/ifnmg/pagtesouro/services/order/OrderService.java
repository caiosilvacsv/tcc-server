package br.edu.ifnmg.pagtesouro.services.order;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerenciar as regras de negócio e o ciclo de vida da entidade {@link Order}.
 *
 * @author Caio da Silva Viana
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Cria um novo pedido (carrinho de compras) associado de forma segura ao estudante logado.
     *
     * @param dto Os itens e quantidades de produtos que o usuário selecionou.
     * @param user O usuário autenticado solicitante.
     * @return O DTO do pedido salvo e consolidado com cálculos seguros do banco.
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderItems(new ArrayList<>());

        BigDecimal totalOrderAmount = BigDecimal.ZERO;

        // Varre a lista de itens recebidos no DTO
        for (OrderRequestDTO.items itemDto : dto.itemsList()) {
            Product product = productRepository.findById(itemDto.productId())
                .orElseThrow(() -> new FindException("Produto com ID " + itemDto.productId() + " não localizado."));

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
}
