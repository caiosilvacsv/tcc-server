package br.edu.ifnmg.pagtesouro.infra.security;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do Filtro de Segurança Estrito HttpOnly (SecurityFilter)")
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve autenticar com sucesso quando o cookie HttpOnly accessToken estiver presente e válido")
    void doFilterInternal_WhenValidAccessTokenCookiePresent_AuthenticatesUser() throws ServletException, IOException {
        String token = "valid-jwt-token";
        String email = "aluno@ifnmg.edu.br";

        Cookie authCookie = new Cookie("accessToken", token);
        when(request.getCookies()).thenReturn(new Cookie[]{authCookie});
        when(tokenService.validateToken(token)).thenReturn(email);

        User user = new User(
            UUID.randomUUID(),
            email,
            "Aluno",
            "Silva",
            "hash",
            "11144477735",
            UserRoles.USER,
            Instant.now(),
            Instant.now()
        );
        when(userRepository.findByEmail(email)).thenReturn(user);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve rejeitar/ignorar cabeçalho Authorization: Bearer legado quando não houver cookie HttpOnly")
    void doFilterInternal_WhenLegacyBearerHeaderPresentWithoutCookie_DoesNotAuthenticate() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenService, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
