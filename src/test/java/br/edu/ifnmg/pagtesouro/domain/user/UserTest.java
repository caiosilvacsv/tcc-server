package br.edu.ifnmg.pagtesouro.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da Entidade de Usuário (User)")
class UserTest {

    @Test
    @DisplayName("Deve inicializar um novo Usuário padrão com perfil USER e ativo")
    void constructor_StandardUser() {
        User user = new User("aluno@ifnmg.edu.br", "cripto123", "Caio", "Viana", "11144477735");
        
        assertEquals("aluno@ifnmg.edu.br", user.getEmail());
        assertEquals("cripto123", user.getPassword());
        assertEquals("Caio", user.getName());
        assertEquals("Viana", user.getLastName());
        assertEquals("11144477735", user.getCpf());
        assertEquals(UserRoles.USER, user.getRole());
        assertTrue(user.isActive());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Deve mapear as autorizações corretas para o perfil USER")
    void getAuthorities_StandardUser() {
        User user = new User("aluno@ifnmg.edu.br", "cripto123", "Caio", "Viana", "11144477735");
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertFalse(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Deve mapear as autorizações completas para o perfil ADMIN")
    void getAuthorities_AdminUser() {
        User admin = new User(
            java.util.UUID.randomUUID(),
            "admin@ifnmg.edu.br",
            "Administrador",
            "Financeiro",
            "cripto123",
            "11144477735",
            UserRoles.ADMIN,
            java.time.Instant.now(),
            java.time.Instant.now()
        );
        
        Collection<? extends GrantedAuthority> authorities = admin.getAuthorities();
        
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Deve retornar true para todas as flags de status de conta ativa do Spring Security")
    void userDetailsFlags_WhenActive() {
        User user = new User("aluno@ifnmg.edu.br", "cripto123", "Caio", "Viana", "11144477735");
        
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Deve retornar false em isEnabled quando conta estiver inativa")
    void userDetailsFlags_WhenInactive() {
        User user = new User("inativo@ifnmg.edu.br", "cripto123", "Aluno", "Inativo", "11144477735");
        user.setActive(false);
        
        assertFalse(user.isActive());
        assertFalse(user.isEnabled());
    }
}
