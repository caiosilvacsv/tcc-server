package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.dto.AuthenticationDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.LoginResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UserResponseDTO;
import br.edu.ifnmg.pagtesouro.services.authentication.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST responsável por expor as rotas públicas de autenticação,
 * controle de acesso e registro de novos usuários no sistema.
 * <p>
 * **Conceito no TCC (Segurança e Criptografia):**
 * Centraliza os endpoints de segurança stateless. Realiza o login seguro de alunos/servidores e
 * o registro com criptografia e validação de CPF matemática em conformidade com as regras
 * de integridade de dados.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

  private final AuthService authService;

  public AuthenticationController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Rota pública para autenticação de credenciais de usuários cadastrados.
   * Valida o e-mail e senha e retorna um token de segurança JWT HMAC256.
   *
   * @param data O DTO contendo e-mail e senha de login.
   * @return Um ResponseEntity contendo o DTO com o token JWT gerado e status 200 OK.
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
    return ResponseEntity.ok(authService.login(data));
  }

  /**
   * Rota pública para cadastro e registro de novos contribuintes/alunos no portal.
   * Efetua a criptografia de senha e a validação rígida de CPF antes da gravação física.
   *
   * @param data O DTO contendo os dados de registro.
   * @return Um ResponseEntity contendo o token JWT gerado para login automático e status 200 OK.
   */
  @PostMapping("/register")
  public ResponseEntity<LoginResponseDTO> register (@RequestBody @Valid RegisterDTO data){
    return ResponseEntity.ok(authService.register(data));
  }

  /**
   * Rota privada para obter os dados cadastrais do usuário atualmente autenticado.
   *
   * @param user Usuário autenticado obtido através do contexto de segurança JWT.
   * @return Um ResponseEntity contendo os dados do perfil em UserResponseDTO e status 200 OK.
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(new UserResponseDTO(user));
  }
}

