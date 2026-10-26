package br.edu.ifnmg.pagtesouro.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitação de redefinição de senha por e-mail.
 *
 * @param email O e-mail do usuário cadastrado
 *
 * @author Caio da Silva Viana
 */
public record ForgotPasswordRequestDTO(
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail informado é inválido.")
    String email
) {}
