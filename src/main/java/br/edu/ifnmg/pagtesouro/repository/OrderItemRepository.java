package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link OrderItem}.
 * <p>
 * **Conceito no TCC:**
 * Gerencia a gravação e leitura granular dos itens individuais dos pedidos, permitindo o controle de status de quitação.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
