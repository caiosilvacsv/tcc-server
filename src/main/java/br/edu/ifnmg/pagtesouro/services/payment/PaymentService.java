package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItemStatus;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import br.edu.ifnmg.pagtesouro.domain.common.PageResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.DirectCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.OrderCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.PaymentHistoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroRequestDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
   * Efetua o checkout direto e simplificado para um produto específico sem exigir carrinho prévio.
   * <p>
   * **Fluxo de Convidado (Opção B):**
   * Cria um Pedido (Order) e um Item (OrderItem) vinculados aos dados cadastrais informados do convidado (guest),
   * garantindo que a refeição possa ser auditada e resgatada no balcão de atendimento por conferência de CPF.
   * </p>
   *
   * @param request Os dados de checkout contendo produto, quantidade e dados do pagador.
   * @return O DTO do checkout resolvido com a URL de redirecionamento.
   */
  @Transactional
  public CheckoutResponseDTO checkoutDirect(DirectCheckoutRequestDTO request) {
    if (request.productId() == null) {
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

    Product product = productRepository.findById(request.productId())
        .orElseThrow(() -> new FindException("Produto com ID " + request.productId() + " não localizado."));

    if (!product.isActive()) {
      throw new ConflictException("O produto não está disponível!");
    }

    // Calcula o valor total consolidado
    BigDecimal amountTotal = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

    // 1. Cria a Ordem física para o comprador anônimo (guest)
    Order guestOrder = new Order();
    guestOrder.setUser(null);
    guestOrder.setGuestCpf(request.contributorCpfCnpj().trim());
    guestOrder.setGuestName(request.contributorName().trim());
    guestOrder.setTotalAmount(amountTotal);
    guestOrder.setCreateAt(Instant.now());
    guestOrder.setOrderItems(new ArrayList<>());

    // 2. Cria o item do pedido associado ao produto com status PENDING
    OrderItem orderItem = new OrderItem();
    orderItem.setOrder(guestOrder);
    orderItem.setProduct(product);
    orderItem.setQuantity(request.quantity());
    orderItem.setTotalAmount(amountTotal);
    orderItem.setStatus(OrderItemStatus.PENDING);
    guestOrder.getOrderItems().add(orderItem);

    Order savedGuestOrder = orderRepository.save(guestOrder);
    List<OrderItem> savedItems = savedGuestOrder.getOrderItems();

    // 3. Cria e persiste o pagamento localmente vinculado aos itens do convidado (status CREATED)
    Payment payment = createInitialPayment(
        amountTotal,
        request.contributorName().trim(),
        request.contributorCpfCnpj().trim(),
        savedItems != null ? new ArrayList<>(savedItems) : new ArrayList<>()
    );

    return executePagTesouroCheckout(payment, product.getCodeService(), request.isMobile());
  }

  /**
   * Efetua o checkout de um Pedido (carrinho) integrado para o estudante logado no portal do IFNMG.
   * Agrupa automaticamente os itens pendentes por código de serviço SISGRU, gerando uma guia PagTesouro para cada serviço.
   *
   * @param orderId O ID único do pedido a ser pago.
   * @param request Os dados cadastrais opcionais do contribuinte pagador.
   * @param user O usuário estudante autenticado requisitante.
   * @return A lista com os DTOs de checkout de cada serviço com suas respectivas URLs do PagTesouro.
   */
  @Transactional
  public List<CheckoutResponseDTO> checkoutOrder(UUID orderId, OrderCheckoutRequestDTO request, User user) {
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

    String contributorName = resolveContributorName(request, user);
    String contributorCpf = (request != null && request.contributorCpfCnpj() != null &&
        !request.contributorCpfCnpj().trim().isEmpty())
        ? request.contributorCpfCnpj().trim() : user.getCpf();

    boolean isMobile = request != null && request.isMobile();

    // 4. Agrupa os itens pendentes por código de serviço SISGRU
    Map<String, List<OrderItem>> itemsByService = pendingItems.stream()
        .collect(Collectors.groupingBy(item -> item.getProduct().getCodeService()));

    List<CheckoutResponseDTO> responses = new ArrayList<>();

    for (Map.Entry<String, List<OrderItem>> entry : itemsByService.entrySet()) {
      String serviceCode = entry.getKey();
      List<OrderItem> groupItems = entry.getValue();

      // Calcula a receita total consolidada deste grupo de serviço
      BigDecimal groupAmount = groupItems.stream()
          .map(OrderItem::getTotalAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      // Persiste o pagamento vinculando-o aos itens do grupo
      Payment payment = createInitialPayment(groupAmount, contributorName, contributorCpf, groupItems);

      responses.add(executePagTesouroCheckout(payment, serviceCode, isMobile));
    }

    return responses;
  }

  /**
   * Resgata de forma paginada o histórico de todas as faturas e tentativas de pagamento
   * efetuadas pelo contribuinte logado no portal com base em seu usuário.
   *
   * @param user O usuário estudante autenticado requisitante
   * @param pageable Configuração de paginação (número de página, tamanho e ordenação)
   * @return A página de DTOs correspondente
   */
  @Transactional(readOnly = true)
  public PageResponseDTO<PaymentHistoryResponseDTO> getMyPayments(User user, Pageable pageable) {
    Page<Payment> payments = paymentRepository.findByContributorCpfCnpj(user.getCpf(), pageable);
    return new PageResponseDTO<>(payments.map(PaymentHistoryResponseDTO::new));
  }

  /**
   * Resolve o nome completo do contribuinte pagador respeitando regras de auditoria e tamanho de coluna.
   */
  private String resolveContributorName(OrderCheckoutRequestDTO request, User user) {
    String fullName = user.getName();
    if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
      fullName += " " + user.getLastName().trim();
    }

    String contributorName = (request != null && request.contributorName() != null && !request.contributorName().trim().isEmpty())
        ? request.contributorName().trim() : fullName;

    // Truncamento defensivo para respeitar o limite de 45 caracteres da coluna VARCHAR(45) no banco
    if (contributorName.length() > 45) {
      contributorName = contributorName.substring(0, 45);
    }
    return contributorName;
  }

  /**
   * Auxiliar responsável por instanciar e persistir o registro inicial de pagamento local.
   */
  private Payment createInitialPayment(BigDecimal amount, String contributorName, String contributorCpf, List<OrderItem> orderItems) {
    Payment payment = new Payment();
    payment.setAmount(amount);
    payment.setPrincipalAmount(amount);
    payment.setStatus(PaymentStatus.CREATED);

    // Mês e Ano de competência atual (MMyyyy)
    payment.setCompetence(LocalDate.now().format(DateTimeFormatter.ofPattern("MMyyyy")));

    // Expiração em 2 dias
    payment.setExpiredAt(LocalDate.now().plusDays(2));

    payment.setContributorName(contributorName);
    payment.setContributorCpfCnpj(contributorCpf);

    if (orderItems != null && !orderItems.isEmpty()) {
      payment.setOrderItems(new ArrayList<>(orderItems));
    }

    return paymentRepository.saveAndFlush(payment);
  }

  /**
   * Constrói o payload oficial exigido pela API do PagTesouro (STN).
   */
  private PagTesouroRequestDTO buildPagTesouroRequest(Payment payment, String serviceCode, boolean isMobile) {
    return new PagTesouroRequestDTO(
        Integer.parseInt(serviceCode),
        payment.getReferenceNumber() != null ? payment.getReferenceNumber().toString() : "0",
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
        isMobile ? 1 : 2,
        pagTesouroClient.getProperties().url_notificacao()
    );
  }

  /**
   * Executa a requisição síncrona ao PagTesouro e consolida a resposta localmente.
   */
  private CheckoutResponseDTO executePagTesouroCheckout(Payment payment, String serviceCode, boolean isMobile) {
    PagTesouroRequestDTO ptRequest = buildPagTesouroRequest(payment, serviceCode, isMobile);

    PagTesouroResponseDTO ptResponse;
    try {
      ptResponse = pagTesouroClient.createPayment(ptRequest)
          .block(Duration.ofSeconds(15));
    } catch (WebClientResponseException ex) {
      String details = ex.getResponseBodyAsString();
      if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 401) {
        throw new IllegalArgumentException(
            "Erro de autenticação com o PagTesouro. Verifique se o Token está configurado corretamente e é válido. Detalhes: " + details);
      }
      throw new RuntimeException("Erro na integração com o PagTesouro: " + ex.getStatusCode() + " - Detalhes: " + details, ex);
    }

    if (ptResponse == null) {
      throw new RuntimeException("Resposta nula recebida da API do PagTesouro.");
    }

    payment.setPagtesouroPaymentId(ptResponse.idPayment());
    payment.setNextUrl(ptResponse.nextUrl());

    Payment saved = paymentRepository.save(payment);
    return new CheckoutResponseDTO(
        saved.getId(),
        saved.getPagtesouroPaymentId(),
        saved.getAmount(),
        saved.getStatus().name(),
        saved.getNextUrl(),
        serviceCode
    );
  }
}
