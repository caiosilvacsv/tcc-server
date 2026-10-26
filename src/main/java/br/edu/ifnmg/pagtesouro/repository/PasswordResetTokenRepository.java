package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.auth.PasswordResetToken;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para busca e controle de tokens de redefinição de senha.
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}
