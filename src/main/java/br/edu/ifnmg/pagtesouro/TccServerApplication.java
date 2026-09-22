package br.edu.ifnmg.pagtesouro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Classe principal de inicialização do servidor backend do TCC
 * <p>
 * **Conceito no TCC (Engenharia de Software e Robustez):**
 * Inicializa a aplicação Spring Boot habilitando três pilares essenciais de resiliência e concorrência:
 * <ul>
 *   <li>{@link EnableAsync}: Ativa o processamento reativo e assíncrono de webhooks.</li>
 *   <li>{@link EnableScheduling}: Habilita a execução automática do Daemon de contingência.</li>
 *   <li>{@link EnableRetry}: Ativa políticas de retentativas automáticas com backoff exponencial para chamadas governamentais instáveis.</li>
 * </ul>
 * </p>
 *
 * @author Caio da Silva Viana
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
public class TccServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TccServerApplication.class, args);
	}
}
