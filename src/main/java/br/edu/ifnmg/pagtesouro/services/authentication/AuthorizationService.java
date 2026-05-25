package br.edu.ifnmg.pagtesouro.services.authentication;

import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Serviço de autorização e autenticação do sistema que implementa a interface {@link UserDetailsService} do Spring Security.
 * <p>
 * **Conceito no TCC (Segurança da Informação e Controle de Acesso):**
 * Esta classe atua como a ponte de integração entre o ecossistema de segurança do Spring Security e a nossa camada
 * de dados (Banco de Dados/PostgreSQL). É responsável por localizar e carregar os dados cadastrais do usuário a partir
 * de sua credencial principal (o e-mail de acesso) para a posterior validação de senhas criptografadas e controle de acessos (roles).
 * </p>
 * <p>
 * O uso de injeção de dependência por construtor segue as melhores práticas da engenharia de software atual,
 * proporcionando maior facilidade para testes unitários isolados e garantindo a imutabilidade do repositório injetado.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Service
public class AuthorizationService implements UserDetailsService {

    private final UserRepository repository;

    /**
     * Construtor da classe com injeção de dependência explícita.
     * Evita injeção por campo direto (@Autowired no atributo), garantindo o desacoplamento e a imutabilidade.
     *
     * @param repository O repositório para acesso aos registros cadastrais de usuário.
     */
    public AuthorizationService(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Localiza os detalhes de um usuário com base em seu e-mail (utilizado como o identificador de login).
     *
     * @param email O e-mail de acesso digitado pelo usuário.
     * @return O registro do usuário populado em conformidade com o contrato {@link UserDetails}.
     * @throws UsernameNotFoundException Se o e-mail informado não corresponder a nenhuma conta cadastrada.
     */
    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        UserDetails user = repository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email);
        }
        return user;
    }
}
