package br.edu.ifnmg.pagtesouro.infra.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração OpenAPI 3 / Swagger da API RESTful do IFNMG - Portal de Débitos e Pagamentos.
 * <p>
 * Centraliza os metadados acadêmicos da documentação e habilita o esquema de segurança global
 * via cabeçalho HTTP Bearer (JWT) no Swagger UI.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "IFNMG - Portal de Débitos e Pagamentos",
        version = "1.0",
        description = "Documentação oficial e interativa da API RESTful desenvolvida no Trabalho de Conclusão de Curso " +
            "(TCC) para integração com a plataforma PagTesouro da Secretaria do Tesouro Nacional (STN).",
        contact = @Contact(
            name = "Caio da Silva Viana",
            email = "cdsv1@aluno.ifnmg.edu.br"
        )
    ),
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    description = "Autenticação via token JWT Bearer. Insira o token gerado no endpoint /auth/login para liberar as operações protegidas.",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}
