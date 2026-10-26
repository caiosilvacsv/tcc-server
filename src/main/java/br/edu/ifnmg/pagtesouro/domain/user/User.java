package br.edu.ifnmg.pagtesouro.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NonNull;
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
@Setter
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
     * Nome do usuário.
     * <p>
     * **Importância:** Este campo é enviado no JSON de solicitação de pagamento ao PagTesouro,
     * constando fisicamente como o "Nome do Contribuinte" na Guia de Recolhimento da União (GRU).
     * </p>
     */
    @NotNull
    @Column(nullable = false)
    private String name;

    /**
     * Sobrenome do usuário.
     * <p>
     * **Importância:** Este campo é enviado no JSON de solicitação de pagamento ao PagTesouro,
     * constando fisicamente como o "Nome do Contribuinte" na Guia de Recolhimento da União (GRU).
     * </p>
     */
    @NotNull
    @Column(nullable = false)
    private String lastName;

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
     * Status de ativação da conta.
     * Se falso, o usuário fica impedido de realizar login na aplicação.
     */
    @NotNull
    @Column(nullable = false)
    private boolean active = true;

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
     * Define o perfil padrão inicial do cadastrado como {@code USER} e o status como ativo.
     *
     * @param email    E-mail de login
     * @param password Senha criptografada
     * @param name     Nome
     * @param lastName Sobrenome
     * @param cpf      CPF (apenas dígitos)
     */
    public User(String email, String password, String name, String lastName, String cpf) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.lastName = lastName;
        this.cpf = cpf;
        this.role = UserRoles.USER;
        this.active = true;
    }

    public User(UUID id, String email, String name, String lastName, String password, String cpf, UserRoles role, Instant createdAt, Instant updatedAt) {
        this(id, email, name, lastName, password, cpf, role, true, createdAt, updatedAt);
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

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Retorna se a conta está ativa e habilitada para autenticação no Spring Security.
     */
    @Override
    public boolean isEnabled() {
        return this.active;
    }
}
