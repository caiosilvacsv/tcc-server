package br.edu.ifnmg.pagtesouro.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA que representa um Usuário/Contribuinte do sistema de pagamentos.
 * <p>
 * **Conceito no TCC:**
 * Modela os cidadãos, alunos e servidores que acessam a plataforma para adquirir tíquetes de
 * alimentação ou quitar débitos, bem como os administradores do setor financeiro.
 * Implementa a interface {@link UserDetails} do Spring Security para viabilizar o controle de
 * sessões *stateless* baseadas em Tokens JWT.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Table(name = "users")
@Entity(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    /**
     * Identificador único do usuário gerado aleatoriamente no formato UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * E-mail institucional ou pessoal do usuário, utilizado para login no sistema.
     * Mapeado como único no banco de dados.
     */
    @Email
    @NotNull
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Nome completo do usuário.
     * <p>
     * **Importância:** Este campo é enviado no JSON de solicitação de pagamento ao PagTesouro,
     * constando fisicamente como o "Nome do Contribuinte" na Guia de Recolhimento da União (GRU).
     * </p>
     */
    @NotNull
    @Column(nullable = false)
    private String name;

    /**
     * Senha criptografada por meio do algoritmo BCrypt.
     */
    @NotNull
    @Column(nullable = false)
    private String password;

    /**
     * CPF do usuário (apenas dígitos).
     * <p>
     * **Importância:** O CPF é um campo obrigatório no envio dos dados para a API do PagTesouro,
     * constando fisicamente como "CPF/CNPJ do Contribuinte" na guia gerada (GRU).
     * </p>
     */
    @NotNull
    @Column(unique = true, nullable = false, length = 14)
    private String cpf;

    /**
     * Perfil de privilégios/acesso do usuário.
     * Salvo como texto no banco de dados PostgreSQL.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRoles role;

    /**
     * Controle de auditoria: data e hora exata em que o usuário foi cadastrado.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Controle de auditoria: data da última alteração de dados cadastrais.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Construtor de conveniência utilizado no fluxo de registro/cadastro de novos usuários.
     * Define o perfil padrão inicial do cadastrado como {@code USER}.
     *
     * @param email    E-mail de login
     * @param password Senha criptografada
     * @param name     Nome completo
     * @param cpf      CPF (apenas dígitos)
     */
    public User(String email, String password, String name, String cpf) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.cpf = cpf;
        this.role = UserRoles.USER;
    }

    /**
     * Retorna os perfis de acesso concedidos ao usuário.
     * Se for {@code ADMIN}, ganha privilégios de administrador e de usuário padrão.
     *
     * @return As autorizações concedidas (nunca nulo)
     */
    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role == UserRoles.ADMIN) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Retorna o identificador de login (e-mail) para autenticação do Spring Security.
     *
     * @return O e-mail do usuário
     */
    @Override
    public @NonNull String getUsername() {
        return email;
    }

    /**
     * Indica se a conta do usuário expirou. Uma conta expirada impede a autenticação.
     *
     * @return {@code true} se a conta do usuário for válida (não expirada),
     *         {@code false} caso contrário.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // Conta vitalícia
    }

    /**
     * Indica se o usuário está bloqueado ou desbloqueado. Um usuário bloqueado não consegue fazer login.
     *
     * @return {@code true} se o usuário não estiver bloqueado,
     *         {@code false} caso contrário.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true; // Conta desbloqueada
    }

    /**
     * Indica se as credenciais (senha) do usuário expiraram. Senhas expiradas impedem o login automático.
     *
     * @return {@code true} se a senha do usuário for válida (não expirada),
     *         {@code false} caso contrário.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Senha permanente
    }

    /**
     * Indica se o usuário está ativo/habilitado no sistema. Contas desabilitadas são rejeitadas no login.
     *
     * @return {@code true} se o usuário estiver habilitado,
     *         {@code false} caso contrário.
     */
    @Override
    public boolean isEnabled() {
        return true; // Usuário ativo
    }
}
