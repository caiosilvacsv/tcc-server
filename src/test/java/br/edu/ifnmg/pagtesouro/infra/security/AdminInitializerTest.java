package br.edu.ifnmg.pagtesouro.infra.security;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes da Promoção Automática de Administrador no Startup (AdminInitializer)")
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(userRepository);
    }

    @Test
    @DisplayName("Deve ignorar inicialização se a propriedade de e-mail estiver vazia ou nula")
    void runWithEmptyEmail() throws Exception {
        ReflectionTestUtils.setField(adminInitializer, "initialAdminEmail", "");

        adminInitializer.run();

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Deve ignorar promoção se o e-mail estiver configurado mas o usuário não existir no banco")
    void runWithUserNotFound() throws Exception {
        String email = "admin@ifnmg.edu.br";
        ReflectionTestUtils.setField(adminInitializer, "initialAdminEmail", email);
        when(userRepository.findByEmail(email)).thenReturn(null);

        adminInitializer.run();

        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve promover o usuário a ADMIN com sucesso se ele for localizado como USER padrão")
    void runWithPromotionSuccess() throws Exception {
        String email = "caio@ifnmg.edu.br";
        ReflectionTestUtils.setField(adminInitializer, "initialAdminEmail", email);

        User user = new User(
                UUID.randomUUID(),
                email,
                "Caio Viana",
                "hash",
                "12345678901",
                UserRoles.USER,
                null,
                null
        );

        when(userRepository.findByEmail(email)).thenReturn(user);

        adminInitializer.run();

        assertEquals(UserRoles.ADMIN, user.getRole());
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Deve ignorar promoção se o usuário correspondente já possuir privilégios de ADMIN")
    void runWithUserAlreadyAdmin() throws Exception {
        String email = "admin_real@ifnmg.edu.br";
        ReflectionTestUtils.setField(adminInitializer, "initialAdminEmail", email);

        User user = new User(
                UUID.randomUUID(),
                email,
                "Caio Viana",
                "hash",
                "12345678901",
                UserRoles.ADMIN,
                null,
                null
        );

        when(userRepository.findByEmail(email)).thenReturn(user);

        adminInitializer.run();

        assertEquals(UserRoles.ADMIN, user.getRole());
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any());
    }
}
