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
    /**
     * Recupera todos os pedidos cadastrados no banco de dados vinculados a um usuário específico.
     * Utilizado para alimentar o histórico acadêmico de compras/cobranças do estudante logado.
     *
     * @param user A entidade do usuário estudante autenticado requisitante
     * @return Uma lista de entidades {@link Order} pertencentes a este estudante
     */
    List<Order> findAllByUser(User user);
}

