package br.edu.ifnmg.pagtesouro.infra.security;

import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de Interceptação e Segurança Stateless do Spring Security.
 * Implementa a estratégia {@link OncePerRequestFilter} para garantir execução única por requisição HTTP.
 * <p>
 * Suporta autenticação híbrida: extração transparente a partir de Cookies seguros HttpOnly (mitigação de XSS)
 * com fallback para o cabeçalho {@code Authorization: Bearer <token>} (Swagger UI, integrações e testes).
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

  @Autowired
  TokenService tokenService;

  @Autowired
  UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    var token = this.recoverToken(request);
    if(token != null){
      var email = tokenService.validateToken(token);
      if (email != null && !email.isBlank()) {
        UserDetails user = userRepository.findByEmail(email);

        if (user != null && user.isEnabled()) {
          var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Método auxiliar responsável por extrair o token JWT da requisição.
   * Por motivos estritos de segurança contra XSS e exfiltração de credenciais,
   * a autenticação aceita EXCLUSIVAMENTE o Cookie seguro HttpOnly "accessToken".
   * Tokens via cabeçalho Authorization: Bearer não são aceitos.
   *
   * @param request A requisição contendo os cookies HTTP
   * @return O token JWT em String se presente no cookie HttpOnly; null caso contrário
   */
  private String recoverToken(HttpServletRequest request){
    if (request.getCookies() != null) {
      for (var cookie : request.getCookies()) {
        if ("accessToken".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }
}
