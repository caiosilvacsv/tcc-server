# 🏛️ Modernização de Meios de Pagamento - IFNMG Campus Salinas (Backend)

Este repositório contém a API REST (*backend*) de alta robustez desenvolvida como a contribuição prática do Trabalho de Conclusão de Curso (TCC) de **Caio da Silva Viana**, sob a orientação de **Danielle Miranda Rodrigues**, para o curso de **Bacharelado em Sistemas de Informação** no **IFNMG - Campus Salinas**.

O objetivo principal desta aplicação é automatizar e modernizar a arrecadação de receitas públicas do IFNMG (como os tíquetes de alimentação do refeitório e multas de biblioteca), integrando a plataforma diretamente com a API do **PagTesouro** (Secretaria do Tesouro Nacional - STN).

---

## 🗺️ 1. Visão Geral do Ecossistema de Negócios

A aplicação atende de forma unificada a dois perfis distintos de arrecadação financeira institucional:

### A. Fluxo de Checkout de Pedido (Estudantes & Servidores Autenticados)

Modela a aquisição convencional estruturada através do conceito de **Carrinho de Compras**:

1. O estudante cria uma conta acadêmica e se autentica via e-mail/CPF (Tokens JWT).
2. Ele monta um carrinho adicionando tíquetes ou selecionando suas taxas de biblioteca pendentes, gerando um Pedido (`Order`) com múltiplos itens (`OrderItem`) em estado `PENDING`.
3. Ao acionar o checkout do pedido, o backend instancia uma transação de `Payment` vinculando-a aos itens solicitados (tabela de ligação `payment_items` via `@ManyToMany`).
4. O aluno é redirecionado via *iFrame* do PagTesouro da STN para efetuar a quitação (Pix, Cartão ou GRU).
5. No retorno assíncrono do governo (via Webhook ou Daemon), atualizamos o `Payment` para `COMPLETED` e os itens de pedido correspondentes para `PAID`, registrando a data e hora de compensação (`paidAt`).

### B. Fluxo de Checkout Direto (Doadores e Cidadãos Anônimos)

Permite a arrecadação simplificada de contribuições espontâneas ou o pagamento rápido de taxas avulsas sem a necessidade de criação de conta:

* O usuário seleciona o produto na tela pública e fornece seu CPF e Nome.
* A API realiza a persistência **direta na entidade `Payment`**, registrando as informações cadastrais do contribuinte e gerando um número sequencial exclusivo de referência (`reference_number`) para busca.

---

## 🕰️ 2. Daemon de Conciliação Failsafe (Resiliência)

Para mitigar cenários de instabilidade na rede da STN ou quedas operacionais do webhook de pagamentos, o backend incorpora um agendador automático em segundo plano (**Daemon**):

* **Componente**: `PaymentDaemonService.java`
* **Lógica de Operação**: De hora em hora, varre o banco local localizando pagamentos que estejam nos status `CREATED`, `STARTED` ou `SUBMITTED` gerados há mais de 10 minutos (dando tempo de interação para o usuário) e há menos de 48 horas (validade útil de guias).
* **Conciliação Ativa**: O Daemon dispara chamadas reativas não-bloqueantes de consulta à API do PagTesouro. Se a STN confirmar a quitação, o backend sincroniza o status do banco local e liquida os itens automaticamente.
* **Operador de Fallback**: O agendador utiliza a expressão cron do Spring Boot mapeando uma propriedade customizável:

  ```java
  @Scheduled(cron = "${api.pagtesouro.daemon.cron:0 0 * * * *}")
  ```

  Se a chave `api.pagtesouro.daemon.cron` não for declarada no `application.properties`, o sistema adota automaticamente o fallback padrão para rodar de 1 em 1 hora (`0 0 * * * *`) de forma resiliente.

---

## 📦 3. Logística de Produtos (`Product`) e Soft-Delete

A API oferece o ciclo completo de gerenciamento de itens e serviços do catálogo de débitos institucional em `/product`:

* **Acesso Controlado**: A listagem de produtos ativos é pública (`GET /product`). Contudo, o cadastro (`POST`), edição (`PUT`) e remoção (`DELETE`) são rotas estritamente restritas a administradores (`ROLE_ADMIN`).
* **Soft-Delete**: Alinhado às normas de auditoria e conformidade financeira governamental (Siafi), a remoção de um produto não deleta fisicamente o seu registro do banco. A API altera a flag `active` para `false`, impedindo novas vendas e compras no frontend, mas mantendo a integridade histórica dos dados fiscais do banco de dados.

---

## 🛠️ Tecnologias e Arquitetura

O projeto adota os padrões mais modernos de engenharia e desenvolvimento de software:

* **Java 25 & Spring Boot 4.0.6**: Backend corporativo de alta performance com a última especificação da JDK.
* **Spring WebFlux (Reatividade HTTP)**: Utilização do `WebClient` não-bloqueante reativo para a integração cliente-servidor assíncrona com os endpoints do PagTesouro.
* **Segurança Stateless (JWT + HMAC256)**: Autenticação via Spring Security baseada em tokens JWT assinados digitalmente com chave simétrica HMAC256 para controle restrito de acesso.
* **Conversor de Precisão Monetária (`MoneyToCentsConverter`)**: Proteção física de arredondamentos na persistência. Os valores monetários decimais em Java (`BigDecimal`) são mapeados e persistidos no PostgreSQL como **centavos** em colunas `INTEGER` de 32 bits, reduzindo consumo de armazenamento e agilizando a indexação.
* **Tratamento Global de Erros (`RestExceptionHandler`)**: Interceptador `@ControllerAdvice` que captura e transforma exceções de negócio em mensagens REST padronizadas no DTO `RestErrorMessage`, ocultando stacktraces de banco em produção.
* **Validador de CPF (`CpfValidator`)**: Algoritmo matemático para validação de dígitos verificadores e normalização de strings antes da persistência no banco.

---

## ⚙️ 4. Configuração Detalhada de Variáveis de Ambiente

A aplicação foi estruturada seguindo os princípios de **Twelve-Factor App**, permitindo que toda a configuração seja externalizada e injetada por meio de variáveis de ambiente. Isso garante o desacoplamento de segredos entre ambientes de homologação e produção sem nenhuma alteração no código.

Abaixo, detalhamos cada uma das variáveis de ambiente lidas pelo arquivo `application.properties`:

### A. Variáveis de Conexão com o Banco de Dados (PostgreSQL)

| Variável de Ambiente | Descrição Técnica e Acadêmica | Obrigatoriedade | Valor Padrão (Fallback) |
| :--- | :--- | :--- | :--- |
| `POSTGRESSQL_URL` | URL de conexão JDBC para o banco PostgreSQL. Deve incluir a porta e o nome da base de dados. Ex: `jdbc:postgresql://db:5432/pagtesouro` | **Obrigatória** | *Nenhum* |
| `POSTGRESSQL_USER` | Nome de usuário físico do banco de dados PostgreSQL. | **Obrigatória** | *Nenhum* |
| `POSTGRESSQL_PASSWORD` | Senha do usuário do banco de dados PostgreSQL. | **Obrigatória** | *Nenhum* |

### B. Variáveis de Segurança e Autenticação (Spring Security / JWT)

| Variável de Ambiente | Descrição Técnica e Acadêmica | Obrigatoriedade | Valor Padrão (Fallback) |
| :--- | :--- | :--- | :--- |
| `ACTIVE_PROFILE` | Define o perfil de execução ativo no Spring. Utilizado para carregar configurações de beans e dialetos específicos. Opções: `dev`, `prod` | Opcional | `dev` |
| `JWT_SECRET` | Chave secreta alfanumérica robusta utilizada para assinar os tokens JWT na camada de segurança com assinatura simétrica **HMAC256**. | Opcional | `my-secret-key` |
| `JWT_ISSUER` | String identificadora que define quem gerou o token (propriedade *issuer*). Utilizado para atestar a legitimidade de origem do JWT. | Opcional | `pagtesouro-api` |
| `JWT_AUDIENCE` | String identificadora que define a aplicação destinatária do token (propriedade *audience*). | Opcional | `api` |
| `JWT_EXPIRATION_HOURS` | Prazo limite de validade útil de cada token emitido para o estudante, parametrizado diretamente na injeção do bean em minutos. | Opcional | `2` *(mapeado em minutos no record)* |

### C. Variáveis de Integração Governamental (API PagTesouro STN)

| Variável de Ambiente | Descrição Técnica e Acadêmica | Obrigatoriedade | Valor Padrão (Fallback) |
| :--- | :--- | :--- | :--- |
| `PAGTESOURO_BASE_URL` | URL base de comunicação HTTP para a API REST da STN. Utiliza por padrão a URL do simulador/homologação de Salinas. | Opcional | `"https://valpagtesouro.tesouro.gov.br"` |
| `PAGTESOURO_TOKEN_SALINAS` | Token JWT de credenciamento oficial da Unidade Gestora (UG) exigido no cabeçalho `Authorization: Bearer` pela STN. | **Obrigatória** | *Nenhum* |
| `PAGTESOURO_URL_NOTIFICACAO` | URL pública exposta pelo backend do IFNMG para receber as notificações ativas (Webhooks) assíncronas do PagTesouro da STN. | Opcional | `"https://meudominio.edu.br/api/payments/webhook"` |
| `PAGTESOURO_DAEMON_CRON` | Expressão cron que regula o intervalo de execução em segundo plano do Daemon Failsafe de conciliação ativa. Padrão: Executa a cada 1 hora. | Opcional | `0 0 * * * *` *(fallback a cada hora)* |

---

## 🧪 5. Suíte de Testes Automatizados (JUnit 5 + Mockito 5)

O backend possui uma suite robusta de **52 testes automatizados e 100% ativos**, operando sob as diretrizes estritas do Mockito 5.x:

* **Cobertura Abrangente**: Valida de ponta a ponta entidades, conversores, enums, controladores de produtos (simulando autenticação administrativa e de usuários comuns via injeção segura de mocks no `SecurityContextHolder`) e os serviços transacionais de pedidos e pagamentos.
* **Comando para Rodar os Testes**:

  ```powershell
  mvn clean test
  ```

  *Garante o status **BUILD SUCCESS** e integridade funcional impecável a cada ciclo de compilação.*

---

## 🐳 6. Orquestração e DevOps com Docker

Fornecemos a infraestrutura de deploy em contêineres para facilitar a implantação local do ecossistema com um único comando:

* **Dockerfile**: Baseado no contêiner enxuto e seguro `eclipse-temurin:25-jre-alpine`, proporcionando excelente tempo de inicialização e baixo consumo de RAM.
* **docker-compose.yml**: Orquestra os seguintes serviços em uma rede interna isolada:
  * `db`: Banco relacional `postgres:16-alpine` mapeado com volumes persistentes em disco e `healthcheck` ativo.
  * `app`: O backend do Spring Boot que inicializa somente após o banco de dados estar totalmente operacional (`service_healthy`).

### Como Rodar Localmente via Docker

1. Gere o empacotamento do arquivo jar:

   ```bash
   mvn clean package -DskipTests
   ```

2. Inicialize os contêineres:

   ```bash
   docker-compose up --build -d
   ```

---

## 🎛️ 7. Coleção de Testes Manuais (Bruno Client)

Na pasta [endpoints collection](./endpoints%20collection) do repositório, está exposta a coleção de chamadas do **Bruno API Client** com todas as requisições configuradas para testes manuais ágeis:

* Registro e Autenticação de Alunos/ADMINs.
* Listagem e Manutenção do Catálogo de Produtos.
* Criação de Pedidos e Checkout (Direto e Integrado).
* Simulação de notificações do webhook governamental da STN.
