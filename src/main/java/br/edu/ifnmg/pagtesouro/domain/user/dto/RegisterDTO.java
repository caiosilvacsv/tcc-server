package br.edu.ifnmg.pagtesouro.domain.user.dto;

import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;

public record RegisterDTO(String login, String password, UserRoles role) {
}
