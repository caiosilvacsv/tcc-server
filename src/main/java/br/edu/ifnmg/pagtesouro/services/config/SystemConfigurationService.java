package br.edu.ifnmg.pagtesouro.services.config;

import br.edu.ifnmg.pagtesouro.domain.config.SystemConfiguration;
import br.edu.ifnmg.pagtesouro.repository.SystemConfigurationRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de gerenciamento dinâmico de parâmetros operacionais do sistema com cache em memória atômico.
 *
 * @author Caio da Silva Viana
 */
@Slf4j
@Service
public class SystemConfigurationService {

    private static final Set<String> SENSITIVE_KEYS = Set.of("pagtesouro.token");

    /**
     * Parâmetros básicos iniciais do sistema com valores pré-configurados para que a aplicação
     * já inicie totalmente parametrizada mesmo em bancos já migrados anteriormente.
     */
    private static final List<SystemConfiguration> DEFAULT_CONFIGURATIONS = List.of(
        new SystemConfiguration(
            "pagtesouro.notification_domains",
            ".gov.br,.edu.br",
            "Domínios aceitos para urlNotificacao da STN"
        ),
        new SystemConfiguration(
            "pagtesouro.url_notificacao",
            "https://pagamentos.ifnmg.edu.br/payments/webhook",
            "URL de callback de notificação oficial do IFNMG"
        ),
        new SystemConfiguration(
            "order.abandoned_days",
            "10",
            "Dias para cancelamento automático de pedidos abandonados"
        ),
        new SystemConfiguration(
            "payment.expiration_days",
            "2",
            "Dias de validade para vencimento das guias geradas no PagTesouro"
        ),
        new SystemConfiguration(
            "pagtesouro.token",
            "",
            "Token de autenticação Bearer emitido pela STN (usa o token do ambiente se vazio)"
        ),
        new SystemConfiguration(
            "auth.reset_token_expiration_minutes",
            "15",
            "Tempo de validade em minutos do token de recuperação de senha"
        ),
        new SystemConfiguration(
            "app.frontend.reset_password_url",
            "http://localhost:3000/reset-password",
            "URL base do front-end para o link de redefinição de senha"
        ),
        new SystemConfiguration(
            "institution.name",
            "IFNMG • Campus Salinas",
            "Nome oficial da instituição e campus exibido no portal"
        ),
        new SystemConfiguration(
            "institution.portal_name",
            "IFNMG.EXPRESS",
            "Sigla de identificação do portal na barra de navegação"
        ),
        new SystemConfiguration(
            "institution.portal_subtitle",
            "Portal Institucional de Pagamentos & Tíquetes",
            "Subtítulo institucional exibido no cabeçalho"
        ),
        new SystemConfiguration(
            "institution.hero_title",
            "Arrecadação Pública Digital & Tíquetes Acadêmicos",
            "Título principal do banner da vitrine pública"
        ),
        new SystemConfiguration(
            "institution.hero_description",
            "Adquira refeições para o refeitório institucional ou efetue a quitação de taxas com liquidação em tempo real via PIX, Cartão ou GRU diretamente pelo PagTesouro.",
            "Texto descritivo do banner da vitrine pública"
        ),
        new SystemConfiguration(
            "institution.guiche_subtitle",
            "Campus Salinas • Refeitório Universitário & Biblioteca Central",
            "Subtítulo operacional exibido no Terminal de Guichê POS"
        ),
        new SystemConfiguration(
            "institution.address",
            "Fazenda Desterro, Rodovia Salinas / Taiobeiras, Km 02 • CEP: 39560-000 • Salinas - MG",
            "Endereço físico institucional exibido no rodapé"
        ),
        new SystemConfiguration(
            "system.version",
            "v3.0",
            "Versão atual do sistema exibida no cabeçalho"
        ),
        new SystemConfiguration(
            "ui.product_preset_colors",
            "#10B981,#F59E0B,#3B82F6,#EF4444,#8B5CF6,#EC4899",
            "Paleta de cores hexadecimais sugeridas para os cards de produtos"
        )
    );

    private final SystemConfigurationRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Value("${api.pagtesouro.url_notificacao:}")
    private String defaultNotificationUrl;

    @Value("${api.pagtesouro.notification-domains:.gov.br,.edu.br}")
    private String defaultNotificationDomains;

    @Value("${api.order.abandoned-expiration-days:10}")
    private int defaultAbandonedDays;

    @Value("${api.pagtesouro.token:}")
    private String defaultPagTesouroToken;

    public SystemConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void initCache() {
        seedMissingDefaultConfigurations();
        refreshCache();
    }

    /**
     * Garante que todas as chaves básicas existam no banco de dados sem sobrescrever
     * valores já personalizados pelo Administrador.
     */
    public synchronized void seedMissingDefaultConfigurations() {
        try {
            List<SystemConfiguration> existing = repository.findAll();
            Set<String> existingKeys = new HashSet<>();
            for (SystemConfiguration cfg : existing) {
                existingKeys.add(cfg.getConfigKey());
            }

            List<SystemConfiguration> missing = new ArrayList<>();
            for (SystemConfiguration def : DEFAULT_CONFIGURATIONS) {
                if (!existingKeys.contains(def.getConfigKey())) {
                    missing.add(new SystemConfiguration(def.getConfigKey(), def.getConfigValue(), def.getDescription()));
                }
            }

            if (!missing.isEmpty()) {
                repository.saveAll(missing);
                log.info("Semeados {} novos parâmetros de configuração padrão no banco de dados.", missing.size());
            }
        } catch (Exception e) {
            log.warn("Aviso ao verificar configurações iniciais padrão: {}", e.getMessage());
        }
    }

    public synchronized void refreshCache() {
        try {
            List<SystemConfiguration> all = repository.findAll();
            cache.clear();
            for (SystemConfiguration config : all) {
                if (config.getConfigValue() != null) {
                    cache.put(config.getConfigKey(), config.getConfigValue());
                }
            }
            log.info("Cache de configurações do sistema carregado com {} parâmetros.", cache.size());
        } catch (Exception e) {
            log.warn("Não foi possível carregar o cache de configurações na inicialização: {}", e.getMessage());
        }
    }

    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    @Transactional(readOnly = true)
    public List<SystemConfiguration> listAll() {
        List<SystemConfiguration> list = new ArrayList<>(repository.findAll());
        list.sort(Comparator.comparing(SystemConfiguration::getConfigKey));
        return list;
    }

    /**
     * Retorna apenas os parâmetros públicos (não-sensíveis) para consumo do Front-end (Server-Driven UI),
     * mesclando os padrões iniciais com os valores atuais em cache.
     */
    public Map<String, String> getPublicConfigurations() {
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemConfiguration def : DEFAULT_CONFIGURATIONS) {
            if (!SENSITIVE_KEYS.contains(def.getConfigKey())) {
                result.put(def.getConfigKey(), def.getConfigValue());
            }
        }
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if (!SENSITIVE_KEYS.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @Transactional
    public SystemConfiguration update(String key, String value) {
        return update(key, value, null);
    }

    @Transactional
    public SystemConfiguration update(String key, String value, String description) {
        SystemConfiguration config = repository.findById(key)
            .orElseGet(() -> new SystemConfiguration(
                key,
                value,
                description != null && !description.isBlank() ? description.trim() : "Configuração dinâmica"
            ));

        config.setConfigValue(value != null ? value.trim() : "");
        if (description != null && !description.isBlank()) {
            config.setDescription(description.trim());
        }
        SystemConfiguration saved = repository.save(config);

        cache.put(key, saved.getConfigValue());
        log.info("[CONFIG AUDIT] Parâmetro '{}' atualizado para '{}'", key, SENSITIVE_KEYS.contains(key) ? "***" : saved.getConfigValue());
        return saved;
    }

    public List<String> getAllowedNotificationDomains() {
        String fallback = (defaultNotificationDomains != null && !defaultNotificationDomains.isBlank())
            ? defaultNotificationDomains
            : ".gov.br,.edu.br";
        String raw = get("pagtesouro.notification_domains", fallback);
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    public String getNotificationUrl() {
        String fromDb = get("pagtesouro.url_notificacao", "");
        if (!fromDb.isBlank()) {
            return fromDb;
        }
        return defaultNotificationUrl;
    }

    public int getAbandonedOrderExpirationDays() {
        String raw = get("order.abandoned_days", String.valueOf(defaultAbandonedDays));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultAbandonedDays;
        }
    }

    public int getPaymentExpirationDays() {
        String raw = get("payment.expiration_days", "2");
        try {
            int days = Integer.parseInt(raw.trim());
            return days > 0 ? days : 2;
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public int getResetTokenExpirationMinutes() {
        String raw = get("auth.reset_token_expiration_minutes", "15");
        try {
            int mins = Integer.parseInt(raw.trim());
            return mins > 0 ? mins : 15;
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    public String getFrontendResetPasswordUrl(String fallbackUrl) {
        String fromDb = get("app.frontend.reset_password_url", "");
        if (!fromDb.isBlank()) {
            return fromDb;
        }
        return fallbackUrl;
    }

    public String getPagTesouroToken() {
        String fromDb = get("pagtesouro.token", "");
        if (!fromDb.isBlank()) {
            return fromDb;
        }
        return defaultPagTesouroToken;
    }
}
