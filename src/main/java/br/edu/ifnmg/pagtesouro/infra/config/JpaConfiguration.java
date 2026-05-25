package br.edu.ifnmg.pagtesouro.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Classe de configuração separada para habilitar os repositórios JPA.
 * <p>
 * **Importância para os Testes:**
 * Manter o `@EnableJpaRepositories` fora da classe principal `TccServerApplication`
 * evita que testes de fatias leves (como `@WebMvcTest` para controllers) tentem inicializar
 * a infraestrutura completa do Hibernate/JPA, resolvendo erros de 'entityManagerFactory' ausente.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Configuration
@EnableJpaRepositories(basePackages = "br.edu.ifnmg.pagtesouro.repository")
public class JpaConfiguration {
}
