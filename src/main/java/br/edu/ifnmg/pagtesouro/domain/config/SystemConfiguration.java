package br.edu.ifnmg.pagtesouro.domain.config;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entidade JPA para armazenamento dinâmico de parâmetros operacionais de negócio da aplicação.
 *
 * @author Caio da Silva Viana
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_configuration")
@Entity(name = "system_configuration")
public class SystemConfiguration {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @NotNull
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public SystemConfiguration(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }
}
