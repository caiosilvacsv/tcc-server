package br.edu.ifnmg.pagtesouro.infra.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

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

  @Value("${api.security.cors.allowed-origins:*}")
  private String allowedOrigins;

  /**
   * Define o filtro de segurança (Security Filter Chain), configurando quais rotas são públicas
   * e quais exigem perfis específicos de autorização.
   *
   * @param http Objeto HttpSecurity para configurar a segurança web
   * @return Cadeia de filtros configurada
   * @throws Exception Assegura que falhas no nível de filtro retornem respostas JSON padronizadas
   */
  @Bean
  public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
            .requestMatchers(
                "/swagger",
                "/swagger/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**"
            ).permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/payment/webhook").permitAll()
            .requestMatchers(HttpMethod.POST, "/payments/anonymous").permitAll()
            .requestMatchers(HttpMethod.GET, "/orders/guest").permitAll()
            .requestMatchers(HttpMethod.GET, "/product/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/product").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/product/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/product/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/orders/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex
            // Tratamento de exceção 403 (Usuário logado, mas sem permissão de ADMIN)
            .accessDeniedHandler((req, res, accessDeniedException) ->{
              res.setStatus(HttpServletResponse.SC_FORBIDDEN);
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter().write("""
                  {
                    "status": 403,
                    "error" : "Unauthorized",
                    "message": "Acesso negado: Você não possui permissão de Adminstrador para realizar a ação."
                  }
                  """);
            })
            //Tratamento de exceção 401 (Token ausente, inválido ou expirado)
            .authenticationEntryPoint((req, res, authException) ->{
              res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              res.setContentType("application/json;charset=UTF-8");
              res.getWriter().write("""
                  {
                    "status": 401,
                    "error" : "Unauthorized",
                    "message": "Não autenticado: Token ausente ou inválido."
                  }
              """);
            })
        )
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /**
   * Garante a interoperabilidade segura entre o backend SPRING e clientes webs's externos.
   * <p>
   *   Mecanismo que padroniza a política de mesma origem, assim declara explicitamente a autorização para consumir
   *   seus dados.
   * </p>
   *
   * @return Registrador de regras de CORS baseado em padrões de URL.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    if ("*".equals(allowedOrigins)) {
      configuration.setAllowedOrigins(List.of("*"));
      configuration.setAllowCredentials(false);
    } else {
      configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
      configuration.setAllowCredentials(true);
    }
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
    configuration.setExposedHeaders(List.of("Authorization"));
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * Interface central do Spring Security responsável por validar credenciais de usuário.
   * <p>
   *   Se as credenciais estiverem corretas, retorna um objeto Authentication preenchido
   *   com os dados do usuário (Principal) e suas permissões (GrantedAuthorities).
   *   Caso contrário, lança BadCredentialsException.
   * </p>
   *
   * @param authenticationConfiguration Objeto Authentication preenchido com os dados do usuário.
   * @return Authentication do SPRING SECURITY
   * @throws Exception Assegura que retorne um JSON padronizado.
   */
  @Bean
  public AuthenticationManager authenticationManager ( AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * Gera o hash criptográfico.
   *
   * @return Hash da senha
   */
  @Bean
  public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
  }
}
