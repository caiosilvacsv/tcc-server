package br.edu.ifnmg.pagtesouro.services.product;

import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductCategoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductResponseDTO;
import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerenciar a lógica de negócios e transações relacionadas à entidade {@link Product}.
 * <p>
 * **Conceito no TCC:**
 * Modela os produtos, taxas acadêmicas e receitas públicas institucionais como itens cobráveis de catálogo e gerência seu ciclo de vida.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository productRepository) {
        this.repository = productRepository;
    }

    /**
     * Cadastra um novo produto/serviço no catálogo.
     * Impede o cadastro de produtos duplicados com o mesmo título comercial.
     *
     * @param dto Os dados do produto a ser cadastrado.
     * @return O DTO do produto salvo com seu ID gerado.
     * @throws ConflictException se já existir um produto com o mesmo título.
     */
    @Transactional
    public ProductResponseDTO create(ProductRequestDTO dto) {
        String trimmedTitle = dto.title().trim();
        if (repository.existsByTitleIgnoreCase(trimmedTitle)) {
            throw new ConflictException("Já existe um produto cadastrado com o título: " + trimmedTitle);
        }

        Product product = new Product();
        updateEntityFromDto(product, dto);
        Product savedProduct = repository.save(product);
        return new ProductResponseDTO(savedProduct);
    }

    /**
     * Atualiza os dados de um produto existente.
     * Impede a alteração para um título já utilizado por outro produto.
     *
     * @param id O identificador único do produto.
     * @param dto Os novos dados do produto.
     * @return O DTO do produto atualizado e persistido.
     * @throws FindException se o produto não for localizado.
     * @throws ConflictException se o novo título já estiver em uso por outro produto.
     */
    @Transactional
    public ProductResponseDTO update(UUID id, ProductRequestDTO dto) {
        Product product = repository.findById(id)
            .orElseThrow(() -> new FindException("Produto não encontrado!"));

        String trimmedTitle = dto.title().trim();
        if (repository.existsByTitleIgnoreCaseAndIdNot(trimmedTitle, id)) {
            throw new ConflictException("Já existe outro produto cadastrado com o título: " + trimmedTitle);
        }

        updateEntityFromDto(product, dto);
        Product savedProduct = repository.save(product);
        return new ProductResponseDTO(savedProduct);
    }

    /**
     * Localiza um produto pelo seu ID.
     *
     * @param id O ID do produto.
     * @return O DTO correspondente.
     */
    @Transactional(readOnly = true)
    public ProductResponseDTO findById(UUID id) {
        Product product = repository.findById(id)
            .orElseThrow(() -> new FindException("Produto não encontrado!"));
        if (!product.isActive()) {
            throw new FindException("Produto não encontrado!");
        }
        return new ProductResponseDTO(product);
    }

    /**
     * Localiza todos os produtos que possuem o título comercial exato fornecido.
     * Suporta múltiplos registros com títulos duplicados em conformidade com as regras do banco.
     *
     * @param title O título do produto.
     * @return Lista de DTOs dos produtos localizados.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findByTitle(String title) {
        List<Product> products = repository.findByTitle(title);
        if (products.isEmpty()) {
            throw new FindException("Nenhum produto localizado com o título: " + title);
        }
        return products.stream()
            .map(ProductResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Retorna todos os produtos cadastrados no banco de dados.
     *
     * @return Lista com todos os produtos.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAll() {
        return repository.findAll().stream()
            .map(ProductResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Retorna apenas os produtos ativos no catálogo de vendas.
     *
     * @return Lista com todos os produtos ativos.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAllActive() {
        return repository.findAllByActiveTrue().stream()
            .map(ProductResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Desativa logicamente um produto (Soft Delete) marcando seu status active como false.
     * Mantém o histórico no banco de dados, conforme as regras de auditoria financeira do TCC.
     *
     * @param id O identificador do produto a ser deletado.
     */
    @Transactional
    public void delete(UUID id) {
        Product product = repository.findById(id)
            .orElseThrow(() -> new FindException("Produto não encontrado!"));
        product.setActive(false);
        repository.save(product);
    }

    /**
     * Retorna a lista de categorias distintas presentes nos produtos cadastrados na base de dados.
     *
     * @param activeOnly Se verdadeiro, filtra apenas categorias que possuem produtos com active = true.
     * @return Lista contendo os DTOs das categorias encontradas.
     */
    @Transactional(readOnly = true)
    public List<ProductCategoryResponseDTO> findCategories(boolean activeOnly) {
        List<ProductCategory> categories = activeOnly
            ? repository.findDistinctActiveCategories()
            : repository.findDistinctCategories();

        return categories.stream()
            .map(ProductCategoryResponseDTO::new)
            .collect(Collectors.toList());
    }

    /**
     * Mapeia os dados do DTO de entrada para atualizar a entidade do banco de dados.
     */
    private void updateEntityFromDto(Product product, ProductRequestDTO dto) {
        product.setTitle(dto.title().trim());
        product.setDescription(dto.description());
        product.setImage(dto.image());
        product.setPrice(dto.price());
        product.setActive(dto.active());
        product.setCodeService(dto.codeService().trim());
        product.setCategory(dto.category());
    }
}
