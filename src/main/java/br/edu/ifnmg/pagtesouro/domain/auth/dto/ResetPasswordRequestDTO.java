package br.edu.ifnmg.pagtesouro.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para efetivação da redefinição de senha através de token seguro.
 *
 * @param token O token de uso único recebido por e-mail
 * @param newPassword A nova senha desejada pelo usuário
 *
 * @author Caio da Silva Viana
 */
public record ResetPasswordRequestDTO(
    @NotBlank(message = "O token de redefinição é obrigatório.")
    String token,

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 6, max = 100, message = "A nova senha deve ter no mínimo 6 caracteres.")
    String newPassword
) {}
