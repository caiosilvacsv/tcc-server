package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.config.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Repositório JPA para operações de persistência de parâmetros dinâmicos de sistema.
 *
 * @author Caio da Silva Viana
 */
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
}
