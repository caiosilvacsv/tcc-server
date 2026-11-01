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
* **Qualidade de Código**: Suite automatizada com **118 testes unitários e de integração** 100% herméticos e aprovados com 100% de sucesso (`mvn clean test`).

---

## 📋 2. O Que o Servidor Está Disposto a Fazer (Capacidades da API)

O `tcc-server` está completamente estruturado e pronto para atender os seguintes subsistemas da instituição:

### Matriz de Endpoints Disponíveis

| Método | Endpoint | Permissão | Descrição / Capacidade do Servidor |
| :--- | :--- | :--- | :--- |
| **`POST`** | `/auth/login` | Pública | Autentica via e-mail/senha, emite cookie seguro `HttpOnly` com JWT e retorna sessão mínima (`AuthSessionDTO`). |
| **`POST`** | `/auth/register` | Pública | Cadastra novo estudante/servidor, emite cookie seguro `HttpOnly` e retorna sessão mínima. |
| **`POST`** | `/auth/logout` | Autenticado | Encerra a sessão no navegador limpando o cookie seguro `HttpOnly`. |
| **`GET`** | `/auth/me` | Autenticado | Retorna os dados cadastrais completos (ID, Nome, E-mail, CPF, Perfil) do usuário logado. |
| **`PATCH`** | `/auth/me` | Autenticado | Atualiza dados cadastrais (nome, sobrenome, e-mail) - LGPD Art. 18. |
| **`PATCH`** | `/auth/me/password` | Autenticado | Realiza a troca voluntária de senha com validação prévia da senha atual. |
| **`POST`** | `/auth/forgot-password` | Pública | Dispara e-mail de recuperação de senha com proteção anti-enumeração de usuários. |
| **`POST`** | `/auth/reset-password` | Pública | Redefine a senha através de token criptográfico temporal de uso único. |
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
* **Migrações de Banco**: Flyway Migration (scripts versionados V1 a V9)
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
| `SERVER_FORWARD_HEADERS_STRATEGY`| Estratégia para cabeçalhos de proxy reverso (`framework` para Nginx/OCI). | `framework` |
| `SPRING_MAIL_HOST` | Host do servidor SMTP para disparo de e-mails. | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | Porta de conexão SMTP TLS. | `587` |
| `SPRING_MAIL_USERNAME` | Usuário/E-mail de autenticação no servidor SMTP. | *Vazio* |
| `SPRING_MAIL_PASSWORD` | Senha de aplicativo para autenticação SMTP. | *Vazio* |
| `MAIL_FROM` | Endereço de remetente exibido nas mensagens enviadas. | `nao-responda@ifnmg.edu.br` |
| `FRONTEND_RESET_PASSWORD_URL` | URL base do frontend para onde o link de redefinição aponta. | `http://localhost:3000/reset-password` |

---

## 🧪 5. Execução dos Testes Automatizados

Para rodar a suíte completa de **118 testes unitários e de integração herméticos** (100% offline):

```bash
mvn clean test
```

Para compilar e gerar o pacote `.jar` da aplicação:

```bash
mvn clean package -DskipTests
```

---

## 🐳 6. Inicialização Completa com Docker (Full-Stack Containerizado)

A aplicação conta com uma orquestração profissional via **Docker Compose** que sobe os **3 pilares da arquitetura** simultaneamente:
1. **`db` (PostgreSQL 16 Alpine)**: Banco de dados relacional com volume persistente e healthcheck.
2. **`app` (Spring Boot 4 / Java 25)**: API REST multiestágio otimizada para containers.
3. **`nginx` (Nginx Alpine)**: Servidor de borda, terminação SSL e hospedagem da SPA React (`nginx/html`), roteando as chamadas para o backend internamente na mesma rede Docker.

### Como Executar em 1 Comando:

```bash
docker compose up --build -d
```

### URLs de Acesso:
* **Aplicação Completa (Frontend + API via Nginx)**: `http://localhost` (ou seu domínio na OCI)
* **Documentação Interativa Swagger UI**: `http://localhost:8080/swagger`
* **Auditoria do Banco PostgreSQL**: `localhost:5432`

### Atualizando o Frontend React:
Para atualizar os arquivos visuais da interface:
1. Na pasta `web`, gere os arquivos estáticos:
   ```bash
   npm run build
   ```
2. Copie o conteúdo de `web/dist` para `server/nginx/html` e o Nginx atualizará a aplicação instantaneamente sem necessidade de reiniciar os containers.

---

## 📁 7. Coleção Bruno API Client

O repositório inclui uma coleção de requisições pronta para uso no **Bruno API Client** na pasta [`endpoints collection`](./endpoints%20collection/), estruturada nas seguintes categorias:

* `login/`: Registro (`Register.yml`), Login (`Login.yml`) e Perfil (`Obter Perfil do Usuário Logado (Me).yml`).
* `product/`: Listagem, Consulta por ID, Categorias, Criação, Atualização e Soft Delete.
* `order/`: Criação de Pedido, Consulta de Pedidos, Cancelamento de Pedido, Pedidos de Visitantes, Consulta de Tíquetes e Baixa de Tíquetes.
* `payment/`: Checkout de Pedido, Checkout Direto, Histórico Paginado e Conexão SSE.
* `WebhookSimulate.yml`: Simulação manual de callbacks da STN.


---

## 🌐 8. Arquitetura de Produção & Proxy Reverso (Nginx / OCI)

Na implantação em produção na **Oracle Cloud Infrastructure (OCI)** (Compute Instance Ubuntu Linux), a aplicação Spring Boot **não é exposta diretamente à Internet**. Em vez disso, adota-se um **Proxy Reverso de alta performance com Nginx** atuando como servidor de borda e terminação TLS.

### A. Motivação e Benefícios da Camada de Borda
1. **Segurança de Portas Privilegiadas**: Portas `80` (HTTP) e `443` (HTTPS) exigem privilégios de superusuário (root) no Linux. O Spring Boot executa isolado como serviço de sistema desprivilegiado ouvindo estritamente na porta interna `127.0.0.1:8080`.
2. **Terminação SSL/TLS de Alta Performance**: A criptografia HTTPS é terminada em nível de sistema operacional pelo Nginx (C/OpenSSL), desonerando o Garbage Collector e as threads da JVM.
3. **Mitigação de Ataques DoS / Slowloris**: O Nginx absorve conexões lentas ou incompletas, impondo limites estritos de payload (`client_max_body_size`), timeouts e controle de taxa antes de repassar a carga ao backend.
4. **Hospedagem Unificada (Frontend SPA + Backend API)**: O Nginx serve os arquivos estáticos compilados do React SPA (`dist/`) e encaminha as rotas de API para o Spring Boot sob o **mesmo domínio**, eliminando latência de preflight e complexidade de CORS.
5. **Streaming em Tempo Real (Server-Sent Events - SSE)**: O endpoint `/payments/{paymentId}/sse` requer desativação de buffer (`proxy_buffering off;`), garantindo que os eventos de compensação bancária do Pix/PagTesouro cheguem instantaneamente ao navegador sem enfileiramento intermediário.

### B. Topologia de Rede na OCI
* **VCN (Virtual Cloud Network)**: Sub-rede pública com Internet Gateway.
* **Security List da OCI (Ingress Rules)**:
  * Porta `22` (SSH): Restrita aos endereços IP administrativos.
  * Porta `80` (HTTP): Liberada para `0.0.0.0/0` (redirecionamento obrigatório para HTTPS).
  * Porta `443` (HTTPS): Liberada para `0.0.0.0/0` (tráfego criptografado).
  * Porta `8080` (Spring Boot): **Bloqueada externamente** (acessível exclusivamente pelo Nginx via `127.0.0.1`).

### C. Arquivo de Configuração do Nginx (`/etc/nginx/sites-available/tcc-ifnmg`)

```nginx
# 1. Redirecionamento forçado de HTTP para HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name pagamentos.salinas.ifnmg.edu.br; # Substitua pelo seu domínio institucional ou IP público

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# 2. Servidor Principal HTTPS Seguro
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name pagamentos.salinas.ifnmg.edu.br;

    # Certificados SSL gerenciados pelo Let's Encrypt / Certbot
    ssl_certificate /etc/letsencrypt/live/pagamentos.salinas.ifnmg.edu.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pagamentos.salinas.ifnmg.edu.br/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    # Cabeçalhos de Proteção HTTP
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Limite de tamanho de requisição (proteção DoS)
    client_max_body_size 10M;

    # Servindo o Frontend SPA (React / Vite)
    root /var/www/tcc-client/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy Reverso para a API REST Spring Boot
    location ~ ^/(auth|product|orders|payments|api|swagger|v3/api-docs|actuator) {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # Repasse dos cabeçalhos de rede reais
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;

        # Suporte a conexões persistentes
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Rota Específica para Server-Sent Events (SSE) - Notificação Pix PagTesouro
    location ~ ^/payments/[^/]+/sse$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # CRÍTICO: Desativa buffers para entrega reativa imediata dos eventos SSE
        proxy_buffering off;
        proxy_cache off;
        chunked_transfer_encoding on;

        # Timeout estendido para sustentar conexão aberta de longa duração
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }
}
```

### D. Passo a Passo de Implantação e Certificado SSL na OCI

1. **Instalar Nginx e Certbot no Ubuntu 24.04 LTS**:
   ```bash
   sudo apt update && sudo apt install -y nginx certbot python3-certbot-nginx
   ```

2. **Ativar o Site e Validar Sintaxe**:
   ```bash
   sudo ln -s /etc/nginx/sites-available/tcc-ifnmg /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl reload nginx
   ```

3. **Gerar Certificado SSL Gratuito com Renovação Automática**:
   ```bash
   sudo certbot --nginx -d pagamentos.salinas.ifnmg.edu.br
   ```

4. **Reconhecimento de Cabeçalhos no Spring Boot**:
   A configuração em `application.properties`:
   ```properties
   server.forward-headers-strategy=framework
   ```
   faz com que o Spring Boot interprete o `X-Forwarded-Proto` enviado pelo Nginx. Isso garante que o método `request.isSecure()` retorne `true`, fazendo com que os Cookies de autenticação `accessToken` sejam transmitidos com o atributo `Secure` pelo navegador e que o IP real do cliente seja registrado para auditoria.
