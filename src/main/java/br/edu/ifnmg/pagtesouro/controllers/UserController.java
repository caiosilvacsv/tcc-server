package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.common.PageResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UpdateRoleRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UpdateStatusRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.dto.UserResponseDTO;
import br.edu.ifnmg.pagtesouro.services.user.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para gestão e governança de usuários (exclusivo para Administradores).
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lista usuários cadastrados com suporte a busca textual por nome, CPF ou e-mail e paginação.
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> listUsers(
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC, size = 15) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(search, pageable));
    }

    /**
     * Atualiza o papel/perfil de privilégios de um usuário (USER ou ADMIN).
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> updateRole(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRoleRequestDTO request,
            @AuthenticationPrincipal User executingAdmin) {
        return ResponseEntity.ok(userService.updateRole(id, request.role(), executingAdmin));
    }

    /**
     * Ativa ou desativa uma conta de usuário.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateStatusRequestDTO request,
            @AuthenticationPrincipal User executingAdmin) {
        return ResponseEntity.ok(userService.updateStatus(id, request.active(), executingAdmin));
    }
}
