package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.auth.dto.AuthResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.AuthSessionDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.domain.user.dto.AuthenticationDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.RegisterDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UserResponseDTO;
import br.edu.ifnmg.pagtesouro.services.authentication.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Controlador de Autenticação (AuthenticationController)")
class AuthenticationControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @Test
    @DisplayName("Deve efetuar login com sucesso, definir cookie HttpOnly e retornar 200 OK com a sessão mínima (sem CPF)")
    void loginSuccess() {
        AuthenticationDTO request = new AuthenticationDTO("aluno@ifnmg.edu.br", "123456");
        AuthSessionDTO sessionDto = new AuthSessionDTO("Aluno", "IFNMG", UserRoles.USER);
        AuthResponseDTO responseDto = new AuthResponseDTO("mocked-jwt-token", sessionDto);

        when(authService.login(request)).thenReturn(responseDto);

        ResponseEntity<AuthSessionDTO> response = authenticationController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Aluno", response.getBody().name());
        assertEquals("IFNMG", response.getBody().lastName());
        assertEquals(UserRoles.USER, response.getBody().role());
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("accessToken=mocked-jwt-token"));
        assertTrue(setCookie.contains("HttpOnly"));
        verify(authService, times(1)).login(request);
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso, definir cookie HttpOnly e retornar 200 OK com a sessão mínima (sem CPF)")
    void registerSuccess() {
        RegisterDTO request = new RegisterDTO(
            "aluno@ifnmg.edu.br",
            "123456",
            "Aluno",
            "IFNMG",
            "11144477735"
        );
        AuthSessionDTO sessionDto = new AuthSessionDTO("Aluno", "IFNMG", UserRoles.USER);
        AuthResponseDTO responseDto = new AuthResponseDTO("mocked-jwt-token-register", sessionDto);

        when(authService.register(request)).thenReturn(responseDto);

        ResponseEntity<AuthSessionDTO> response = authenticationController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Aluno", response.getBody().name());
        assertEquals("IFNMG", response.getBody().lastName());
        assertEquals(UserRoles.USER, response.getBody().role());
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("accessToken=mocked-jwt-token-register"));
        assertTrue(setCookie.contains("HttpOnly"));
        verify(authService, times(1)).register(request);
    }

    @Test
    @DisplayName("Deve retornar os dados cadastrais do perfil do usuário autenticado no endpoint /auth/me")
    void getMeSuccess() {
        UUID userId = UUID.randomUUID();
        User authenticatedUser = new User(
            userId,
            "servidor@ifnmg.edu.br",
            "Servidor",
            "Federal",
            "secret-hash",
            "12345678909",
            UserRoles.ADMIN,
            Instant.now(),
            Instant.now()
        );

        ResponseEntity<UserResponseDTO> response = authenticationController.getMe(authenticatedUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().id());
        assertEquals("Servidor", response.getBody().name());
        assertEquals("Federal", response.getBody().lastName());
        assertEquals("servidor@ifnmg.edu.br", response.getBody().email());
        assertEquals("12345678909", response.getBody().cpf());
        assertEquals(UserRoles.ADMIN, response.getBody().role());
    }
}
