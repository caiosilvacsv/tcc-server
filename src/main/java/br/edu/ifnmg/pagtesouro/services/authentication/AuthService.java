package br.edu.ifnmg.pagtesouro.services.authentication;

import br.edu.ifnmg.pagtesouro.domain.auth.dto.AuthResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.AuthSessionDTO;
import br.edu.ifnmg.pagtesouro.domain.user.CpfValidator;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.dto.AuthenticationDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.ChangePasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UpdateProfileRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UserResponseDTO;
import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.InvalidCPFException;
import br.edu.ifnmg.pagtesouro.infra.security.TokenService;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por gerenciar as regras de negócio de autenticação, registro e perfil de usuários.
 *
 * @author Caio da Silva Viana
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
   * Realiza o login do usuário validando as credenciais fornecidas e o status ativo da conta.
   */
  public AuthResponseDTO login(AuthenticationDTO data) {
    var userNamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
    var auth = this.authenticationManager.authenticate(userNamePassword);
    
    if (auth.getPrincipal() instanceof User user) {
      if (!user.isEnabled()) {
        throw new SecurityException("Acesso negado: esta conta de usuário encontra-se desativada.");
      }
      String token = tokenService.generateToken(user);
      return new AuthResponseDTO(token, new AuthSessionDTO(user));
    }
    throw new RuntimeException("Falha ao recuperar o usuário autenticado");
  }

  /**
   * Cadastra um novo usuário no sistema e retorna a sessão mínima e o token JWT para emissão do cookie.
   */
  @Transactional
  public AuthResponseDTO register(RegisterDTO data) {
    String cpf = CpfValidator.normalize(data.cpf());

    if (!CpfValidator.isValid(cpf)) throw new InvalidCPFException();
    if (userRepository.findByCpf(cpf) != null) throw new ConflictException("CPF already exists");
    if (userRepository.findByEmail(data.email()) != null) throw new ConflictException("Email already exists");

    String password = passwordEncoder.encode(data.password());
    var user = new User(
        data.email(),
        password,
        data.name().trim(),
        data.lastName().trim(),
        cpf
    );
    User savedUser = userRepository.save(user);

    String token = tokenService.generateToken(savedUser);
    return new AuthResponseDTO(token, new AuthSessionDTO(savedUser));
  }

  /**
   * Atualiza os dados cadastrais (nome, sobrenome e e-mail) do usuário autenticado (LGPD Art. 18).
   */
  @Transactional
  public UserResponseDTO updateProfile(User user, UpdateProfileRequestDTO dto) {
    if (userRepository.existsByEmailAndIdNot(dto.email().trim(), user.getId())) {
      throw new ConflictException("O e-mail informado já está em uso por outro usuário.");
    }

    user.setName(dto.name().trim());
    user.setLastName(dto.lastName().trim());
    user.setEmail(dto.email().trim());

    User updated = userRepository.save(user);
    return new UserResponseDTO(updated);
  }

  /**
   * Efetua a troca de senha do usuário autenticado, validando a senha atual.
   */
  @Transactional
  public void changePassword(User user, ChangePasswordRequestDTO dto) {
    if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
      throw new IllegalArgumentException("A senha atual informada está incorreta.");
    }

    user.setPassword(passwordEncoder.encode(dto.newPassword()));
    userRepository.save(user);
  }
}
