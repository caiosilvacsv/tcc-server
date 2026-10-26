package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.auth.dto.AuthResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.AuthSessionDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.ForgotPasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.ResetPasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.dto.*;
import br.edu.ifnmg.pagtesouro.services.auth.PasswordResetService;
import br.edu.ifnmg.pagtesouro.services.authentication.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * Controlador REST responsável por expor as rotas de autenticação,
 * controle de acesso, retificação de dados (LGPD) e recuperação de senha.
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

  private final AuthService authService;
  private final PasswordResetService passwordResetService;

  @Value("${api.security.token.expiration-time-in-minutes:20}")
  private long expirationMinutes;

  public AuthenticationController(AuthService authService, PasswordResetService passwordResetService) {
    this.authService = authService;
    this.passwordResetService = passwordResetService;
  }

  /**
   * Rota pública para autenticação.
   * Emite estritamente um Cookie seguro HttpOnly ("accessToken") para mitigar ataques XSS.
   * O corpo JSON retorna apenas o DTO de sessão mínima (AuthSessionDTO: name, lastName, role),
   * em conformidade com o princípio de minimização da LGPD (sem CPF ou e-mail no payload).
   */
  @PostMapping("/login")
  public ResponseEntity<AuthSessionDTO> login(@RequestBody @Valid AuthenticationDTO data){
    AuthResponseDTO response = authService.login(data);
    ResponseCookie cookie = buildAuthCookie(response.token(), Duration.ofMinutes(expirationMinutes));

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(response.session());
  }

  /**
   * Rota pública para cadastro de novos contribuintes/alunos.
   * Emite estritamente um Cookie seguro HttpOnly ("accessToken") e retorna a sessão mínima cadastrada.
   */
  @PostMapping("/register")
  public ResponseEntity<AuthSessionDTO> register(@RequestBody @Valid RegisterDTO data){
    AuthResponseDTO response = authService.register(data);
    ResponseCookie cookie = buildAuthCookie(response.token(), Duration.ofMinutes(expirationMinutes));

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(response.session());
  }

  /**
   * Encerra a sessão do usuário no navegador limpando o cookie seguro HttpOnly.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    ResponseCookie cookie = buildAuthCookie("", Duration.ZERO);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .build();
  }

  /**
   * Rota privada para obter os dados cadastrais do usuário autenticado.
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(new UserResponseDTO(user));
  }

  /**
   * Rota privada para retificação de dados cadastrais (nome, sobrenome e e-mail) - LGPD Art. 18.
   */
  @PatchMapping("/me")
  public ResponseEntity<UserResponseDTO> updateProfile(
      @AuthenticationPrincipal User user,
      @RequestBody @Valid UpdateProfileRequestDTO dto) {
    return ResponseEntity.ok(authService.updateProfile(user, dto));
  }

  /**
   * Rota privada para troca voluntária de senha com validação prévia da senha atual.
   */
  @PatchMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal User user,
      @RequestBody @Valid ChangePasswordRequestDTO dto) {
    authService.changePassword(user, dto);
    return ResponseEntity.noContent().build();
  }

  /**
   * Rota pública para solicitação de recuperação de senha via e-mail.
   * Aplica proteção anti-enumeração LGPD.
   */
  @PostMapping("/forgot-password")
  public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO request) {
    passwordResetService.initiatePasswordReset(request);
    return ResponseEntity.ok(Map.of(
        "message", "Se o e-mail informado estiver cadastrado em nosso sistema, as instruções de redefinição foram enviadas."
    ));
  }

  /**
   * Rota pública para efetivação de nova senha através de token de uso único.
   */
  @PostMapping("/reset-password")
  public ResponseEntity<Map<String, String>> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO request) {
    passwordResetService.resetPassword(request);
    return ResponseEntity.ok(Map.of(
        "message", "Senha redefinida com sucesso. Você já pode fazer login com suas novas credenciais."
    ));
  }

  private ResponseCookie buildAuthCookie(String token, Duration maxAge) {
    return ResponseCookie.from("accessToken", token)
        .httpOnly(true)
        .secure(false) // Permite tráfego local/túneis; em produção com HTTPS o browser adiciona Secure
        .path("/")
        .maxAge(maxAge)
        .sameSite("Lax")
        .build();
  }
}
