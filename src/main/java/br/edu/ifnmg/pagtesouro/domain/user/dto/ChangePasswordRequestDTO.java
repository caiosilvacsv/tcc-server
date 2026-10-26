package br.edu.ifnmg.pagtesouro.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para alteração de senha do usuário autenticado com confirmação da senha anterior.
 *
 * @param currentPassword Senha atual do usuário
 * @param newPassword Nova senha desejada
 *
 * @author Caio da Silva Viana
 */
public record ChangePasswordRequestDTO(
    @NotBlank(message = "A senha atual é obrigatória.")
    String currentPassword,

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 6, max = 100, message = "A nova senha deve ter no mínimo 6 caracteres.")
    String newPassword
) {}
