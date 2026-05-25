package br.edu.ifnmg.pagtesouro.infra.security;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {
  private final JwtProperties jwtProperties;

  public TokenService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

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
      throw new RuntimeException("Error while generating token", e);
    }
  }

  public String validateToken( String token){
    try {
      Algorithm algorithm = Algorithm.HMAC256(jwtProperties.secret());
      return JWT.require(algorithm)
          .withIssuer(jwtProperties.issuer())
          .build()
          .verify(token)
          .getSubject();
    }catch(JWTVerificationException e){
      return "";
    }
  }

  private Instant genExpirationDate(){
    return Instant
        .now()
        .plus(jwtProperties.expirationTimeInMinutes(), ChronoUnit.MINUTES);
  }
}
