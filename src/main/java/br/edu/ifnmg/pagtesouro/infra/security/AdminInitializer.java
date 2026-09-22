package br.edu.ifnmg.pagtesouro.infra.security;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

/**
 * Componente do ciclo de vida do Spring Boot responsável por efetuar a inicialização e promoção
 * automática de administradores de teste sob demanda no startup do servidor.
 * <p>
 * **Conceito no TCC (DevOps & Automação):**
 * Evita a necessidade de manipulação direta de SQL no banco de dados para credenciamento do primeiro ADMIN.
 * Lê a propriedade configurada sob a chave {@code api.security.initial-admin-email} (que pode ser
 * mapeada para a variável de ambiente {@code INITIAL_ADMIN_EMAIL}) e promove o usuário correspondente
 * para o privilégio de ADMIN transacionalmente na inicialização.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger LOGGER = Logger.getLogger(AdminInitializer.class.getName());

    private final UserRepository userRepository;

    @Value("${api.security.initial-admin-email:}")
    private String initialAdminEmail;

    public AdminInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (initialAdminEmail != null && !initialAdminEmail.trim().isEmpty()) {
            String email = initialAdminEmail.trim();
            LOGGER.info("TCC DevOps: Verificando promoção automática de administrador para o e-mail: " + email);

            // Realiza a busca segura e faz o cast para a entidade User
            Object userDetails = userRepository.findByEmail(email);
            if (userDetails instanceof User user) {
                if (user.getRole() != UserRoles.ADMIN) {
                    user.setRole(UserRoles.ADMIN);
                    userRepository.save(user);
                    LOGGER.info("TCC DevOps SUCESSO: Usuário " + email + " promovido ao perfil ADMIN com sucesso!");
                } else {
                    LOGGER.info("TCC DevOps: O usuário " + email + " já é um administrador cadastrado.");
                }
            } else {
                LOGGER.warning("TCC DevOps AVISO: Nenhum usuário com e-mail " + email + 
                        " localizado no banco de dados. Cadastre o usuário via '/auth/register' primeiro.");
            }
        }
    }
}
