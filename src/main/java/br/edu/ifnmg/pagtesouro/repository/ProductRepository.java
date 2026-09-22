package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
     *
     * @param title O título do produto/serviço.
     * @return Lista contendo os produtos localizados.
     */
    List<Product> findByTitle(String title);

    /**
     * Verifica se já existe produto cadastrado com o título informado (case-insensitive).
     *
     * @param title O título do produto a ser verificado.
     * @return true se já existir um produto com este título, false caso contrário.
     */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Verifica se já existe outro produto com o título informado, desconsiderando o próprio produto pelo ID (case-insensitive).
     *
     * @param title O título do produto a ser verificado.
     * @param id ID do produto a ser desconsiderado na busca.
     * @return true se já existir outro produto com este título, false caso contrário.
     */
    boolean existsByTitleIgnoreCaseAndIdNot(String title, UUID id);

    /**
     * Retorna a lista de todos os produtos que estão com o status ativo no sistema.
     * Utilizado para alimentar o catálogo de vendas no front-end.
     *
     * @return Lista de produtos ativos.
     */
    List<Product> findAllByActiveTrue();

    /**
     * Retorna a lista de categorias distintas presentes nos produtos cadastrados.
     *
     * @return Lista com as categorias cadastradas na base de dados.
     */
    @Query("SELECT DISTINCT p.category FROM products p WHERE p.category IS NOT NULL")
    List<ProductCategory> findDistinctCategories();

    /**
     * Retorna a lista de categorias distintas presentes em produtos atualmente ATIVOS no sistema.
     *
     * @return Lista com as categorias ativas na vitrine.
     */
    @Query("SELECT DISTINCT p.category FROM products p WHERE p.category IS NOT NULL AND p.active = true")
    List<ProductCategory> findDistinctActiveCategories();
}

