package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductResponseDTO;
import br.edu.ifnmg.pagtesouro.services.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST responsável por expor as rotas de gerenciamento do catálogo de Produtos e Serviços cobráveis do IFNMG.
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /**
     * Rota pública para listagem de produtos.
     * <p>
     * Se o solicitante for um administrador (ADMIN) e activeOnly for false, retorna todos os produtos (ativos e inativos).
     * Caso contrário (estudante comum ou anônimo), retorna obrigatoriamente apenas os produtos ativos.
     * </p>
     *
     * @param activeOnly Define se retorna apenas produtos ativos (padrão true).
     * @return Lista com os DTOs dos produtos correspondentes.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAll(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<ProductResponseDTO> products;
        if (isAdmin && !activeOnly) {
            products = service.findAll();
        } else {
            products = service.findAllActive();
        }

        return ResponseEntity.ok(products);
    }

    /**
     * Rota pública para buscar um produto específico pelo seu ID.
     *
     * @param id O ID único do produto.
     * @return Os detalhes do produto com status 200 OK.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getById(@PathVariable UUID id) {
        ProductResponseDTO product = service.findById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Rota privada (apenas ADMIN) para cadastrar um novo produto.
     *
     * @param request O DTO contendo os dados do novo produto.
     * @return O produto criado com status 201 Created.
     */
    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(@RequestBody @Valid ProductRequestDTO request) {
        ProductResponseDTO product = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Rota privada (apenas ADMIN) para atualizar os dados de um produto existente.
     *
     * @param id O ID único do produto a ser editado.
     * @param request O DTO contendo os novos dados do produto.
     * @return O produto atualizado com status 200 OK.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable UUID id,
            @RequestBody @Valid ProductRequestDTO request) {
        ProductResponseDTO product = service.update(id, request);
        return ResponseEntity.ok(product);
    }

    /**
     * Rota privada (apenas ADMIN) para desativar logicamente um produto (Soft Delete).
     *
     * @param id O ID único do produto a ser deletado.
     * @return Resposta vazia com status 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
