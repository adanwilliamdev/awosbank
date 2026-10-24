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
- Gerenciamento das chaves vinculadas ao cliente.

### 👤 Perfil do cliente
- Visualização e atualização de dados cadastrais.
- Máscaras dinâmicas para campos de formulário.
- Interface para edição de informações.

### 🕵️ Histórico de auditoria
- Registro de cadastros, logins, transferências, depósitos, saques, alterações de perfil e chaves PIX.
- Registro de operações com falha, incluindo saldo insuficiente e senha incorreta.
- Persistência dos registros de falha em transações independentes.
- Armazenamento de usuário, ação, resultado, detalhes e IP de origem, sem registrar senhas.
- Tabela append-only protegida por trigger do PostgreSQL contra operações de `UPDATE` e `DELETE`.
- Consulta pelo próprio cliente em `GET /api/auditoria`.

### 🧾 Comprovantes e tratamento de erros
- Geração de comprovantes de transações.
- Exibição de recibos em modal.
- Impressão de comprovantes.
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
| Banco de dados | PostgreSQL 16, Flyway |
| API docs | springdoc-openapi, Swagger UI |
| Testes | JUnit 5, Mockito, Spring Boot Test, Testcontainers, JaCoCo |
| Infraestrutura e CI/CD | Docker, Docker Compose, GitHub Actions |
| Validação | Spring Boot Validation |
| Templates | Thymeleaf |
| Estilização | Bootstrap 5.3, CSS |
| Interatividade | JavaScript, Fetch API, AJAX |
| Gráficos | Chart.js |
| Produtividade | Lombok |
| Build | Maven |

---

## 🏗️ Arquitetura

A aplicação utiliza uma arquitetura baseada no ecossistema Spring Boot, com separação de responsabilidades entre as camadas web, de negócio e de persistência.

- **Controllers:** recebem e processam requisições HTTP.
- **Services:** concentram as regras de negócio.
- **Repositories:** realizam operações de persistência com Spring Data JPA.
- **Entities:** representam os dados persistidos no banco.
- **Templates:** renderizam páginas com Thymeleaf.
- **Spring Security:** gerencia autenticação e autorização.

---

## 🚀 Como executar localmente

### Opção 1: Docker Compose

**Pré-requisito:** Docker com Compose v2.

```bash
git clone https://github.com/adanwilliamdev/awosbank.git
cd awosbank
docker compose up -d --build
```

- **Aplicação:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health

Configure as variáveis de ambiente no arquivo `.env`, utilizando `.env.example` como referência.

Para interromper os serviços:

```bash
docker compose down
```

Para interromper os serviços e remover os volumes:

```bash
docker compose down -v
```

### Opção 2: Maven e PostgreSQL no Docker

**Pré-requisitos:** Java 17+, Maven e Docker.

```bash
git clone https://github.com/adanwilliamdev/awosbank.git
cd awosbank
docker compose up -d db
mvn spring-boot:run
```

O perfil `dev` executa as migrations do Flyway e configura o usuário de demonstração.

As migrations ficam em `src/main/resources/db/migration`. O Hibernate utiliza `ddl-auto=validate` para validar as entidades em relação ao schema.

Para alterar o banco, crie uma nova migration, como `V3__descricao.sql`.

### Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil `dev` ou `prod` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/awosbank` | URL JDBC |
| `DB_USERNAME` | `awosbank` | Usuário do banco |
| `DB_PASSWORD` | `awosbank` | Senha do banco |
| `FORWARD_HEADERS_STRATEGY` | `none` | Estratégia de encaminhamento de cabeçalhos |

No perfil `prod`, as credenciais do banco devem ser fornecidas por variáveis de ambiente. O Swagger fica desabilitado e o usuário de demonstração não é criado.

Para implantação em produção, configure HTTPS e utilize `FORWARD_HEADERS_STRATEGY=framework` quando houver um proxy reverso.

---

## 📖 Documentação da API

Com a aplicação executando no perfil `dev`:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

A API utiliza a sessão de autenticação web. Após entrar em `/login`, acesse o Swagger no mesmo navegador para utilizar a opção *Try it out*.

Os endpoints estão agrupados em Transações, Chaves PIX e Auditoria.

---

## 🧪 Testes

```bash
mvn test
mvn verify
```

O comando `mvn test` executa os testes automatizados. O comando `mvn verify` também gera o relatório de cobertura do JaCoCo em `target/site/jacoco/index.html`.

| Tipo | Classes |
|---|---|
| Unitários (JUnit 5 + Mockito) | `TransacaoServiceTest`, `UsuarioServiceTest`, `ContaServiceTest`, `PixKeyServiceTest`, `AuditoriaServiceTest`, `AuditoriaAspectTest`, `DocumentoValidatorTest` |
| Repositórios (`@DataJpaTest`) | `RepositoriosJpaTest` |
| Integração com Spring | `ApiIntegrationTest`, `AuditoriaIntegrationTest`, `SegurancaWebTest`, `TransferenciaConcorrenciaTest` |
| PostgreSQL com Testcontainers | `PostgresMigrationTest` |

O teste `PostgresMigrationTest` valida migrations, entidades, locks, constraints e o trigger de auditoria utilizando PostgreSQL real. Ele é ignorado automaticamente quando o Docker não está disponível.

---

## 🔄 CI/CD

O workflow `.github/workflows/ci.yml` executa a cada push ou pull request.

1. **Build e testes:** `mvn verify` com Java 17 e geração de relatórios.
2. **Smoke test:** inicialização do Docker Compose e validação dos endpoints `/actuator/health`, `/login` e OpenAPI.
3. **Publicação:** envio da imagem ao GitHub Container Registry em pushes para `main` ou criação de tags `v*`.

A imagem publicada utiliza tags `latest`, `sha` e versão.

Para implantação:

```bash
docker compose pull app
docker compose up -d
```

Configure `APP_IMAGE=ghcr.io/<usuario>/awosbank:latest` no arquivo `.env`.

O Dependabot está configurado em `.github/dependabot.yml` para atualizações semanais do Maven, GitHub Actions e Docker.

---

## 🔑 Credenciais de teste

| Campo | Credencial |
|---|---|
| E-mail | `cliente@awosbank.com` |
| Senha | `123456` |

As credenciais dependem da configuração do usuário de demonstração na aplicação.

---

## 🎯 Objetivos do projeto

- Desenvolvimento backend com Java e Spring Boot.
- Construção de aplicações web com arquitetura MVC.
- Autenticação e autorização com Spring Security.
- Persistência de dados com JPA e Hibernate.
- Desenvolvimento de interfaces com Thymeleaf e Bootstrap.
- Integração entre frontend e backend.
- Requisições assíncronas com Fetch API e AJAX.
- Validação de dados e tratamento de exceções.
- Testes automatizados e de integração.
- Migrações de banco de dados com Flyway.
- Containerização e automação de pipelines CI/CD.
- Auditoria de operações e rastreabilidade de eventos.

---

## ✅ Melhorias implementadas

- [x] Migração do H2 para PostgreSQL com Flyway.
- [x] Testes automatizados com JUnit e Mockito.
- [x] Testes de integração com PostgreSQL via Testcontainers.
- [x] Dockerfile multi-stage e Docker Compose.
- [x] Documentação da API com Swagger/OpenAPI.
- [x] Histórico de auditoria das operações.
- [x] Pipeline de integração e entrega contínuas.

## 🔮 Próximos passos

- [ ] Autenticação JWT para a API.
- [ ] Rate limiting e bloqueio temporário após falhas de login.
- [ ] Interface web para consulta ao histórico de auditoria.

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
