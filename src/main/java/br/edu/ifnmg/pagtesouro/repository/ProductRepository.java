package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link Product}.
 * <p>
 * **Conceito no TCC:**
 * Abstrai a persistência e consulta dos itens cobráveis do IFNMG.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    /**
     * Localiza produtos pelo seu título comercial exato.
     * Como o título não é único no banco, pode retornar múltiplos registros (ex: históricos ou preços diferentes).
     *
     * @param title O título do produto/serviço.
     * @return Lista contendo os produtos localizados.
     */
    List<Product> findByTitle(String title);

    /**
     * Retorna a lista de todos os produtos que estão com o status ativo no sistema.
     * Utilizado para alimentar o catálogo de vendas no front-end.
     *
     * @return Lista de produtos ativos.
     */
    List<Product> findAllByActiveTrue();
}

