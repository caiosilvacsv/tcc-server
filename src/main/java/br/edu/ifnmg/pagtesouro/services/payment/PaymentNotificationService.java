package br.edu.ifnmg.pagtesouro.services.payment;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serviço responsável por gerenciar o ciclo de vida das conexões ativas de Server-Sent Events (SSE)
 * no portal do IFNMG.
 * <p>
 * **Conceito no TCC (Comunicação Assíncrona e Tempo Real):**
 * Permite que navegadores de alunos ou cidadãos externos assinem um canal de notificação push
 * para um pagamento específico. O servidor envia um evento reativo no momento em que a fatura
 * é compensada no PagTesouro, evitando requisições repetitivas de polling.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Service
public class PaymentNotificationService {

    private static final Logger LOGGER = Logger.getLogger(PaymentNotificationService.class.getName());

    /**
     * Timeout padrão da conexão SSE em milissegundos (10 minutos).
     * Tempo altamente seguro e suficiente para a finalização física de pagamentos por Pix ou Cartão.
     */
    private static final Long DEFAULT_TIMEOUT = 600_000L;

    /**
     * Mapa concorrente thread-safe para rastrear emissores SSE associados a pagamentos pendentes.
     */
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registra um novo canal de Server-Sent Events (SSE) para monitoramento ativo do pagamento.
     * Implementa listeners rígidos de limpeza para prevenir vazamento de memória (Memory Leaks).
     *
     * @param paymentId O ID único do pagamento registrado no sistema.
     * @return O {@link SseEmitter} criado e enfileirado para envio contínuo.
     */
    public SseEmitter registerEmitter(UUID paymentId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // Registro de Callbacks de Limpeza e Segurança
        emitter.onCompletion(() -> {
            LOGGER.info("Conexão SSE completada para o pagamento: " + paymentId);
            emitters.remove(paymentId);
        });

        emitter.onTimeout(() -> {
            LOGGER.warning("Conexão SSE expirou (Timeout) para o pagamento: " + paymentId);
            emitters.remove(paymentId);
        });

        emitter.onError((ex) -> {
            LOGGER.log(Level.WARNING, "Erro na conexão SSE do pagamento: " + paymentId, ex);
            emitters.remove(paymentId);
        });

        // Adiciona ao mapa de concorrência ativa
        emitters.put(paymentId, emitter);

        // Envia um evento inicial de conexão estabelecida ("CONNECT")
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("Conexão SSE estabelecida com sucesso para monitoramento do pagamento " + paymentId));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Falha ao enviar evento inicial CONNECT para " + paymentId, e);
            emitter.completeWithError(e);
            emitters.remove(paymentId);
        }

        return emitter;
    }

    /**
     * Notifica o canal SSE ativo do cliente sobre a compensação e quitação de sua fatura.
     * Envia o sinalizador de conclusão e encerra a conexão de forma graciosa.
     *
     * @param paymentId O ID único do pagamento quitado.
     * @param status O novo status transacional (ex: "COMPLETED").
     */
    public void notifyPaymentPaid(UUID paymentId, String status) {
        SseEmitter emitter = emitters.get(paymentId);
        if (emitter != null) {
            try {
                LOGGER.info("Enviando evento SSE de pagamento quitado para: " + paymentId);
                emitter.send(SseEmitter.event()
                        .name("PAYMENT_PAID")
                        .data(status));
                
                // Finaliza graciosamente a conexão visto que a fatura já atingiu seu estado terminal (quitação)
                emitter.complete();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erro de I/O ao notificar SSE de quitação do pagamento: " + paymentId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(paymentId);
            }
        }
    }

    /**
     * Retorna a quantidade de conexões SSE ativas no momento (útil para telemetria/auditoria).
     *
     * @return O número de emitters atualmente enfileirados em memória.
     */
    public int getActiveConnectionsCount() {
        return emitters.size();
    }
}
