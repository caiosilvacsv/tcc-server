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
 * **Conceito no TCC (Segurança da Informação e Autenticação Stateless):**
 * Atua interceptando todas as chamadas HTTP destinadas a rotas privadas. Extrai o cabeçalho
 * `Authorization`, decodifica o token JWT bearer e, caso seja matematicamente válido e não expirado,
 * injeta os detalhes do usuário logado diretamente no `SecurityContextHolder` do Spring, autorizando
 * a transação e seus privilégios de acesso (Roles).
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

  /**
   * Intercepta a requisição, extrai o token JWT, valida suas assinaturas HMAC256
   * e configura a autenticação no contexto de segurança do Spring Boot.
   *
   * @param request A requisição Servlet recebida
   * @param response A resposta Servlet a ser enviada
   * @param filterChain A cadeia de filtros de segurança subsequente
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    var token = this.recoverToken(request);
    if(token != null){
      var email = tokenService.validateToken(token);
      UserDetails user = userRepository.findByEmail(email);

      if (user != null) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Método auxiliar responsável por extrair o token Bearer do cabeçalho HTTP Authorization.
   *
   * @param request A requisição contendo os cabeçalhos HTTP
   * @return O token JWT em String se presente; null caso contrário
   */
  private String recoverToken(HttpServletRequest request){
    var authHeader = request.getHeader("Authorization");
    if(authHeader == null) return null;
    return authHeader.replace("Bearer ", "");
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }
}
