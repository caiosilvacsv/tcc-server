package br.edu.ifnmg.pagtesouro.services.user;

import br.edu.ifnmg.pagtesouro.domain.common.PageResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UserResponseDTO;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Serviço responsável pela administração e governança de contas de usuários (exclusivo para ADMIN).
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lista usuários cadastrados com paginação e suporte a busca textual por nome, CPF ou e-mail.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> listUsers(String search, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(search, pageable);
        return PageResponseDTO.from(page.map(UserResponseDTO::new));
    }

    /**
     * Altera o papel de privilégios de um usuário (USER ou ADMIN) com salvaguarda contra auto-bloqueio.
     */
    @Transactional
    public UserResponseDTO updateRole(UUID userId, UserRoles newRole, User executingAdmin) {
        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new FindException("Usuário não encontrado com ID: " + userId));

        // Impede que o próprio administrador rebaixe o seu perfil para USER
        if (targetUser.getId().equals(executingAdmin.getId()) && newRole == UserRoles.USER) {
            throw new IllegalArgumentException("Ação não permitida: um administrador não pode revogar os seus próprios privilégios.");
        }

        // Salvaguarda: Impede rebaixar o único administrador ativo da instituição
        if (targetUser.getRole() == UserRoles.ADMIN && newRole == UserRoles.USER) {
            long activeAdmins = userRepository.countByRoleAndActiveTrue(UserRoles.ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalStateException("Operação bloqueada: não é permitido rebaixar o único administrador ativo do sistema.");
            }
        }

        log.info("[ADMIN AUDIT] Administrador '{}' alterou a role do usuário '{}' ({}) de {} para {}",
            executingAdmin.getEmail(), targetUser.getEmail(), targetUser.getId(), targetUser.getRole(), newRole);

        targetUser.setRole(newRole);
        User saved = userRepository.save(targetUser);
        return new UserResponseDTO(saved);
    }

    /**
     * Ativa ou desativa uma conta de usuário com proteções de segurança.
     */
    @Transactional
    public UserResponseDTO updateStatus(UUID userId, boolean active, User executingAdmin) {
        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new FindException("Usuário não encontrado com ID: " + userId));

        // Impede que um administrador desative a própria conta
        if (!active && targetUser.getId().equals(executingAdmin.getId())) {
            throw new IllegalArgumentException("Ação não permitida: um administrador não pode desativar a sua própria conta.");
        }

        // Impede desativar o único administrador ativo do sistema
        if (!active && targetUser.getRole() == UserRoles.ADMIN) {
            long activeAdmins = userRepository.countByRoleAndActiveTrue(UserRoles.ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalStateException("Operação bloqueada: não é permitido desativar o único administrador ativo do sistema.");
            }
        }

        log.info("[ADMIN AUDIT] Administrador '{}' alterou o status da conta '{}' ({}) para active={}",
            executingAdmin.getEmail(), targetUser.getEmail(), targetUser.getId(), active);

        targetUser.setActive(active);
        User saved = userRepository.save(targetUser);
        return new UserResponseDTO(saved);
    }
}
