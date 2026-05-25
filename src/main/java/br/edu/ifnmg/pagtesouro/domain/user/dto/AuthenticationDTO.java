package br.edu.ifnmg.pagtesouro.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO (Data Transfer Object) de Autenticação.
 * Encapsula e valida as credenciais de login recebidas na requisição HTTP {@code POST /auth/login}.
 *
 * @param email    O e-mail do usuário (deve ser válido e não pode ser em branco)
 * @param password A senha do usuário (não pode ser em branco)
 *
 * @author Caio da Silva Viana
 */
public record AuthenticationDTO (
    @Email
    @NotBlank(message = "E-mail é obrigatório")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    String password
){
}

