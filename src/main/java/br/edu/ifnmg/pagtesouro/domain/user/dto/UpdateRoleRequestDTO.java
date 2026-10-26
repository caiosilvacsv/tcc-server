package br.edu.ifnmg.pagtesouro.domain.user.dto;

import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para atualização de papel/perfil de privilégios de usuário por um Administrador.
 *
 * @param role Novo perfil do usuário (USER ou ADMIN)
 *
 * @author Caio da Silva Viana
 */
public record UpdateRoleRequestDTO(
    @NotNull(message = "O perfil (role) é obrigatório.")
    UserRoles role
) {}
