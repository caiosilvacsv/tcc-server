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
 * Serviço responsável por gerenciar o ciclo de vida e regras de negócio da entidade {@link Order}.
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
     * Cria um novo pedido (carrinho de compras) para o usuário autenticado.
     *
     * @param dto Os itens e quantidades solicitados.
     * @param user O usuário/estudante autenticado que realiza a solicitação.
     * @return O DTO do pedido salvo e populado.
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO dto, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderItems(new ArrayList<>());

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        
        for (OrderRequestDTO.ItemRequestDTO itemDto : dto.items()) {
            Product product = productRepository.findById(itemDto.productId())
                .orElseThrow(() -> new FindException("Produto com ID " + itemDto.productId() + " não localizado."));
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDto.quantity());
            
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.quantity()));
            item.setTotalAmount(itemTotal);
            item.setStatus(OrderItemStatus.PENDING);
            
            order.getOrderItems().add(item);
            totalOrderAmount = totalOrderAmount.add(itemTotal);
        }

        order.setTotalAmount(totalOrderAmount);
        Order savedOrder = orderRepository.save(order);
        return new OrderResponseDTO(savedOrder);
    }

    /**
     * Retorna o histórico de pedidos efetuados pelo usuário autenticado.
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
     * Localiza um pedido específico pelo seu ID único.
     * Realiza proteção de leitura para garantir que apenas o proprietário ou administradores acessem.
     *
     * @param orderId O ID do pedido buscado.
     * @param user O usuário solicitante (para validação de segurança).
     * @return O DTO do pedido localizado.
     * @throws SecurityException se o usuário não for o dono do pedido ou ADMIN.
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID orderId, User user) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new FindException("Pedido não encontrado!"));
        
        // Proteção: apenas o próprio estudante ou um ADMIN pode ler o pedido
        if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("Acesso negado ao pedido.");
        }
        
        return new OrderResponseDTO(order);
    }
}
