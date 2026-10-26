package br.edu.ifnmg.pagtesouro.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização voluntária dos dados cadastrais do próprio usuário (LGPD Art. 18).
 *
 * @param name Nome do usuário
 * @param lastName Sobrenome do usuário
 * @param email Novo endereço de e-mail institucional ou pessoal
 *
 * @author Caio da Silva Viana
 */
public record UpdateProfileRequestDTO(
    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
    String name,

    @NotBlank(message = "O sobrenome é obrigatório.")
    @Size(min = 2, max = 100, message = "O sobrenome deve ter entre 2 e 100 caracteres.")
    String lastName,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail informado é inválido.")
    String email
) {}
