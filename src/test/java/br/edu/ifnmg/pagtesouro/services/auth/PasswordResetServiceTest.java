package br.edu.ifnmg.pagtesouro.services.auth;

import br.edu.ifnmg.pagtesouro.domain.auth.PasswordResetToken;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.ForgotPasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.ResetPasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.repository.PasswordResetTokenRepository;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import br.edu.ifnmg.pagtesouro.services.mail.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Recuperação de Senha (PasswordResetService)")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendResetUrl", "http://localhost:3000/reset-password");

        activeUser = new User(
                UUID.randomUUID(),
                "aluno@ifnmg.edu.br",
                "Caio",
                "Viana",
                "old_encoded_hash",
                "12345678901",
                UserRoles.USER,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Anti-enumeração: Deve retornar silenciosamente sem erros se e-mail não existir")
    void initiatePasswordReset_NonExistentEmailReturnsSilently() {
        when(userRepository.findOptionalByEmail("naoexiste@ifnmg.edu.br")).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                passwordResetService.initiatePasswordReset(new ForgotPasswordRequestDTO("naoexiste@ifnmg.edu.br"))
        );

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Anti-enumeração: Deve retornar silenciosamente se conta estiver desativada")
    void initiatePasswordReset_InactiveUserReturnsSilently() {
        activeUser.setActive(false);
        when(userRepository.findOptionalByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        assertDoesNotThrow(() ->
                passwordResetService.initiatePasswordReset(new ForgotPasswordRequestDTO(activeUser.getEmail()))
        );

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Deve gerar token temporário e disparar e-mail quando usuário for válido e ativo")
    void initiatePasswordReset_Success() {
        when(userRepository.findOptionalByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        passwordResetService.initiatePasswordReset(new ForgotPasswordRequestDTO(activeUser.getEmail()));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(1)).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken);
        assertEquals(activeUser, savedToken.getUser());
        assertFalse(savedToken.isUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));

        verify(emailService, times(1)).sendPasswordResetEmail(
                eq(activeUser.getEmail()),
                eq(activeUser.getName()),
                contains("token=" + savedToken.getToken())
        );
    }

    @Test
    @DisplayName("Deve redefinir a senha com sucesso, codificar hash e invalidar token")
    void resetPassword_Success() {
        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(tokenStr, activeUser, Instant.now().plus(Duration.ofMinutes(15)));

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenha@2026")).thenReturn("new_encoded_hash");

        passwordResetService.resetPassword(new ResetPasswordRequestDTO(tokenStr, "NovaSenha@2026"));

        assertEquals("new_encoded_hash", activeUser.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository, times(1)).save(activeUser);
        verify(tokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("Deve lançar exceção se o token de redefinição não for encontrado")
    void resetPassword_TokenNotFoundThrowsException() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                passwordResetService.resetPassword(new ResetPasswordRequestDTO("invalid-token", "NovaSenha@2026"))
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção se o token de redefinição já estiver expirado")
    void resetPassword_ExpiredTokenThrowsException() {
        String tokenStr = UUID.randomUUID().toString();
        // Token expirado há 5 minutos
        PasswordResetToken token = new PasswordResetToken(tokenStr, activeUser, Instant.now().minus(Duration.ofMinutes(5)));

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () ->
                passwordResetService.resetPassword(new ResetPasswordRequestDTO(tokenStr, "NovaSenha@2026"))
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção se o token já tiver sido utilizado (uso único)")
    void resetPassword_AlreadyUsedTokenThrowsException() {
        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken(tokenStr, activeUser, Instant.now().plus(Duration.ofMinutes(10)));
        token.setUsed(true);

        when(tokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () ->
                passwordResetService.resetPassword(new ResetPasswordRequestDTO(tokenStr, "NovaSenha@2026"))
        );

        verify(userRepository, never()).save(any());
    }
}
