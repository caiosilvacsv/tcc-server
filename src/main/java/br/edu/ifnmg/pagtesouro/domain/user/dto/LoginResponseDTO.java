package br.edu.ifnmg.pagtesouro.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO (Data Transfer Object) de Resposta de Login.
 * Encapsula a resposta retornada pela API após uma autenticação ou registro bem-sucedido.
 *
 * @param token O Token de acesso JWT (JSON Web Token) gerado, que deve ser armazenado
 *              pelo frontend e enviado nos cabeçalhos das requisições subsequentes para
 *              acesso aos endpoints protegidos.
 *
 * @author Caio da Silva Viana
 */
public record LoginResponseDTO(
    @NotBlank
    String token
) {
}
