package br.edu.ifnmg.pagtesouro.services.config;

import br.edu.ifnmg.pagtesouro.domain.config.SystemConfiguration;
import br.edu.ifnmg.pagtesouro.repository.SystemConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Configurações Dinâmicas (SystemConfigurationService)")
class SystemConfigurationServiceTest {

    @Mock
    private SystemConfigurationRepository repository;

    @InjectMocks
    private SystemConfigurationService configService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(configService, "defaultNotificationUrl", "https://default.ifnmg.edu.br/webhook");
        ReflectionTestUtils.setField(configService, "defaultAbandonedDays", 10);
        ReflectionTestUtils.setField(configService, "defaultPagTesouroToken", "env-token-123");
    }

    @Test
    @DisplayName("Deve inicializar cache a partir do banco e recuperar valores com fallback")
    void initCacheAndGetWithFallback() {
        SystemConfiguration config1 = new SystemConfiguration("pagtesouro.url_notificacao", "https://pagamentos.ifnmg.edu.br/payments/webhook", "URL");
        SystemConfiguration config2 = new SystemConfiguration("order.abandoned_days", "15", "Dias");

        when(repository.findAll()).thenReturn(List.of(config1, config2));

        configService.refreshCache();

        assertEquals("https://pagamentos.ifnmg.edu.br/payments/webhook", configService.getNotificationUrl());
        assertEquals(15, configService.getAbandonedOrderExpirationDays());
        // Chave não presente no banco -> deve usar o fallback padrão do .env
        assertEquals("env-token-123", configService.getPagTesouroToken());
    }

    @Test
    @DisplayName("Deve atualizar configuração no banco e atualizar cache em memória atomicamente")
    void updateConfigurationSuccess() {
        SystemConfiguration existing = new SystemConfiguration("order.abandoned_days", "10", "Dias");
        when(repository.findById("order.abandoned_days")).thenReturn(Optional.of(existing));
        when(repository.save(any(SystemConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemConfiguration updated = configService.update("order.abandoned_days", "20");

        assertNotNull(updated);
        assertEquals("20", updated.getConfigValue());
        assertEquals(20, configService.getAbandonedOrderExpirationDays());
        verify(repository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Deve criar nova configuração se a chave ainda não existir no banco")
    void updateConfigurationCreateNew() {
        when(repository.findById("nova.chave")).thenReturn(Optional.empty());
        when(repository.save(any(SystemConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemConfiguration created = configService.update("nova.chave", "valor_custom");

        assertNotNull(created);
        assertEquals("valor_custom", created.getConfigValue());
        assertEquals("valor_custom", configService.get("nova.chave", "fallback"));
        verify(repository, times(1)).save(any(SystemConfiguration.class));
    }

    @Test
    @DisplayName("Deve retornar lista de domínios permitidos para notificação da STN")
    void getAllowedNotificationDomains() {
        SystemConfiguration config = new SystemConfiguration("pagtesouro.notification_domains", ".gov.br, .edu.br, .ifnmg.edu.br", "Domínios");
        when(repository.findAll()).thenReturn(List.of(config));

        configService.refreshCache();

        List<String> domains = configService.getAllowedNotificationDomains();
        assertEquals(3, domains.size());
        assertTrue(domains.contains(".gov.br"));
        assertTrue(domains.contains(".edu.br"));
        assertTrue(domains.contains(".ifnmg.edu.br"));
    }
}
