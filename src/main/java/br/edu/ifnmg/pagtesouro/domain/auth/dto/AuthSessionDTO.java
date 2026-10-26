package br.edu.ifnmg.pagtesouro.domain.auth.dto;

import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;

/**
 * DTO de sessão mínima retornado pelas rotas de autenticação (/auth/login e /auth/register).
 * Em estrita conformidade com o Princípio da Minimização da LGPD (Art. 6º, III),
 * omite dados sensíveis ou privados (como CPF e e-mail), trafegando apenas os metadados
 * essenciais para renderização da barra de navegação e controle de rotas no cliente.
 *
 * @param name Primeiro nome do usuário autenticado.
 * @param lastName Sobrenome do usuário autenticado.
 * @param role Perfil de autorização do usuário (USER ou ADMIN).
 *
 * @author Caio da Silva Viana
 */
public record AuthSessionDTO(
    String name,
    String lastName,
    UserRoles role
) {
    public AuthSessionDTO(User user) {
        this(user.getName(), user.getLastName(), user.getRole());
    }
}
