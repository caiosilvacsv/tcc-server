package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import java.util.List;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link Order}.
 * <p>
 * **Conceito no TCC:**
 * Gerencia a gravação e leitura do cabeçalho dos carrinhos/pedidos de tíquetes e multas dos estudantes.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findAllByUser(User user);
}

