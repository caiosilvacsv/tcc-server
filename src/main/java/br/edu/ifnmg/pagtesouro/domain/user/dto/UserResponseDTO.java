package br.edu.ifnmg.pagtesouro.domain.user.dto;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) para representação cadastral dos dados do usuário autenticado.
 * Utilizado no endpoint de consulta de perfil (/auth/me).
 *
 * @param id Identificador único do usuário (UUID).
 * @param name Primeiro nome do usuário.
 * @param lastName Sobrenome do usuário.
 * @param email Endereço de e-mail institucional ou pessoal.
 * @param cpf Cadastro de Pessoa Física (CPF) formatado/higienizado.
 * @param role Perfil de privilégios de acesso do usuário (USER ou ADMIN).
 *
 * @author Caio da Silva Viana
 */
public record UserResponseDTO(
    UUID id,
    String name,
    String lastName,
    String email,
    String cpf,
    UserRoles role
) {
  public UserResponseDTO(User user) {
    this(
        user.getId(),
        user.getName(),
        user.getLastName(),
        user.getEmail(),
        user.getCpf(),
        user.getRole()
    );
  }
}
