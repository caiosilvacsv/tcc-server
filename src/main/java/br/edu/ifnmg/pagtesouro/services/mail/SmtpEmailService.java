package br.edu.ifnmg.pagtesouro.services.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Implementação SMTP do serviço de e-mail institucional.
 * Utiliza o {@link JavaMailSender} oficial do Spring Boot com suporte a Gmail Senha de App
 * ou qualquer servidor de correio corporativo via protocolo SMTP padrão.
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:nao-responda@ifnmg.edu.br}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        // Fallback de desenvolvimento: se o e-mail não estiver configurado no .env, não falha a aplicação
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("[EMAIL-DEV-MODE] Servidor SMTP não configurado no .env. Link de redefinição para '{}': {}", toEmail, resetLink);
            return;
        }

        try {
            String htmlTemplate = loadPasswordResetHtmlTemplate();
            String htmlContent = htmlTemplate
                .replace("{{userName}}", userName != null ? userName : "Usuário")
                .replace("{{resetLink}}", resetLink)
                .replace("{{expirationMinutes}}", "15");

            String plainText = "Olá, " + userName + "!\n\n"
                + "Recebemos uma solicitação para redefinir a sua senha no portal do IFNMG.\n"
                + "Para redefinir sua senha, acesse o link a seguir (válido por 15 minutos):\n"
                + resetLink + "\n\n"
                + "Se você não solicitou esta alteração, por favor desconsidere esta mensagem.\n\n"
                + "IFNMG - Campus Salinas";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, "IFNMG - PagTesouro");
            helper.setTo(toEmail);
            helper.setSubject("Redefinição de Senha - IFNMG Campus Salinas");
            helper.setText(plainText, htmlContent);

            mailSender.send(message);
            log.info("E-mail de recuperação de senha enviado com sucesso para: {}", toEmail);
        } catch (Exception ex) {
            log.error("Falha ao enviar e-mail de recuperação de senha para '{}': {}", toEmail, ex.getMessage(), ex);
            // Registra em log mas não quebra a requisição do usuário
        }
    }

    private String loadPasswordResetHtmlTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/password-reset.html");
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Template HTML de e-mail não localizado, utilizando layout de fallback: {}", e.getMessage());
            return "<html><body><h2>IFNMG Campus Salinas</h2><p>Olá, {{userName}}!</p><p><a href=\"{{resetLink}}\">Clique aqui para redefinir sua senha</a> (Válido por {{expirationMinutes}} minutos).</p></body></html>";
        }
    }
}
