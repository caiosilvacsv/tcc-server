package br.edu.ifnmg.pagtesouro.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da Entidade de Usuário (User)")
class UserTest {

    @Test
    @DisplayName("Deve inicializar um novo Usuário padrão com perfil USER")
    void constructor_StandardUser() {
        User user = new User("aluno@ifnmg.edu.br", "cripto123", "Caio Viana", "11144477735");
        
        assertEquals("aluno@ifnmg.edu.br", user.getEmail());
        assertEquals("cripto123", user.getPassword());
        assertEquals("Caio Viana", user.getName());
        assertEquals("11144477735", user.getCpf());
        assertEquals(UserRoles.USER, user.getRole());
    }

    @Test
    @DisplayName("Deve mapear as autorizações corretas para o perfil USER")
    void getAuthorities_StandardUser() {
        User user = new User("aluno@ifnmg.edu.br", "cripto123", "Caio Viana", "11144477735");
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertFalse(authorities.stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Deve mapear as autorizações completas para o perfil ADMIN")
    void getAuthorities_AdminUser() {
        // Usando o construtor AllArgsConstructor do Lombok, pois a entidade User não possui setters
        User admin = new User(
            java.util.UUID.randomUUID(),
            "admin@ifnmg.edu.br",
            "Administrador Financeiro",
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
    @DisplayName("Deve retornar true para todas as flags de status de conta vitalícia do Spring Security")
    void userDetailsFlags() {
        User user = new User();
        
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }
}
