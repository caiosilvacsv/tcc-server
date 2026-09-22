# 🏛️ Modernização de Meios de Pagamento - IFNMG Campus Salinas (Backend)

Este repositório contém a API REST (*backend*) corporativa e de alta robustez desenvolvida como a contribuição prática do Trabalho de Conclusão de Curso (TCC) de **Caio da Silva Viana**, sob a orientação de **Danielle Miranda Rodrigues**, para o curso de **Bacharelado em Sistemas de Informação** no **Instituto Federal do Norte de Minas Gerais (IFNMG) - Campus Salinas**.

O objetivo primordial desta aplicação é automatizar e modernizar a arrecadação de receitas públicas institucionais (como tíquetes do restaurante estudantil, taxas de biblioteca, segundas vias de documentos e doações), integrando o campus diretamente à API do **PagTesouro** da **Secretaria do Tesouro Nacional (STN)**.

---

## 🚀 1. O Que Foi Feito (Histórico de Engenharia & Entregas)

Nesta etapa do projeto, foram implementadas as seguintes soluções arquiteturais e funcionais:

### A. Núcleo de Domínio e Modelo Rico (DDD)

* **Cancelamento Atômico de Pedidos (`Order.java`)**: Implementado o método rico `order.cancel()`, encapsulando as regras de transição de estado. O cancelamento é idempotente, propaga em cascata para os itens pendentes (`PENDING` -> `CANCELLED`) e rejeita categoricamente o cancelamento caso haja itens quitados (`PAID`) ou já consumidos (`EXCHANGED`).
* **Suporte a Convidados/Anônimos (`BuyerType`)**: Criada a migration Flyway `V7__allow_guest_orders.sql`, viabilizando compras avulsas e doações por cidadãos sem cadastro prévio, persistindo `guest_cpf` e `guest_name` de forma desnormalizada para auditoria fiscal.
* **Prevenção de Duplicidade de Produtos**: Adicionada verificação no `ProductRepository` e `ProductService` (`existsByTitleIgnoreCase` e `existsByTitleIgnoreCaseAndIdNot`), impedindo o cadastro ou renomeação para produtos com títulos comerciais já existentes.
* **Conversor Monetário de Precisão (`MoneyToCentsConverter`)**: Mapeamento do `BigDecimal` do Java para colunas `INTEGER` (centavos) no PostgreSQL, eliminando erros de arredondamento de ponto flutuante e otimizando o espaço físico em disco.

### B. Motor de Checkout & Pagamentos Governamentais

* **Checkout Multi-Serviço SISGRU**: O `PaymentService` analisa o carrinho de compras e agrupa automaticamente os itens pelo seu código de serviço do SISGRU (`codeService`). Se o pedido possuir itens de receitas fiscais distintas, a API gera chamadas separadas e transparentes à STN, devolvendo a lista consolidada de faturas (`CheckoutResponseDTO`).
* **Checkout Direto Anônimo**: Geração imediata de guia/Pix para contribuintes não autenticados, com geração de número de referência sequencial exclusivo (`reference_number`) atrelado a uma SEQUENCE física do banco de dados.
* **Notificação em Tempo Real via Server-Sent Events (SSE)**: Rota reativa `GET /payments/{paymentId}/sse` via `PaymentNotificationService`. O frontend estabelece uma conexão unidirecional leve e recebe automaticamente o evento `PAYMENT_PAID` no instante exato da confirmação bancária.

### C. Resiliência & Daemon de Conciliação Failsafe

* **Daemon em Segundo Plano (`PaymentDaemonService`)**: Job agendado (`@Scheduled`) de periodicidade configurável que varre transações pendentes e executa a conciliação ativa reativa contra a API da STN, garantindo que pagamentos quitados sejam baixados mesmo se o webhook do governo falhar.
* **Expiração de Pedidos Abandonados**: O Daemon cancela automaticamente pedidos pendentes antigos sem intenção de pagamento há mais de 10 dias (`api.order.abandoned-expiration-days`), liberando recursos e mantendo o banco higienizado.
* **Proteção contra Quitação Tardia**: Se a STN confirmar o pagamento de uma fatura de um pedido que já havia sido cancelado localmente por decurso de prazo, o sistema não ressuscita itens cancelados e impede incoerências de estoque.

### D. Carteira e Baixa de Tíquetes no Refeitório

* **Fila FIFO de Consumo**: Consulta indexada de tíquetes disponíveis por CPF (`GET /orders/tickets`), atendendo tanto alunos cadastrados quanto convidados.
* **Baixa em Lote Atômica (`PUT /orders/items/exchange`)**: Permite aos operadores do guichê do restaurante institucional efetuar a troca de múltiplos tíquetes de uma só vez (`BatchExchangeRequestDTO`), com verificação de posse, proteção estrita contra gasto duplo e auditoria com `exchangedAt`.

### E. Infraestrutura, Segurança e DevOps

* **Autenticação Stateless JWT**: Segurança via Spring Security com tokens assinados com algoritmo simétrico HMAC256.
* **Endpoint de Perfil (`GET /auth/me`)**: Retorno seguro dos dados e permissões do usuário logado via `UserResponseDTO`.
* **Promoção Automática de Administrador (`AdminInitializer`)**: Inicializador de startup que audita o banco e promove automaticamente o e-mail definido na variável `INITIAL_ADMIN_EMAIL` para a role `ADMIN`.
* **Paginação Limpa (`PageResponseDTO`)**: DTO genérico de 5 elementos que remove o excesso de metadados padrão do Spring Data, ideal para montagem de tabelas fluidas no frontend.
* **Tratamento Global de Erros (`RestExceptionHandler`)**: Interceptador `@RestControllerAdvice` que captura exceções e garante respostas padronizadas em `application/json` com os códigos HTTP semânticos correspondentes (400, 401, 403, 404, 409, 422).
* **Documentação Swagger UI / OpenAPI 3**: Integração viva do SpringDoc exposta diretamente no caminho `/swagger` com suporte à autenticação Bearer.
* **Qualidade de Código**: Suite automatizada com **94 testes unitários e de integração** 100% herméticos e aprovados com 100% de sucesso (`mvn clean test`).

---

## 📋 2. O Que o Servidor Está Disposto a Fazer (Capacidades da API)

O `tcc-server` está completamente estruturado e pronto para atender os seguintes subsistemas da instituição:

### Matriz de Endpoints Disponíveis

| Método | Endpoint | Permissão | Descrição / Capacidade do Servidor |
| :--- | :--- | :--- | :--- |
| **`POST`** | `/auth/login` | Pública | Autentica o usuário via e-mail e senha, retornando o Token JWT de acesso. |
| **`POST`** | `/auth/register` | Pública | Cadastra um novo estudante/servidor com validação matemática de CPF e e-mail único. |
| **`GET`** | `/auth/me` | Autenticado | Retorna os dados cadastrais (ID, Nome, E-mail, CPF, Perfil) do usuário logado. |
| **`GET`** | `/product` | Pública | Retorna os produtos ativos no catálogo. Se `activeOnly=false` e solicitante for `ADMIN`, lista também inativos. |
| **`GET`** | `/product/{id}` | Pública | Consulta os detalhes completos de um produto pelo seu ID. |
| **`GET`** | `/product/categories` | Pública | Retorna a lista de categorias disponíveis no catálogo (`key`, `value`, `displayName`). |
| **`POST`** | `/product` | **ADMIN** | Cadastra um novo produto/taxa com código SISGRU, aplicando validação contra duplicidade de título. |
| **`PUT`** | `/product/{id}` | **ADMIN** | Atualiza os dados fiscais e comerciais de um produto existente com checagem de unicidade. |
| **`DELETE`** | `/product/{id}` | **ADMIN** | Desativa logicamente o produto (Soft Delete), preservando o histórico financeiro. |
| **`POST`** | `/orders` | Autenticado | Cria um novo pedido vinculado ao carrinho de compras do aluno com múltiplos itens. |
| **`GET`** | `/orders/{id}` | Autenticado | Consulta os detalhes, status e itens de um pedido específico. |
| **`GET`** | `/orders` | Autenticado | Lista todos os pedidos pertencentes ao usuário autenticado. |
| **`PATCH`** | `/orders/{id}/cancel` | Autenticado | Cancela um pedido pendente sem necessidade de payload, aplicando regras DDD. |
| **`GET`** | `/orders/guest` | Pública | Consulta os pedidos e compras efetuados por um contribuinte anônimo através do seu CPF. |
| **`GET`** | `/orders/tickets` | Pública | Consulta a carteira de tíquetes quitados e prontos para consumo associados a um CPF. |
| **`PUT`** | `/orders/items/exchange` | **ADMIN** | Realiza a baixa física atômica de tíquetes no guichê do restaurante universitário. |
| **`POST`** | `/payments/checkout/order` | Autenticado | Realiza o checkout do carrinho, gerando faturas no PagTesouro agrupadas por código SISGRU. |
| **`POST`** | `/payments/checkout/direct` | Pública | Realiza o checkout avulso para visitantes e doadores sem conta prévia. |
| **`GET`** | `/payments` | Autenticado | Consulta de forma paginada e limpa (`PageResponseDTO`) o histórico de pagamentos do CPF logado. |
| **`GET`** | `/payments/{paymentId}/sse` | Pública | Canal Server-Sent Events (SSE) para atualização instantânea na tela de pagamento. |
| **`POST`** | `/api/payments/webhook` | Pública (Token STN) | Endpoint de callback para recebimento de notificações automáticas de compensação da STN. |
| **`GET`** | `/swagger` | Pública | Interface interativa gráfica do Swagger UI para exploração e testes de todas as rotas. |

---

## 🛠️ 3. Tecnologias Utilizadas

* **Linguagem & Plataforma**: Java 25 & Spring Boot 4.0.6
* **Persistência de Dados**: Spring Data JPA, Hibernate, PostgreSQL 16
* **Migrações de Banco**: Flyway Migration (scripts versionados V1 a V7)
* **Comunicação Reativa**: Spring WebFlux (`WebClient`) & Spring Web MVC (`SseEmitter`)
* **Segurança**: Spring Security 6, JWT (JSON Web Tokens), BCrypt
* **Documentação de API**: SpringDoc OpenAPI 3 (Swagger UI)
* **Testes Automatizados**: JUnit 5, Mockito 5.x
* **DevOps & Contêineres**: Docker & Docker Compose

---

## ⚙️ 4. Variáveis de Ambiente e Configuração

O sistema segue os preceitos do **Twelve-Factor App**, permitindo configuração completa via variáveis de ambiente:

| Variável | Descrição | Padrão |
| :--- | :--- | :--- |
| `PORT` | Porta HTTP em que a aplicação responderá. | `8080` |
| `ACTIVE_PROFILE` | Perfil ativo do Spring (`dev`, `prod`). | `dev` |
| `POSTGRESSQL_URL` | URL de conexão JDBC do banco de dados PostgreSQL. | *Definido pelo ambiente* |
| `POSTGRESSQL_USER` | Usuário de autenticação no banco de dados. | *Definido pelo ambiente* |
| `POSTGRESSQL_PASSWORD` | Senha de autenticação no banco de dados. | *Definido pelo ambiente* |
| `JWT_SECRET` | Chave simétrica HMAC256 para assinatura dos tokens. | `my-secret-key` |
| `JWT_ISSUER` | Identificador de emissão do token JWT. | `pagtesouro-api` |
| `JWT_AUDIENCE` | Destinatário esperado do token JWT. | `api` |
| `JWT_EXPIRATION_MINUTES` | Tempo limite de validade do token (em minutos). | `20` |
| `INITIAL_ADMIN_EMAIL` | E-mail a ser promovido automaticamente a ADMIN na inicialização. | *Vazio* |
| `PAGTESOURO_BASE_URL` | URL base da API do PagTesouro (Simulador ou Produção). | `https://valpagtesouro.tesouro.gov.br` |
| `PAGTESOURO_TOKEN_SALINAS` | Token de credenciamento oficial da Unidade Gestora de Salinas. | *Obrigatório* |
| `PAGTESOURO_URL_NOTIFICACAO` | URL pública do backend para o recebimento do Webhook da STN. | `https://meudominio.edu.br/api/payments/webhook` |
| `PAGTESOURO_DAEMON_CRON` | Cron do Daemon de conciliação de pagamentos. | `0 0 * * * *` *(a cada hora)* |
| `ORDER_ABANDONED_EXPIRATION_DAYS`| Prazo em dias para o cancelamento automático de pedidos abandonados. | `10` |

---

## 🧪 5. Execução dos Testes Automatizados

Para rodar a suíte completa de **94 testes unitários e de integração herméticos** (100% offline):

```bash
mvn clean test
```

Para compilar e gerar o pacote `.jar` da aplicação:

```bash
mvn clean package -DskipTests
```

---

## 🐳 6. Inicialização com Docker

A aplicação inclui suporte a contêineres para execução rápida em conjunto com o PostgreSQL:

1. Gere o pacote JAR da aplicação:

   ```bash
   mvn clean package -DskipTests
   ```

2. Suba o ecossistema completo:

   ```bash
   docker-compose up --build -d
   ```

3. Acesse a documentação interativa da API:

   ```text
   http://localhost:8080/swagger
   ```

---

## 📁 7. Coleção Bruno API Client

O repositório inclui uma coleção de requisições pronta para uso no **Bruno API Client** na pasta [`endpoints collection`](./endpoints%20collection/), estruturada nas seguintes categorias:

* `login/`: Registro (`Register.yml`), Login (`Login.yml`) e Perfil (`Obter Perfil do Usuário Logado (Me).yml`).
* `product/`: Listagem, Consulta por ID, Categorias, Criação, Atualização e Soft Delete.
* `order/`: Criação de Pedido, Consulta de Pedidos, Cancelamento de Pedido, Pedidos de Visitantes, Consulta de Tíquetes e Baixa de Tíquetes.
* `payment/`: Checkout de Pedido, Checkout Direto, Histórico Paginado e Conexão SSE.
* `WebhookSimulate.yml`: Simulação manual de callbacks da STN.
