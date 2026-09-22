package br.edu.ifnmg.pagtesouro.domain.user;

import lombok.Getter;

/**
 * Enum que define os perfis de acesso e autorizações dos usuários no sistema.
 * <p>
 * **Conceito no TCC:**
 * Mapeia os níveis de segurança de rotas do Spring Security. Permite distinguir
 * entre usuários comuns que efetuam compras/pagamentos (alunos, servidores, cidadãos)
 * e administradores do setor financeiro (tesouraria) que gerenciam relatórios e produtos.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum UserRoles {

    /**
     * Perfil de Administrador: Acesso completo ao sistema, relatórios da tesouraria
     * e gerenciamento de produtos (tíquetes/multas).
     */
    ADMIN("admin"),

    /**
     * Perfil de Usuário Padrão: Alunos, servidores ou cidadãos com permissão apenas para
     * comprar tíquetes, consultar histórico pessoal e efetuar pagamentos.
     */
    USER("user");

    private final String role;

    UserRoles(String role) {
        this.role = role;
    }

    /**
     * Converte e valida uma string de perfil para o Enum correspondente de forma segura.
     *
     * @param role O nome do perfil (ex: "admin", "user")
     * @return O Enum {@link UserRoles} correspondente
     * @throws IllegalArgumentException se o perfil fornecido for inválido
     */
    public static UserRoles fromRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Perfil de acesso não pode ser nulo");
        }
        for (UserRoles userRole : UserRoles.values()) {
            if (userRole.getRole().equalsIgnoreCase(role.trim())) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("Perfil de acesso inválido: " + role);
    }
}
