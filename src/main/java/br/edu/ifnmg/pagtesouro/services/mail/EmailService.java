package br.edu.ifnmg.pagtesouro.services.mail;

/**
 * Interface do serviço de disparo de e-mails transacionais do sistema.
 *
 * @author Caio da Silva Viana
 */
public interface EmailService {

    /**
     * Envia o e-mail transacional com o link para redefinição de senha do usuário.
     *
     * @param toEmail Endereço de e-mail do destinatário
     * @param userName Nome legível do usuário
     * @param resetLink URL completa de redefinição com token seguro
     */
    void sendPasswordResetEmail(String toEmail, String userName, String resetLink);
}
