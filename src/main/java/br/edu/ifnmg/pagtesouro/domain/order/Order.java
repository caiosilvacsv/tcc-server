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
     * O estudante ou usuário institucional do IFNMG associado a este pedido.
     * Permite valor nulo (null) para pedidos gerados através de checkout direto por visitantes externos.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * CPF ou CNPJ informado pelo visitante externo no checkout direto.
     * Utilizado para localização do pedido e conferência cadastral no balcão de atendimento.
     */
    @Column(name = "guest_cpf", length = 14)
    private String guestCpf;

    /**
     * Nome completo informado pelo visitante externo no checkout direto.
     */
    @Column(name = "guest_name", length = 45)
    private String guestName;

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

    /**
     * Retorna o enquadramento do comprador com base na presença de usuário autenticado.
     *
     * @return {@link BuyerType#STUDENT} se houver usuário vinculado, ou {@link BuyerType#GUEST} caso contrário.
     */
    @Transient
    public BuyerType getBuyerType() {
        return this.user != null ? BuyerType.STUDENT : BuyerType.GUEST;
    }

    /**
     * Retorna o CPF do comprador de forma unificada, priorizando o usuário autenticado.
     *
     * @return O CPF do estudante ou do visitante convidado.
     */
    @Transient
    public String getBuyerCpf() {
        return this.user != null ? this.user.getCpf() : this.guestCpf;
    }

    /**
     * Retorna o nome completo do comprador de forma unificada, priorizando o usuário autenticado.
     *
     * @return O nome do estudante ou do visitante convidado.
     */
    @Transient
    public String getBuyerName() {
        if (this.user != null) {
            String name = this.user.getName();
            if (this.user.getLastName() != null && !this.user.getLastName().trim().isEmpty()) {
                name += " " + this.user.getLastName().trim();
            }
            return name;
        }
        return this.guestName;
    }

    /**
     * Efetua o cancelamento do pedido e de todos os seus itens pendentes.
     * <p>
     * **Regra de Negócio (DDD):**
     * - Não permite o cancelamento se o pedido já possuir itens pagos ou resgatados.
     * - Não permite cancelar um pedido que já se encontra cancelado (idempotência).
     * - Marca os itens pendentes como {@link OrderItemStatus#CANCELLED} e preenche {@code cancelledAt}.
     * </p>
     *
     * @throws IllegalStateException se o pedido já estiver pago ou previamente cancelado.
     */
    public void cancel() {
        OrderStatus currentStatus = this.getStatus();
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.PARTIALLY_PAID) {
            throw new IllegalStateException("Não é permitido cancelar um pedido que já possui itens pagos ou resgatados.");
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Este pedido já se encontra cancelado.");
        }
        this.cancelledAt = Instant.now();
        if (this.orderItems != null) {
            for (OrderItem item : this.orderItems) {
                if (item.getStatus() == OrderItemStatus.PENDING) {
                    item.setStatus(OrderItemStatus.CANCELLED);
                }
            }
        }
    }
}

