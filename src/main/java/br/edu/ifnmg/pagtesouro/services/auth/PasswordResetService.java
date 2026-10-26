package br.edu.ifnmg.pagtesouro.services.auth;

import br.edu.ifnmg.pagtesouro.domain.auth.PasswordResetToken;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.ForgotPasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.auth.dto.ResetPasswordRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.repository.PasswordResetTokenRepository;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import br.edu.ifnmg.pagtesouro.services.config.SystemConfigurationService;
import br.edu.ifnmg.pagtesouro.services.mail.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pelas regras de negócio de recuperação de senha por e-mail com tokens de uso único.
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private SystemConfigurationService systemConfigurationService;

    @Value("${app.frontend.reset-password-url:http://localhost:3000/reset-password}")
    private String frontendResetUrl;

    @Autowired
    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) SystemConfigurationService systemConfigurationService) {
        this(userRepository, tokenRepository, emailService, passwordEncoder);
        this.systemConfigurationService = systemConfigurationService;
    }

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inicia o fluxo de redefinição de senha enviando um e-mail institucional com token temporário.
     */
    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequestDTO request) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        Optional<User> optionalUser = userRepository.findOptionalByEmail(email);

        if (optionalUser.isEmpty()) {
            log.info("Tentativa de recuperação de senha para e-mail não cadastrado: {}", email);
            return; // Retorno silencioso anti-enumeração
        }

        User user = optionalUser.get();
        if (!user.isEnabled()) {
            log.warn("Tentativa de recuperação de senha para conta desativada: {}", email);
            return;
        }

        int expMinutes = systemConfigurationService != null
            ? systemConfigurationService.getResetTokenExpirationMinutes()
            : 15;
        String baseResetUrl = systemConfigurationService != null
            ? systemConfigurationService.getFrontendResetPasswordUrl(frontendResetUrl)
            : frontendResetUrl;

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(expMinutes));

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
        tokenRepository.save(resetToken);

        String separator = baseResetUrl.contains("?") ? "&" : "?";
        String resetLink = baseResetUrl + separator + "token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);
    }

    /**
     * Efetiva a alteração da senha com o token seguro e o invalida após o uso.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token().trim())
            .orElseThrow(() -> new IllegalArgumentException("Token de redefinição de senha inválido ou expirado."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Este link de redefinição já foi utilizado ou expirou.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Senha redefinida com sucesso para o usuário ID: {}", user.getId());
    }
}
