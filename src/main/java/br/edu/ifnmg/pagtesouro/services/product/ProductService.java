package br.edu.ifnmg.pagtesouro.services.product;

import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductResponseDTO;
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
 * Modela os produtos, taxas acadêmicas e receitas públicas institucionais como itens cobráveis de catálogo e gerencia seu ciclo de vida.
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
     *
     * @param dto Os dados do produto a ser cadastrado.
     * @return O DTO do produto salvo com seu ID gerado.
     */
    @Transactional
    public ProductResponseDTO create(ProductRequestDTO dto) {
        Product product = new Product();
        updateEntityFromDto(product, dto);
        Product savedProduct = repository.save(product);
        return new ProductResponseDTO(savedProduct);
    }

    /**
     * Atualiza os dados de um produto existente.
     *
     * @param id O identificador único do produto.
     * @param dto Os novos dados do produto.
     * @return O DTO do produto atualizado e persistido.
     */
    @Transactional
    public ProductResponseDTO update(UUID id, ProductRequestDTO dto) {
        Product product = repository.findById(id)
            .orElseThrow(() -> new FindException("Produto não encontrado!"));
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
     * Mantém o histórico no banco de dados, em conformidade com as regras de auditoria financeira do Siafi/TCC.
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
     * Mapeia os dados do DTO de entrada para atualizar a entidade do banco de dados.
     */
    private void updateEntityFromDto(Product product, ProductRequestDTO dto) {
        product.setTitle(dto.title());
        product.setDescription(dto.description());
        product.setImage(dto.image());
        product.setPrice(dto.price());
        product.setActive(dto.active());
        product.setCodeService(dto.codeService());
        product.setCategory(dto.category());
    }
}
