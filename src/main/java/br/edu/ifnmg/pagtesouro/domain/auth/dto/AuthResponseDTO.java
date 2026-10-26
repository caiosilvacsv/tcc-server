package br.edu.ifnmg.pagtesouro.domain.auth.dto;

/**
 * DTO interno que encapsula o resultado de uma autenticação bem-sucedida,
 * contendo o token JWT gerado (destinado à emissão do Cookie HttpOnly)
 * e o DTO de sessão mínima (destinado ao corpo da resposta HTTP).
 *
 * @param token O token de acesso JWT a ser inserido no Cookie HttpOnly.
 * @param session Os metadados de sessão mínima do usuário.
 *
 * @author Caio da Silva Viana
 */
public record AuthResponseDTO(
    String token,
    AuthSessionDTO session
) {
}
