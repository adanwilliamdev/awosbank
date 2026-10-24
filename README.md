# 🏦 AWOS Bank — Internet Banking

Sistema de **Internet Banking desenvolvido em Java e Spring Boot**, com interface web moderna, autenticação segura e funcionalidades para gerenciamento de conta bancária, transações e chaves PIX.

O projeto simula a experiência de um banco digital, combinando processamento no backend com uma interface responsiva desenvolvida com Thymeleaf e Bootstrap.

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-005F0F?style=for-the-badge&logo=thymeleaf)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap)
![JavaScript](https://img.shields.io/badge/JavaScript-Vanilla-F7DF1E?style=for-the-badge&logo=javascript)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

---

## ✨ Funcionalidades

### 🔐 Autenticação e segurança
- Cadastro e login de clientes.
- Criptografia de senhas com BCrypt.
- Proteção de rotas com Spring Security.
- Controle de acesso às funcionalidades da aplicação.

### 📊 Dashboard bancário
- Visualização do saldo da conta.
- Opção para ocultar e exibir o saldo.
- Gráfico de entradas e saídas com Chart.js.
- Ações dinâmicas utilizando AJAX e Fetch API.

### 💳 Extrato bancário
- Consulta ao histórico de transações.
- Filtros por período, com data inicial e final.
- Filtragem por tipo de transação.
- Visualização organizada das movimentações financeiras.

### 🔑 Gerenciamento de chaves PIX
- Cadastro de chaves PIX.
- Consulta, edição e exclusão de chaves.
- Interface para gerenciamento das chaves vinculadas ao cliente.

### 👤 Perfil do cliente
- Visualização e atualização de dados cadastrais.
- Máscaras dinâmicas para campos de formulário.
- Interface intuitiva para edição de informações.

### 🕵️ Histórico de auditoria
- Registro de **cadastro, login (sucesso e falha), transferências, depósitos, saques, alterações de perfil e chaves PIX**.
- Tentativas que **falham** (saldo insuficiente, senha incorreta…) também ficam registradas — gravadas em transação própria, então sobrevivem ao rollback da operação.
- Cada registro guarda usuário, ação, resultado, detalhes e IP de origem. **Nunca** senhas.
- Tabela *append-only*: um trigger do PostgreSQL bloqueia `UPDATE` e `DELETE`.
- Consulta pelo próprio cliente em `GET /api/auditoria`.

### 🧾 Comprovantes e tratamento de erros
- Geração de comprovantes de transações.
- Exibição de recibos em modal.
- Suporte à impressão de comprovantes.
- Páginas personalizadas para erros HTTP 404 e 500.

---

## 🛠️ Tecnologias utilizadas

| Categoria | Tecnologias |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Aplicação web | Spring Web |
| Segurança | Spring Security, BCrypt |
| Persistência | Spring Data JPA, Hibernate |
| Banco de dados | PostgreSQL 16 + Flyway (migrations versionadas) |
| API docs | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, Spring Boot Test, Testcontainers, JaCoCo |
| Infra / CI | Docker, Docker Compose, GitHub Actions |
| Validação | Spring Boot Validation |
| Templates | Thymeleaf |
| Estilização | Bootstrap 5.3, CSS |
| Interatividade | JavaScript, Fetch API, AJAX |
| Gráficos | Chart.js |
| Produtividade | Lombok |
| Build | Maven |

---

## 🏗️ Arquitetura

A aplicação utiliza uma arquitetura baseada no ecossistema Spring Boot, separando as responsabilidades entre a camada web, a lógica de negócio e a persistência de dados.

- **Controllers:** recebem e processam as requisições HTTP.
- **Services:** concentram as regras de negócio.
- **Repositories:** realizam as operações de persistência com Spring Data JPA.
- **Entities:** representam as entidades persistidas no banco de dados.
- **Templates:** renderizam as páginas utilizando Thymeleaf.
- **Spring Security:** gerencia a autenticação e a autorização.

---

## 🚀 Como executar localmente

### Opção 1 — Docker Compose (mais simples)

Pré-requisito: Docker com Compose v2.

```bash
git clone https://github.com/adanwilliamdev/awosbank.git
cd awosbank
docker compose up -d --build
```

- Aplicação: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

Para trocar senha do banco, portas ou perfil, copie `.env.example` para `.env` e ajuste. Para parar e apagar os dados: `docker compose down -v`.

### Opção 2 — Maven + só o banco no Docker

Pré-requisitos: Java 17+, Maven e Docker.

```bash
docker compose up -d db      # PostgreSQL em localhost:5432
mvn spring-boot:run          # perfil "dev": cria o schema via Flyway e semeia o usuário de demonstração
```

O schema é criado e versionado pelo **Flyway** (`src/main/resources/db/migration`). O Hibernate roda com `ddl-auto=validate`: se uma entidade divergir das migrations, a aplicação não sobe. Para mudar o banco, crie uma nova migration (`V3__descricao.sql`) — nunca edite uma já aplicada.

### Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` (demo + Swagger) ou `prod` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/awosbank` | URL JDBC |
| `DB_USERNAME` / `DB_PASSWORD` | `awosbank` / `awosbank` | **Obrigatórias, sem padrão, no perfil `prod`** |
| `FORWARD_HEADERS_STRATEGY` | `none` | Use `framework` atrás de proxy reverso (IP real na auditoria) |

> **Produção:** use `SPRING_PROFILES_ACTIVE=prod` atrás de HTTPS (o cookie de sessão é `Secure`). O perfil `prod` desliga o Swagger e não cria usuário de demonstração.
>
> **Já tinha dados do H2/`ddl-auto=update`?** O H2 era em memória, então não há o que migrar. Se você usava um PostgreSQL criado pelo Hibernate, rode o Flyway com baseline na V1 (`spring.flyway.baseline-version=1` + `baseline-on-migrate=true`) para ele aplicar apenas a V2.

---

## 📖 Documentação da API (Swagger / OpenAPI)

Com a aplicação rodando (perfil `dev`):

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>

A API usa a sessão do login web: entre em `/login` no mesmo navegador e use *Try it out*. Os endpoints estão agrupados em **Transações**, **Chaves PIX** e **Auditoria**.

## 🧪 Testes

```bash
mvn test       # unitários + integração (H2 em memória) — rápido
mvn verify     # + relatório de cobertura (target/site/jacoco/index.html)
```

| Tipo | Classes |
|---|---|
| Unitários (JUnit 5 + Mockito) | `TransacaoServiceTest`, `UsuarioServiceTest`, `ContaServiceTest`, `PixKeyServiceTest`, `AuditoriaServiceTest`, `AuditoriaAspectTest`, `DocumentoValidatorTest` |
| Repositórios (`@DataJpaTest`) | `RepositoriosJpaTest` |
| Integração (contexto Spring completo) | `ApiIntegrationTest`, `AuditoriaIntegrationTest`, `SegurancaWebTest`, `TransferenciaConcorrenciaTest` |
| Integração com **PostgreSQL real** (Testcontainers) | `PostgresMigrationTest` — valida as migrations contra as entidades, locks, constraints e o trigger da auditoria. Pulado automaticamente se não houver Docker. |

## 🔄 CI/CD

O workflow `.github/workflows/ci.yml` roda a cada push/PR:

1. **Build e testes** — `mvn verify` (Java 17), com relatórios de teste e cobertura como artefatos.
2. **Smoke test** — sobe o `docker compose` e confere `/actuator/health`, `/login` e o OpenAPI.
3. **Publicação (CD)** — em push na `main` ou tag `v*`, publica a imagem no GitHub Container Registry (`ghcr.io/<usuario>/awosbank`) com tags `latest`, `sha` e versão.

Para implantar a imagem publicada num servidor: defina `APP_IMAGE=ghcr.io/<usuario>/awosbank:latest` no `.env` e rode `docker compose pull app && docker compose up -d`.

O Dependabot (`.github/dependabot.yml`) abre PRs semanais para Maven, GitHub Actions e Docker.

---

## 🔑 Credenciais de teste

Caso os dados de demonstração estejam configurados na aplicação:

| Campo | Credencial |
|---|---|
| E-mail | `cliente@awosbank.com` |
| Senha | `123456` |

> **Importante:** as credenciais só funcionarão se o usuário de demonstração for criado ou carregado pela aplicação. Utilize dados fictícios exclusivamente para testes.

---

## 🎯 Objetivos do projeto

O AWOS Bank foi desenvolvido para praticar e demonstrar conhecimentos em:

- Desenvolvimento backend com Java e Spring Boot.
- Criação de aplicações web com arquitetura MVC.
- Autenticação e autorização com Spring Security.
- Persistência de dados com JPA e Hibernate.
- Desenvolvimento de interfaces com Thymeleaf e Bootstrap.
- Integração entre frontend e backend.
- Manipulação assíncrona de requisições HTTP.
- Validação de dados e tratamento de exceções.
- Organização e manutenção de aplicações Java.

---

## ✅ Melhorias implementadas

- [x] Migração do H2 para PostgreSQL (com migrations Flyway).
- [x] Testes automatizados com JUnit e Mockito.
- [x] Implementação de testes de integração (inclusive com PostgreSQL via Testcontainers).
- [x] Dockerização da aplicação (Dockerfile multi-stage + Docker Compose).
- [x] Documentação da API com Swagger/OpenAPI.
- [x] Histórico de auditoria das operações.
- [x] Pipeline de integração e entrega contínuas (CI/CD).

## 🔮 Próximos passos

- [ ] Autenticação por token (JWT) para a API, além da sessão web.
- [ ] Rate limiting / bloqueio temporário após tentativas de login com falha (os dados já estão na auditoria).
- [ ] Tela web para o histórico de auditoria.

---

## ⚠️ Aviso

Este projeto tem finalidade **educacional e demonstrativa**. Não utilize dados bancários reais nem o disponibilize como serviço financeiro de produção sem implementar e auditar os controles de segurança, as regras de negócio e os requisitos regulatórios necessários.

---

## 👨‍💻 Autor

**Adan William Oliveira Santos**

Desenvolvedor com foco em Java, Spring Boot e desenvolvimento backend.

[![GitHub](https://img.shields.io/badge/GitHub-adanwilliamdev-181717?style=for-the-badge&logo=github)](https://github.com/adanwilliamdev)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Perfil-0A66C2?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/adan-william-9593163b7/)

---

⭐ Se este projeto foi útil ou interessante, considere deixar uma estrela no repositório!
