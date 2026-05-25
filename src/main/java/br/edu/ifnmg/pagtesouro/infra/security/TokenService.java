package br.edu.ifnmg.pagtesouro.infra.security;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Serviço responsável pela geração, assinatura e validação matemática de Tokens JWT (JSON Web Tokens).
 * <p>
 * **Conceito no TCC (Segurança e HMAC256):**
 * Utiliza o algoritmo simétrico **HMAC256** com base em uma chave secreta dinâmica e robusta para
 * assinar digitalmente cada token emitido. Garante o princípio de não-repúdio e integridade, impedindo
 * que o token seja alterado no cliente. Permite o controle de sessões stateless com tempo de expiração
 * controlado via propriedades da aplicação.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Service
public class TokenService {
  
  private final JwtProperties jwtProperties;

  public TokenService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Gera e assina digitalmente um token JWT para um usuário autenticado com sucesso.
   *
   * @param user O usuário autenticado proprietário do token
   * @return String contendo o Token JWT assinado
   * @throws RuntimeException se ocorrer um erro interno na geração da assinatura
   */
  public String generateToken(User user) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtProperties.secret());
      return JWT.create()
          .withIssuer(jwtProperties.issuer())
          .withSubject(user.getEmail())
          .withExpiresAt(genExpirationDate())
          .withAudience(jwtProperties.audience())
          .sign(algorithm);
    } catch (JWTCreationException e) {
      throw new RuntimeException("Erro ao gerar token de autenticação JWT!", e);
    }
  }

  /**
   * Decodifica, verifica as assinaturas matemáticas e valida a expiração de um token JWT.
   *
   * @param token O token recebido no cabeçalho HTTP
   * @return O e-mail (subject) do usuário proprietário se o token for válido; String vazia caso contrário
   */
  public String validateToken(String token){
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtProperties.secret());
      return JWT.require(algorithm)
          .withIssuer(jwtProperties.issuer())
          .build()
          .verify(token)
          .getSubject();
    } catch(JWTVerificationException e){
      return "";
    }
  }

  /**
   * Gera a data e hora exatas de expiração para o token JWT com base na propriedade de tempo em minutos.
   */
  private Instant genExpirationDate(){
    return Instant
        .now()
        .plus(jwtProperties.expirationTimeInMinutes(), ChronoUnit.MINUTES);
  }
}
