package br.edu.ifnmg.pagtesouro.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes dos Perfis de Acesso (UserRoles)")
class UserRolesTest {

    @Test
    @DisplayName("Deve converter string correspondente para o Enum UserRoles de forma segura")
    void fromRole_Success() {
        assertEquals(UserRoles.ADMIN, UserRoles.fromRole("admin"));
        assertEquals(UserRoles.USER, UserRoles.fromRole("user"));
        
        // Com letras maiúsculas e espaços
        assertEquals(UserRoles.ADMIN, UserRoles.fromRole("  ADMIN  "));
        assertEquals(UserRoles.USER, UserRoles.fromRole("User"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valores nulos ou inválidos")
    void fromRole_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> UserRoles.fromRole(null));
        assertThrows(IllegalArgumentException.class, () -> UserRoles.fromRole("invalido"));
        assertThrows(IllegalArgumentException.class, () -> UserRoles.fromRole(""));
    }
}
