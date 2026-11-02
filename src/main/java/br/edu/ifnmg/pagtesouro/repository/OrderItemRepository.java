package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link OrderItem}.
 *
 * @author Caio da Silva Viana
 */

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT COUNT(i) FROM order_item i WHERE i.status IN (br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus.PAID, br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus.EXCHANGED)")
    long countTotalBoughtTickets();

    @Query("SELECT COUNT(i) FROM order_item i WHERE i.status = br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus.EXCHANGED")
    long countTotalExchangedTickets();
}
