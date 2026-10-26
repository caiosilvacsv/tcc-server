package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.config.SystemConfiguration;
import br.edu.ifnmg.pagtesouro.services.config.SystemConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gerenciamento dos parâmetros dinâmicos de sistema.
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/config")
public class SystemConfigurationController {

    private final SystemConfigurationService configService;

    public SystemConfigurationController(SystemConfigurationService configService) {
        this.configService = configService;
    }

    /**
     * Retorna apenas os parâmetros públicos (não-sensíveis) de interface e instituição
     * para hidratação dinâmica do Front-end.
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> listPublicConfigs() {
        return ResponseEntity.ok(configService.getPublicConfigurations());
    }

    /**
     * Lista todos os parâmetros configuráveis do sistema (exclusivo para ADMIN).
     */
    @GetMapping
    public ResponseEntity<List<SystemConfiguration>> listAll() {
        return ResponseEntity.ok(configService.listAll());
    }

    /**
     * Atualiza ou cria o valor de um parâmetro dinâmico em tempo de execução (exclusivo para ADMIN).
     */
    @PutMapping("/{key}")
    public ResponseEntity<SystemConfiguration> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.getOrDefault("value", "");
        String description = body.get("description");
        return ResponseEntity.ok(configService.update(key, value, description));
    }
}
