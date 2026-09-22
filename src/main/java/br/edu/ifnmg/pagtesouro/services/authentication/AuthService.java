package br.edu.ifnmg.pagtesouro.services.authentication;

import br.edu.ifnmg.pagtesouro.domain.user.CpfValidator;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.dto.AuthenticationDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.LoginResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.InvalidCPFException;
import br.edu.ifnmg.pagtesouro.infra.security.TokenService;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por gerenciar as regras de negócio de autenticação e registro de usuários.
 * <p>
 * **Responsabilidades:**
 * <ul>
 *   <li>Autenticação de credenciais via {@link AuthenticationManager} (Spring Security).</li>
 *   <li>Criptografia de senhas no cadastro utilizando {@link PasswordEncoder} (BCrypt).</li>
 *   <li>Verificação de duplicidade de e-mails para novos cadastros.</li>
 *   <li>Geração e emissão de Tokens JWT de acesso seguro via {@link TokenService}.</li>
 * </ul>
 * </p>
 */
@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public AuthService(
      AuthenticationManager authenticationManager,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService
    ) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  /**
   * Realiza o login do usuário validando as credenciais fornecidas.
   * Se bem-sucedido, gera e retorna um Token JWT válido de acesso.
   *
   * @param data DTO contendo e-mail e senha de login
   * @return DTO contendo o token JWT gerado
   * @throws RuntimeException se o principal de autenticação for inválido
   */
  public LoginResponseDTO login(AuthenticationDTO data) {
    var userNamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
    var auth = this.authenticationManager.authenticate(userNamePassword);
    
    // Uso do Pattern Matching para garantir a segurança de tipos
    if (auth.getPrincipal() instanceof User user) {
      String token = tokenService.generateToken(user);
      return new LoginResponseDTO(token);
    }
    throw new RuntimeException("Falha ao recuperar o usuário autenticado");
  }

  /**
   * Cadastra um novo usuário no sistema e retorna um token de autenticação JWT.
   * <p>
   * **Validação e Normalização:**
   * O CPF fornecido é normalizado antes da validação e da persistência (removendo pontos,
   * traços e espaços), garantindo que formatos como {@code 123.456.789-09} sejam persistidos
   * de maneira limpa apenas com os dígitos numéricos: {@code 12345678909}.
   * </p>
   *
   * @param data DTO contendo os dados de registro recebidos na requisição (e-mail, senha, nome, CPF)
   * @return DTO contendo o token de autenticação JWT gerado para login automático
   * @throws InvalidCPFException se o CPF fornecido for considerado matematicamente inválido
   * @throws ConflictException se o CPF informado já estiver cadastrado no sistema
   * @throws ConflictException se o e-mail informado já estiver cadastrado no sistema
   */
  public LoginResponseDTO register(RegisterDTO data) {
    String cpf = CpfValidator.normalize(data.cpf());

    if (!CpfValidator.isValid(cpf)) throw new InvalidCPFException();
    if (userRepository.findByCpf(cpf) != null) throw new ConflictException("CPF already exists");
    if (userRepository.findByEmail(data.email()) != null) throw new ConflictException("Email already exists");

    String password = passwordEncoder.encode(data.password());
    var user = new User(
        data.email(),
        password,
        data.name(),
        data.last_name(),
        cpf
    );
    userRepository.save(user);

    String token = tokenService.generateToken(user);
    return new LoginResponseDTO(token);
  }
}
