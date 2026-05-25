# 🏛️ Modernização de Meios de Pagamento - IFNMG Campus Salinas (Backend)

Este repositório contém a API REST (*backend*) desenvolvida como parte prática do Trabalho de Conclusão de Curso (TCC) de **Caio da Silva Viana**, sob a orientação de **Danielle Miranda Rodrigues**, para o curso de **Bacharelado em Sistemas de Informação** no **IFNMG - Campus Salinas**.

O objetivo principal desta aplicação é automatizar e modernizar a comercialização dos tíquetes físicos de alimentação do refeitório institucional, substituindo a circulação de dinheiro em espécie e a burocracia na emissão manual de GRUs (Guias de Recolhimento da União) por um ecossistema digital integrado diretamente com a API do **PagTesouro** (Secretaria do Tesouro Nacional - STN).

---

## 🚀 Funcionalidades Planejadas e Escopo

A aplicação é dividida em dois grandes perfis de acesso (**Roles**):

### 👤 Perfil do Usuário (Estudantes, Servidores e Cidadãos)
- **Autenticação Segura**: Cadastro e Login via e-mail e CPF, utilizando criptografia de senha e tokens JWT.
- **Compra de Tíquetes**: Seleção da quantidade e tipo de tíquete para compra.
  - **Tíquete Estudante**: R$ 2,50 (Amarelo)
  - **Tíquete Servidor/Professor**: R$ 15,00 (Azul)
  - **Tíquete Lanche**: R$ 0,75 (Verde Água)
- **Integração PagTesouro**: Geração da solicitação de pagamento e redirecionamento seguro via *iFrame* no frontend para escolha do método de pagamento.
  - **Pix** (Instantâneo/Interoperável)
  - **Cartão de Crédito**
  - **Saldo de Carteira Digital**
  - **Boleto GRU**
- **Histórico Pessoal**: Consulta ao histórico de pedidos efetuados e status das transações.

### ⚙️ Perfil do Administrador (Tesouraria e Setor Financeiro)
- **Controle de Tíquetes**: Cadastro, edição e desativação de tipos de tíquetes/serviços disponíveis.
- **Consulta de Relatórios**: Visualização detalhada das transações concluídas para conciliação bancária interna.
- **Autorização de Trocas/Estornos**: Validação e auditoria das transações solicitadas por usuários.

---

## 🛠️ Tecnologias e Arquitetura

O backend foi construído seguindo uma **Arquitetura em Três Camadas (Apresentação, Aplicação e Dados)** com as seguintes tecnologias:

- **Java 25**: Utilização da versão mais recente do JDK para máxima performance e aproveitamento das novas features de linguagem.
- **Spring Boot 4.0.6**: Framework de desenvolvimento ágil para criação de microsserviços e APIs RESTful estruturadas.
- **Spring Data JPA**: Abstração de banco de dados para mapeamento objeto-relacional (ORM).
- **PostgreSQL**: SGBD relacional de alta confiabilidade para persistência de dados.
- **Flyway Database Migrations**: Controle de versão do esquema do banco de dados (tabelas e sementes).
- **Spring Security & Auth0 Java-JWT**: Controle de segurança de rotas de forma *stateless* (sem estado de sessão), protegendo os endpoints sensíveis.
- **Lombok**: Redução de código boilerplate (geração automática de getters, setters, construtores).

---

## 💻 Modos e Meios de Desenvolvimento

Esta seção descreve os ambientes e como a aplicação se comporta durante a integração técnica com o governo.

### 🔌 Modos de Integração com o PagTesouro
1. **Modo Homologação / Simulação (Desenvolvimento)**:
   - A aplicação se conecta ao **Simulador do PagTesouro** através da URL de homologação (`https://valpagtesouro.tesouro.gov.br/simulador/`).
   - Permite testar todo o fluxo da API (solicitação de pagamento, redirecionamento de URL e processamento de webhooks fictícios de sucesso ou rejeição) sem envolver movimentação financeira real ou credenciamento de produção.
2. **Modo Produção**:
   - Requer credenciamento formal do IFNMG junto ao **SISGRU** para obtenção do token JWT oficial da Unidade Gestora (UG).
   - Aponta para os servidores de produção da STN, processando pagamentos oficiais integrados diretamente na Conta Única do Tesouro Nacional.

### 📐 Padrões de Projeto Utilizados
- **Padrão Controller-Service-Repository**: Separação clara de responsabilidades de exposição de rotas (Controllers), processamento de regras de negócios e integrações externas (Services) e acesso ao banco (Repositories).
- **Conversor de Precisão Monetária (`MoneyToCentsConverter`)**: Visando mitigar bugs financeiros clássicos decorrentes do uso de números decimais (`double` ou `float`) e para garantir conformidade com o banco de dados, os valores monetários são convertidos e armazenados como centavos (`Long`) no PostgreSQL, sendo convertidos para `BigDecimal` apenas na camada do domínio da aplicação.
- **Tratamento Global de Erros (`RestExceptionHandler`)**: Mapeamento centralizado de exceções HTTP (`@ControllerAdvice`), garantindo respostas JSON limpas e padronizadas para qualquer falha interna ou de negócios da API.

---

## ⚙️ Configuração do Ambiente e Execução

### 📋 Pré-requisitos
- **Java JDK 25** instalado.
- **Apache Maven 3.9+** instalado.
- **PostgreSQL 15+** rodando localmente.
- **Bruno** (Cliente HTTP para testes das coleções em `endpoints collection`).

### 🛠️ Variáveis de Ambiente Configuráveis
A aplicação lê as propriedades a partir do `application.properties`. As seguintes variáveis podem ser configuradas no sistema para customização:

| Variável | Descrição | Valor Padrão (Dev) |
| :--- | :--- | :--- |
| `ACTIVE_PROFILE` | Perfil ativo do Spring Boot | `dev` |
| `JWT_SECRET` | Chave secreta de assinatura dos tokens da API | `my-secret-key` |
| `JWT_ISSUER` | Emissor dos tokens de autenticação | `pagtesouro-api` |
| `JWT_AUDIENCE` | Audiência destinatária do token | `api` |
| `JWT_EXPIRATION_HOURS` | Tempo de expiração do token gerado (horas) | `2` |

### 🚀 Executando a Aplicação
1. **Clonar o Repositório**:
   ```bash
   git clone https://github.com/CaioSilvaCsv/tcc-server.git
   cd tcc-server
   ```
2. **Criar o Banco de Dados**:
   Crie uma base de dados no PostgreSQL chamada `pagtesouro`. O Flyway se encarregará de criar as tabelas automaticamente na primeira execução da aplicação.
   ```sql
   CREATE DATABASE pagtesouro;
   ```
3. **Iniciar a API**:
   Execute o comando Maven para inicializar o servidor em modo de desenvolvimento:
   ```bash
   mvn spring-boot:run
   ```
   *O servidor iniciará por padrão na porta `8080`.*

4. **Importar a Coleção de Testes (Bruno)**:
   - Abra a ferramenta **Bruno**.
   - Escolha "Open Collection" e aponte para a pasta `endpoints collection` na raiz do projeto.
   - Use as requisições prontas de `Register` e `Login` para testar os endpoints de autenticação.
