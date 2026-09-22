package br.edu.ifnmg.pagtesouro.domain.user.dto;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do DTO de Perfil de Usuário (UserResponseDTO)")
class UserResponseDTOTest {

    @Test
    @DisplayName("Deve mapear corretamente todos os campos a partir da entidade User")
    void shouldMapFromUserEntity() {
        UUID id = UUID.randomUUID();
        User user = new User(
            id,
            "carlos@ifnmg.edu.br",
            "Carlos",
            "Silva",
            "hash123",
            "12345678909",
            UserRoles.ADMIN,
            Instant.now(),
            Instant.now()
        );

        UserResponseDTO dto = new UserResponseDTO(user);

        assertEquals(id, dto.id());
        assertEquals("Carlos", dto.name());
        assertEquals("Silva", dto.lastName());
        assertEquals("carlos@ifnmg.edu.br", dto.email());
        assertEquals("12345678909", dto.cpf());
        assertEquals(UserRoles.ADMIN, dto.role());
    }

    @Test
    @DisplayName("Deve manter os dados consistentes ao instanciar diretamente os atributos")
    void shouldInstantiateDirectly() {
        UUID id = UUID.randomUUID();
        UserResponseDTO dto = new UserResponseDTO(
            id,
            "Maria",
            "Souza",
            "maria@ifnmg.edu.br",
            "98765432100",
            UserRoles.USER
        );

        assertEquals(id, dto.id());
        assertEquals("Maria", dto.name());
        assertEquals("Souza", dto.lastName());
        assertEquals("maria@ifnmg.edu.br", dto.email());
        assertEquals("98765432100", dto.cpf());
        assertEquals(UserRoles.USER, dto.role());
    }
}
