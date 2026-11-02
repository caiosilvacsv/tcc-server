package br.edu.ifnmg.pagtesouro.services.payment;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
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
import br.edu.ifnmg.pagtesouro.domain.user.CpfValidator;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.exceptions.PagTesouroIntegrationException;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.PagTesouroClient;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroRequestDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto.PagTesouroResponseDTO;
import br.edu.ifnmg.pagtesouro.infra.pagtesouro.enums.PagTesouroErrorCode;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import br.edu.ifnmg.pagtesouro.services.config.SystemConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
@Slf4j
@Service
public class PaymentService {

  private final ProductRepository productRepository;
  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final PagTesouroClient pagTesouroClient;
  private final SystemConfigurationService systemConfigurationService;

  public PaymentService(
      ProductRepository productRepository,
      PaymentRepository paymentRepository,
      OrderRepository orderRepository,
      PagTesouroClient pagTesouroClient,
      SystemConfigurationService systemConfigurationService) {
    this.productRepository = productRepository;
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.pagTesouroClient = pagTesouroClient;
    this.systemConfigurationService = systemConfigurationService;
  }

  /**
   * Efetua o checkout direto e simplificado para um produto específico sem exigir carrinho prévio.
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

    BigDecimal amountTotal = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));

    String normalizedCpf = CpfValidator.normalize(request.contributorCpfCnpj());
    String contributorCpf = (normalizedCpf != null && !normalizedCpf.isEmpty()) ? normalizedCpf : request.contributorCpfCnpj().trim();

    Order guestOrder = new Order();
    guestOrder.setUser(null);
    guestOrder.setGuestCpf(contributorCpf);
    guestOrder.setGuestName(request.contributorName().trim());
    guestOrder.setTotalAmount(amountTotal);
    guestOrder.setCreateAt(Instant.now());
    guestOrder.setOrderItems(new ArrayList<>());

    OrderItem orderItem = new OrderItem();
    orderItem.setOrder(guestOrder);
    orderItem.setProduct(product);
    orderItem.setQuantity(request.quantity());
    orderItem.setTotalAmount(amountTotal);
    orderItem.setStatus(OrderItemStatus.PENDING);
    guestOrder.getOrderItems().add(orderItem);

    Order savedGuestOrder = orderRepository.save(guestOrder);
    List<OrderItem> savedItems = savedGuestOrder.getOrderItems();

    Payment payment = createInitialPayment(
        amountTotal,
        request.contributorName().trim(),
        contributorCpf,
        savedItems != null ? new ArrayList<>(savedItems) : new ArrayList<>()
    );

    return executePagTesouroCheckout(payment, product.getCodeService(), request.isMobile());
  }

  /**
   * Efetua o checkout de um Pedido (carrinho) integrado para o estudante logado no portal do IFNMG.
   */
  @Transactional
  public List<CheckoutResponseDTO> checkoutOrder(UUID orderId, OrderCheckoutRequestDTO request, User user) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new FindException("Pedido com ID " + orderId + " não localizado."));

    if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
      throw new SecurityException("Acesso negado a este pedido.");
    }

    if (order.getStatus() == OrderStatus.COMPLETED) {
      throw new ConflictException("Este pedido já foi pago e liquidado anteriormente.");
    }

    if (order.getStatus() == OrderStatus.CANCELLED) {
      throw new IllegalStateException("Não é possível realizar checkout de um pedido cancelado.");
    }

    List<OrderItem> pendingItems = order.getOrderItems().stream()
        .filter(item -> item.getStatus() == OrderItemStatus.PENDING)
        .toList();

    if (pendingItems.isEmpty()) {
      throw new ConflictException("Não existem itens pendentes de liquidação para este pedido.");
    }

    String contributorName = resolveContributorName(request, user);
    String contributorCpf = (request != null && request.contributorCpfCnpj() != null &&
        !request.contributorCpfCnpj().trim().isEmpty())
        ? request.contributorCpfCnpj().trim() : user.getCpf();

    boolean isMobile = request != null && request.isMobile();

    List<Payment> priorPendingPayments = paymentRepository.findPendingPaymentsByOrderId(orderId);

    Map<String, List<OrderItem>> itemsByService = pendingItems.stream()
        .collect(Collectors.groupingBy(item -> item.getProduct().getCodeService()));

    List<CheckoutResponseDTO> responses = new ArrayList<>();

    for (Map.Entry<String, List<OrderItem>> entry : itemsByService.entrySet()) {
      String serviceCode = entry.getKey();
      List<OrderItem> groupItems = entry.getValue();

      if (priorPendingPayments != null) {
        for (Payment prior : priorPendingPayments) {
          if (prior.getOrderItems() != null && prior.getOrderItems().stream()
              .anyMatch(item -> item.getProduct() != null && serviceCode.equals(item.getProduct().getCodeService()))) {
            log.info("Cancelando sessão de pagamento pendente anterior (id={}) para o pedido {} e serviço {}: a STN proíbe reutilização de proximaUrl no iframe.",
                prior.getId(), orderId, serviceCode);
            prior.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(prior);
          }
        }
      }

      BigDecimal groupAmount = groupItems.stream()
          .map(OrderItem::getTotalAmount)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      Payment payment = createInitialPayment(groupAmount, contributorName, contributorCpf, groupItems);
      responses.add(executePagTesouroCheckout(payment, serviceCode, isMobile));
    }

    return responses;
  }

  @Transactional(readOnly = true)
  public PageResponseDTO<PaymentHistoryResponseDTO> getMyPayments(User user, Pageable pageable) {
    Page<Payment> payments = paymentRepository.findByUserId(user.getId(), pageable);
    return new PageResponseDTO<>(payments.map(PaymentHistoryResponseDTO::new));
  }

  private String resolveContributorName(OrderCheckoutRequestDTO request, User user) {
    String fullName = user.getName();
    if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
      fullName += " " + user.getLastName().trim();
    }

    String contributorName = (request != null && request.contributorName() != null && !request.contributorName().trim().isEmpty())
        ? request.contributorName().trim() : fullName;

    if (contributorName.length() > 45) {
      contributorName = contributorName.substring(0, 45);
    }
    return contributorName;
  }

  private Payment createInitialPayment(BigDecimal amount, String contributorName, String contributorCpf, List<OrderItem> orderItems) {
    int expDays = systemConfigurationService != null
        ? systemConfigurationService.getPaymentExpirationDays()
        : 2;

    Payment payment = new Payment();
    payment.setAmount(amount);
    payment.setPrincipalAmount(amount);
    payment.setStatus(PaymentStatus.CREATED);
    payment.setCompetence(LocalDate.now().format(DateTimeFormatter.ofPattern("MMyyyy")));
    payment.setExpiredAt(LocalDate.now().plusDays(expDays > 0 ? expDays : 2));
    payment.setContributorName(contributorName);
    payment.setContributorCpfCnpj(contributorCpf);

    if (orderItems != null && !orderItems.isEmpty()) {
      payment.setOrderItems(new ArrayList<>(orderItems));
    }

    return paymentRepository.saveAndFlush(payment);
  }

  public String sanitizeNotificationUrl(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      return null;
    }
    String trimmed = rawUrl.trim().toLowerCase();
    List<String> allowedDomains = systemConfigurationService != null
        ? systemConfigurationService.getAllowedNotificationDomains()
        : List.of(".gov.br", ".edu.br");

    boolean matches = allowedDomains.stream().anyMatch(domain -> trimmed.contains(domain.toLowerCase()));
    if (!matches) {
      log.warn("URL de notificação '{}' ignorada: a STN exige domínio oficial ({}) para evitar erro C0024.",
          rawUrl, String.join(", ", allowedDomains));
      return null;
    }
    return rawUrl.trim();
  }

  private PagTesouroRequestDTO buildPagTesouroRequest(Payment payment, String serviceCode, boolean isMobile) {
    String notifUrl = systemConfigurationService != null
        ? systemConfigurationService.getNotificationUrl()
        : pagTesouroClient.getProperties().url_notificacao();

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
        sanitizeNotificationUrl(notifUrl)
    );
  }

  private CheckoutResponseDTO executePagTesouroCheckout(Payment payment, String serviceCode, boolean isMobile) {
    PagTesouroRequestDTO ptRequest = buildPagTesouroRequest(payment, serviceCode, isMobile);

    PagTesouroResponseDTO ptResponse;
    try {
      ptResponse = pagTesouroClient.createPayment(ptRequest)
          .block(Duration.ofSeconds(15));
    } catch (WebClientResponseException ex) {
      String details = ex.getResponseBodyAsString();
      int status = ex.getStatusCode().value();
      if (status == 401 || status == 403) {
        throw new PagTesouroIntegrationException(PagTesouroErrorCode.AUTH_ERROR, details);
      }
      if (status == 422) {
        PagTesouroErrorCode code = PagTesouroErrorCode.UNKNOWN_ERROR;
        if (details != null && details.contains("C0024")) code = PagTesouroErrorCode.C0024;
        else if (details != null && details.contains("C0012")) code = PagTesouroErrorCode.C0012;
        else if (details != null && details.contains("C0001")) code = PagTesouroErrorCode.C0001;
        throw new PagTesouroIntegrationException(code, details);
      }
      if (status >= 500) {
        throw new PagTesouroIntegrationException(PagTesouroErrorCode.GATEWAY_ERROR, details);
      }
      throw new PagTesouroIntegrationException("Erro retornado pelo PagTesouro: " + ex.getStatusCode(), HttpStatus.BAD_GATEWAY, "PAGTESOURO_ERROR", details);
    }

    if (ptResponse == null) {
      throw new PagTesouroIntegrationException(PagTesouroErrorCode.GATEWAY_ERROR, "Resposta nula recebida da API do PagTesouro.");
    }

    payment.setPagtesouroPaymentId(ptResponse.idPayment());
    payment.setNextUrl(ptResponse.nextUrl());

    Payment saved = paymentRepository.save(payment);

    List<String> itemsDesc = new ArrayList<>();
    String categoryLabel = null;
    if (saved.getOrderItems() != null) {
      for (OrderItem oi : saved.getOrderItems()) {
        String title = oi.getProduct() != null ? oi.getProduct().getTitle() : "Serviço Institucional";
        int qty = oi.getQuantity() != null ? oi.getQuantity() : 1;
        itemsDesc.add(qty + "x " + title);
        if (categoryLabel == null && oi.getProduct() != null && oi.getProduct().getCategory() != null) {
          categoryLabel = oi.getProduct().getCategory().getDisplayName();
        }
      }
    }
    String serviceTitle = "Guia SISGRU #" + serviceCode + (categoryLabel != null ? " — " + categoryLabel : "");

    return new CheckoutResponseDTO(
        saved.getId(),
        saved.getPagtesouroPaymentId(),
        saved.getAmount(),
        saved.getStatus().name(),
        saved.getNextUrl(),
        serviceCode,
        serviceTitle,
        itemsDesc
    );
  }
}
