package br.edu.ifnmg.pagtesouro.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO (Data Transfer Object) de Registro.
 * Encapsula e valida os dados de cadastro de um novo usuário recebidos na requisição HTTP {@code POST /auth/register}.
 *
 * @param email     E-mail de cadastro (validado pelo Spring para garantir formato de e-mail correto)
 * @param password  Senha de acesso do usuário (criptografada no backend antes de persistir no banco)
 * @param name      Nome do usuário (obrigatório, não pode ser vazio)
 * @param last_name Sobrenome do usuário (obrigatório, não pode ser vazio)
 * @param cpf       CPF do contribuinte (validado via Regex para conter exatamente 11 dígitos numéricos limpos)
 *
 * @author Caio da Silva Viana
 */
public record RegisterDTO(
    @Email
    @NotBlank(message = "E-mail é obrigatório")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    String password,

    @NotBlank(message = "Nome é obrigatório")
    String name,

    @NotBlank(message = "Sobrenome é obrigatório")
    String last_name,

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter exatamente 11 dígitos numéricos")
    String cpf
) {
}
