package br.edu.ifnmg.pagtesouro.infra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroProperties;

/**
 * Classe de configuração de segurança do Spring Security.
 * Configura as regras de autenticação baseadas em token stateless (JWT) e autorizações por perfil (Roles).
 *
 * @author Caio da Silva Viana
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, PagTesouroProperties.class})
public class SecurityConfiguration {

  @Autowired
  SecurityFilter securityFilter;

  /**
   * Define o filtro de segurança (Security Filter Chain), configurando quais rotas são públicas
   * e quais exigem perfis específicos de autorização.
   *
   * @param http Objeto HttpSecurity para configurar a segurança web
   * @return Cadeia de filtros configurada
   */
  @Bean
  public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/payment/webhook").permitAll()
            .requestMatchers(HttpMethod.POST, "/product").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/product/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/product/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/product/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public AuthenticationManager authenticationManager ( AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
  }
}
