package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link Order}.
 *
 * @author Caio da Silva Viana
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"orderItems"})
    @Override
    List<Order> findAll();

    List<Order> findAllByUser(User user);

    Page<Order> findAllByUser(User user, Pageable pageable);

    List<Order> findByGuestCpf(String guestCpf);

    List<Order> findByCancelledAtIsNullAndCreateAtBefore(Instant cutoffDate);

    default long countByStatus(OrderStatus status) {
        return findAll().stream()
                .filter(order -> order.getStatus() == status)
                .count();
    }
}
