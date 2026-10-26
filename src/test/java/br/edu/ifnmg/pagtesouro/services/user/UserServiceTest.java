package br.edu.ifnmg.pagtesouro.services.user;

import br.edu.ifnmg.pagtesouro.domain.common.PageResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UserResponseDTO;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Gerenciamento de Usuários (UserService)")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminUser = new User(
                UUID.randomUUID(),
                "admin@ifnmg.edu.br",
                "Admin",
                "Federal",
                "hash",
                "11122233344",
                UserRoles.ADMIN,
                Instant.now(),
                Instant.now()
        );

        normalUser = new User(
                UUID.randomUUID(),
                "aluno@ifnmg.edu.br",
                "Aluno",
                "IFNMG",
                "hash",
                "55566677788",
                UserRoles.USER,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Deve listar usuários paginados com sucesso")
    void listUsersSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(normalUser, adminUser));

        when(userRepository.searchUsers("aluno", pageable)).thenReturn(page);

        PageResponseDTO<UserResponseDTO> response = userService.listUsers("aluno", pageable);

        assertNotNull(response);
        assertEquals(2, response.totalElements());
        assertEquals("Aluno", response.content().get(0).name());
        verify(userRepository, times(1)).searchUsers("aluno", pageable);
    }

    @Test
    @DisplayName("Deve alterar papel de usuário para ADMIN com sucesso")
    void updateRoleToAdminSuccess() {
        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(normalUser)).thenReturn(normalUser);

        UserResponseDTO response = userService.updateRole(normalUser.getId(), UserRoles.ADMIN, adminUser);

        assertEquals(UserRoles.ADMIN, response.role());
        assertEquals(UserRoles.ADMIN, normalUser.getRole());
        verify(userRepository, times(1)).save(normalUser);
    }

    @Test
    @DisplayName("Deve impedir que o administrador rebaixe o próprio perfil (salvaguarda de auto-revogação)")
    void updateRoleSelfLockoutPrevented() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateRole(adminUser.getId(), UserRoles.USER, adminUser)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve desativar usuário com sucesso")
    void updateStatusDeactivateSuccess() {
        when(userRepository.findById(normalUser.getId())).thenReturn(Optional.of(normalUser));
        when(userRepository.save(normalUser)).thenReturn(normalUser);

        UserResponseDTO response = userService.updateStatus(normalUser.getId(), false, adminUser);

        assertFalse(response.active());
        assertFalse(normalUser.isActive());
        verify(userRepository, times(1)).save(normalUser);
    }

    @Test
    @DisplayName("Deve impedir auto-desativação do administrador logado")
    void updateStatusSelfDeactivationPrevented() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateStatus(adminUser.getId(), false, adminUser)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir desativação do último administrador ativo no sistema")
    void updateStatusPreventDeactivatingLastAdmin() {
        User otherAdmin = new User(
                UUID.randomUUID(),
                "other@ifnmg.edu.br",
                "Outro",
                "Admin",
                "hash",
                "99988877766",
                UserRoles.ADMIN,
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRoleAndActiveTrue(UserRoles.ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
                userService.updateStatus(otherAdmin.getId(), false, adminUser)
        );

        verify(userRepository, never()).save(otherAdmin);
    }
}
