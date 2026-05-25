package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link User}.
 * <p>
 * **Conceito no TCC:**
 * Abstrai o acesso aos dados cadastrais dos estudantes e administradores.
 * Fornece métodos utilitários essenciais para o fluxo de autenticação do Spring Security.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Localiza um usuário pelo seu e-mail de acesso. Utilizado no fluxo de login e validação JWT.
     *
     * @param email O e-mail de login do usuário
     * @return O {@link UserDetails} correspondente se localizado; {@code null} caso contrário
     */
    UserDetails findByEmail(String email);

    /**
     * Localiza um usuário pelo seu CPF de contribuinte.
     *
     * @param cpf O CPF do contribuinte (apenas dígitos)
     * @return O {@link UserDetails} correspondente se localizado; {@code null} caso contrário
     */
    UserDetails findByCpf(String cpf);
}

