package br.edu.ifnmg.pagtesouro.domain.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para ativação ou inativação de conta de usuário por um Administrador.
 *
 * @param active Novo status da conta (true = ativa, false = desativada)
 *
 * @author Caio da Silva Viana
 */
public record UpdateStatusRequestDTO(
    @NotNull(message = "O campo active é obrigatório.")
    Boolean active
) {}
