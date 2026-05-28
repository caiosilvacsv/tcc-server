package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.PaymentHistoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroRequestDTO;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável por orquestrar a lógica de checkout e envio de transações para o PagTesouro.
 * Suporta checkout direto (simplificado/anônimo) e checkout a partir de um Pedido (carrinho de compras).
 *
 * @author Caio da Silva Viana
 */
@Service
public class PaymentService {

  private final ProductRepository productRepository;
  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final PagTesouroClient pagTesouroClient;

  public PaymentService(
      ProductRepository productRepository,
      PaymentRepository paymentRepository,
      OrderRepository orderRepository,
      PagTesouroClient pagTesouroClient) {
    this.productRepository = productRepository;
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.pagTesouroClient = pagTesouroClient;
  }

  /**
   * Efetua o checkout simplificado/direto de um produto para doadores logados ou anônimos.
   *
   * @param request Os dados de checkout contendo produto, quantidade e dados do pagador.
   * @return Um {@link Mono} contendo o DTO do checkout resolvido com a URL de redirecionamento.
   */
  @Transactional
  public Mono<CheckoutResponseDTO> checkoutDirect(CheckoutRequestDTO request) {
    if (request.productID() == null) {
      throw new IllegalArgumentException("O ID do produto é obrigatório para checkout direto!");
    }
    if (request.quantity() == null || request.quantity() < 1) {
      throw new IllegalArgumentException("A quantidade deve ser maior ou igual a 1 para checkout direto!");
    }

    if (request.contributorCpfCnpj() == null || request.contributorCpfCnpj().trim().isEmpty()) {
      throw new IllegalArgumentException("O CPF/CNPJ do pagador é obrigatório para checkout simplificado!");
    }
    if (request.contributorName() == null || request.contributorName().trim().isEmpty()) {
      throw new IllegalArgumentException("O nome do pagador é obrigatório para checkout simplificado!");
    }

    Product product = productRepository.findById(request.productID())
        .orElseThrow(() -> new FindException("Produto com ID " + request.productID() + " não localizado."));

    // Calcula o valor total consolidado
    BigDecimal amountTotal = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

    // Cria e persiste o pagamento localmente (status CREATED)
    Payment payment = createPayment(request, amountTotal);

    // Monta a requisição de cobrança para a API do PagTesouro da STN.
    PagTesouroRequestDTO ptRequest = new PagTesouroRequestDTO(
        Integer.parseInt(product.getCodeService()),
        payment.getReferenceNumber().toString(),
        payment.getCompetence(),
        payment.getExpiredAt().format(DateTimeFormatter.ofPattern("ddMMyyyy")),
        payment.getContributorCpfCnpj(),
        payment.getContributorName(),
        payment.getAmount(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        2, // Modo navegação: nova aba (Recomendado para UX Desktop)
        pagTesouroClient.getProperties().url_notificacao()
    );

    // Efetua a chamada HTTP reativa ao PagTesouro e atualiza o registro local com a resposta
    return pagTesouroClient.createPayment(ptRequest)
        .onErrorMap(WebClientResponseException.class, ex -> {
            String details = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 401) {
                return new IllegalArgumentException("Erro de autenticação com o PagTesouro (Salinas). Verifique se o Token de Salinas está configurado corretamente e é válido. Detalhes: " + details);
            }
            return new RuntimeException("Erro na integração com o PagTesouro: " + ex.getStatusCode() + " - Detalhes: " + details, ex);
        })
        .publishOn(Schedulers.boundedElastic())
        .flatMap( ptResponse -> {
          payment.setPagtesouroPaymentId(ptResponse.idPayment());
          payment.setNextUrl(ptResponse.nextUrl());

          return Mono.fromCallable(() -> paymentRepository.save(payment))
            .map(saved -> new CheckoutResponseDTO(
                saved.getId(),
                saved.getPagtesouroPaymentId(),
                saved.getAmount(),
                saved.getStatus().name(),
                saved.getNextUrl()
          ));
        });
  }

  /**
   * Efetua o checkout de um Pedido (carrinho de compras) integrado para o estudante logado no portal do IFNMG.
   *
   * @param orderId O ID único do pedido a ser pago.
   * @param request Os dados cadastrais do contribuinte que está pagando a guia.
   * @param user O usuário estudante autenticado requisitante.
   * @return Um {@link Mono} contendo o DTO do checkout com a URL de redirecionamento do PagTesouro.
   */
  @Transactional
  public Mono<CheckoutResponseDTO> checkoutOrder(UUID orderId, CheckoutRequestDTO request, User user) {
    // 1. Localiza o pedido no banco
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new FindException("Pedido com ID " + orderId + " não localizado."));

    // 2. Proteção de Segurança do IFNMG: Somente o próprio dono do pedido ou ADMINs podem pagar
    if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
      throw new SecurityException("Acesso negado a este pedido.");
    }

    // 3. Filtra apenas os itens de pedido que de fato estão pendentes de liquidação
    List<OrderItem> pendingItems = order.getOrderItems().stream()
        .filter(item -> item.getStatus() == OrderItemStatus.PENDING)
        .toList();

    if (pendingItems.isEmpty()) {
      throw new IllegalStateException("Não existem itens pendentes de liquidação para este pedido.");
    }

    // 4. Calcula a receita total consolidada da tentativa de pagamento
    BigDecimal amountTotal = pendingItems.stream()
        .map(OrderItem::getTotalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // 5. Instancia a entidade de pagamento local vinculando-a aos itens pendentes do pedido
    Payment payment = new Payment();
    payment.setAmount(amountTotal);
    payment.setPrincipalAmount(amountTotal);
    payment.setStatus(PaymentStatus.CREATED);
    payment.setCompetence(LocalDate.now().format(DateTimeFormatter.ofPattern("MMyyyy")));
    payment.setExpiredAt(LocalDate.now().plusDays(2));
    String fullName = user.getName();
    if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
        fullName += " " + user.getLastName();
    }

    String contributorName = (request.contributorName() != null && !request.contributorName().trim().isEmpty())
        ? request.contributorName() : fullName;

    // Truncamento defensivo para respeitar o limite de 45 caracteres da coluna VARCHAR(45) no banco
    if (contributorName.length() > 45) {
        contributorName = contributorName.substring(0, 45);
    }

    String contributorCpf = (request.contributorCpfCnpj() != null && !request.contributorCpfCnpj().trim().isEmpty())
        ? request.contributorCpfCnpj() : user.getCpf();

    payment.setContributorName(contributorName);
    payment.setContributorCpfCnpj(contributorCpf);
    payment.setOrderItems(pendingItems); // Mapeamento bidirecional seguro na tabela 'payment_items'

    Payment savedPayment = paymentRepository.saveAndFlush(payment);

    // 6. Obtém o código de serviço para o SISGRU (assumindo o primeiro produto dos itens)
    String serviceCode = pendingItems.get(0).getProduct().getCodeService();

    // 7. Estrutura a payload de integração com a API da STN
    PagTesouroRequestDTO ptRequest = new PagTesouroRequestDTO(
        Integer.parseInt(serviceCode),
        savedPayment.getReferenceNumber().toString(),
        savedPayment.getCompetence(),
        savedPayment.getExpiredAt().format(DateTimeFormatter.ofPattern("ddMMyyyy")),
        savedPayment.getContributorCpfCnpj(),
        savedPayment.getContributorName(),
        savedPayment.getAmount(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        request.isMobile()? 1:2,
        pagTesouroClient.getProperties().url_notificacao()
    );

    // 8. Efetua a requisição reativa não-bloqueante
    return pagTesouroClient.createPayment(ptRequest)
        .onErrorMap(WebClientResponseException.class, ex -> {
            String details = ex.getResponseBodyAsString();
            if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 401) {
                return new IllegalArgumentException("Erro de autenticação com o PagTesouro (Salinas). Verifique se o Token de Salinas está configurado corretamente e é válido. Detalhes: " + details);
            }
            return new RuntimeException("Erro na integração com o PagTesouro: " + ex.getStatusCode() + " - Detalhes: " + details, ex);
        })
        .publishOn(Schedulers.boundedElastic())
        .flatMap(ptResponse -> {
          savedPayment.setPagtesouroPaymentId(ptResponse.idPayment());
          savedPayment.setNextUrl(ptResponse.nextUrl());

          return Mono.fromCallable(() -> paymentRepository.save(savedPayment))
          .map(saved -> new CheckoutResponseDTO(
              saved.getId(),
              saved.getPagtesouroPaymentId(),
              saved.getAmount(),
              saved.getStatus().name(),
              saved.getNextUrl()
          ));
        });
  }

  /**
   * Resgata de forma paginada e reativa o histórico de todas as faturas e tentativas de pagamento
   * efetuadas pelo contribuinte logado no portal com base em seu CPF.
   *
   * @param user O usuário estudante autenticado requisitante
   * @param pageable Configuração de paginação (número de página, tamanho e ordenação)
   * @return Um {@link Mono} contendo a página de DTOs correspondente
   */
  @Transactional(readOnly = true)
  public Mono<Page<PaymentHistoryResponseDTO>> getMyPayments(User user, Pageable pageable) {
    return Mono.fromCallable(() -> {
      Page<Payment> payments = paymentRepository.findByContributorCpfCnpj(user.getCpf(), pageable);
      return payments.map(PaymentHistoryResponseDTO::new);
    }).subscribeOn(Schedulers.boundedElastic());
  }

  /**
   * Auxiliar responsável por instanciar e persistir o registro inicial de pagamento local.
   */
  private Payment createPayment(CheckoutRequestDTO request, BigDecimal amountTotal) {
    Payment payment = new Payment();
    payment.setAmount(amountTotal);
    payment.setPrincipalAmount(amountTotal);
    payment.setStatus(PaymentStatus.CREATED);

    // Mês e Ano de competência atual (MMyyyy)
    payment.setCompetence(LocalDate.now().format(DateTimeFormatter.ofPattern("MMyyyy")));

    // Expiração em 2 dias
    payment.setExpiredAt(LocalDate.now().plusDays(2));

    // Dados de quem está pagando
    payment.setContributorName(request.contributorName());
    payment.setContributorCpfCnpj(request.contributorCpfCnpj());

    return paymentRepository.saveAndFlush(payment);
  }
}
