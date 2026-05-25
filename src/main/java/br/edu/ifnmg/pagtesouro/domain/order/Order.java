package br.edu.ifnmg.pagtesouro.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.product.MoneyToCentsConverter;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entidade JPA que representa um Pedido de compra de tíquetes ou quitação de multas (cabeçalho).
 * <p>
 * **Conceito no TCC (Portal de Débitos):**
 * Atua como um "carrinho" agrupador contendo itens de cobrança. Por conta da normalização, o status
 * e as datas de liquidação são controlados de forma granular nos itens (OrderItem). O status geral
 * do pedido é calculado dinamicamente com base no estado de seus itens, otimizando o modelo de
 * banco de dados e evitando redundâncias (Rich Domain Model).
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
@Entity(name = "orders")
public class Order {

    /**
     * Identificador único do pedido gerado pelo backend.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * O estudante ou usuário do IFNMG associado a este pedido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Valor total consolidado em centavos de todos os itens do pedido.
     */
    @Column(name = "total_amount", nullable = false)
    @Convert(converter = MoneyToCentsConverter.class)
    private BigDecimal totalAmount;

    /**
     * Relacionamento bidirecional Um-para-Muitos com os itens individuais deste pedido.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    /**
     * Data e hora de criação do pedido. Gerada automaticamente na persistência.
     */
    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Instant createAt;

    /**
     * Data e hora de modificação ou atualização do pedido.
     */
    @UpdateTimestamp
    @Column(name = "update_at")
    private Instant updateAt;

    /**
     * Data e hora em que o pedido foi cancelado (por expiração ou desistência).
     */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Retorna o status consolidado do pedido calculado dinamicamente com base nos itens.
     * Propriedade não-persistida no banco de dados (@Transient) para evitar redundâncias físicas.
     *
     * @return O {@link OrderStatus} correspondente ao estado dos itens
     */
    @Transient
    public OrderStatus getStatus() {
        if (orderItems == null || orderItems.isEmpty()) {
            return OrderStatus.CREATED;
        }

        boolean hasPending = false;
        boolean hasPaid = false;
        boolean hasCancelled = false;

        for (OrderItem item : orderItems) {
            if (item.getStatus() == OrderItemStatus.PENDING) {
                hasPending = true;
            } else if (item.getStatus() == OrderItemStatus.PAID) {
                hasPaid = true;
            } else if (item.getStatus() == OrderItemStatus.CANCELLED) {
                hasCancelled = true;
            }
        }

        // Se todos os itens estão cancelados
        if (hasCancelled && !hasPending && !hasPaid) {
            return OrderStatus.CANCELLED;
        }

        // Se pelo menos um item foi pago, verifica se há outros pendentes
        if (hasPaid) {
            return hasPending ? OrderStatus.PARTIALLY_PAID : OrderStatus.COMPLETED;
        }

        // Estado padrão: aguardando pagamento
        return OrderStatus.PENDING_PAYMENT;
    }
}

