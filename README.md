# 🏦 AWOS Bank — Internet Banking

<p align="center">
  <strong>Uma experiência de banco digital desenvolvida com Java e Spring Boot.</strong>
  <br>
  Gerencie contas, transações financeiras e chaves PIX em uma aplicação web moderna, com segurança, auditoria e infraestrutura automatizada.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot" alt="Spring Boot 3.2.5">
  <img src="https://img.shields.io/badge/Thymeleaf-3.1-005F0F?style=for-the-badge&logo=thymeleaf" alt="Thymeleaf 3.1">
  <img src="https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap" alt="Bootstrap 5.3">
  <img src="https://img.shields.io/badge/JavaScript-Vanilla-F7DF1E?style=for-the-badge&logo=javascript" alt="JavaScript">
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL 16">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker Compose">
  <img src="https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="OpenAPI Swagger">
  <img src="https://img.shields.io/badge/CI-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions">
  <img src="https://img.shields.io/badge/Render-Live_Demo-46E3B7?style=for-the-badge&logo=render&logoColor=white" alt="Render">
</p>

---

## 🌐 Demonstração online

<p align="center">
  <a href="https://awosbank.onrender.com">
    <img src="https://img.shields.io/badge/ACESSAR_DEMONSTRAÇÃO-Visitar_AWOS_Bank-16A34A?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Acessar demonstração online">
  </a>
</p>

**Acesse:** https://awosbank.onrender.com

---

## ✨ Funcionalidades

### 🔐 Autenticação e segurança

- Cadastro e login de clientes.
- Criptografia de senhas com BCrypt.
- Autenticação e proteção de rotas com Spring Security.
- Controle de acesso às funcionalidades da aplicação.

### 📊 Dashboard bancário

- Visualização do saldo da conta.
- Opção para ocultar e exibir o saldo.
- Gráficos de entradas e saídas com Chart.js.
- Atualizações dinâmicas utilizando AJAX e Fetch API.

### 💳 Extrato bancário

- Consulta ao histórico de transações.
- Filtros por período, com datas inicial e final.
- Filtragem por tipo de transação.
- Visualização organizada das movimentações financeiras.

### 🔑 Gerenciamento de chaves PIX

- Cadastro de chaves PIX.
- Consulta, edição e exclusão de chaves.
- Gerenciamento das chaves vinculadas ao cliente.

### 👤 Perfil do cliente

- Consulta e atualização de dados cadastrais.
- Máscaras dinâmicas para campos de formulário.
- Interface intuitiva para edição de informações.

### 🕵️ Histórico de auditoria

- Registro de cadastros, logins bem-sucedidos e falhos, transferências, depósitos, saques, alterações de perfil e operações com chaves PIX.
- Registro de falhas de negócio, incluindo saldo insuficiente e senha incorreta.
- Persistência dos registros de falha em transações independentes, permitindo que sobrevivam ao rollback da operação principal.
- Armazenamento de usuário, ação, resultado, detalhes e IP de origem, sem registrar senhas.
- Proteção *append-only*: um trigger do PostgreSQL bloqueia operações de `UPDATE` e `DELETE`.
- Consulta dos registros pelo endpoint `GET /api/auditoria`.

### 🧾 Comprovantes e tratamento de erros

- Geração de comprovantes de transações.
- Exibição de recibos em modais.
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
| Banco de dados | PostgreSQL 16 |
| Migrações | Flyway |
| Documentação da API | springdoc-openapi, Swagger UI |
| Testes | JUnit 5, Mockito, Spring Boot Test, Testcontainers |
| Cobertura de testes | JaCoCo |
| Infraestrutura | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Hospedagem | Render |
| Validação | Spring Boot Validation |
| Templates | Thymeleaf |
| Interface | Bootstrap 5.3, CSS |
| Interatividade | JavaScript, Fetch API, AJAX |
| Gráficos | Chart.js |
| Produtividade | Lombok |
| Build | Maven |

---

## 🏗️ Arquitetura

O AWOS Bank utiliza uma arquitetura baseada no ecossistema Spring Boot, com separação de responsabilidades entre as camadas de apresentação, negócio e persistência.

| Camada | Responsabilidade |
|---|---|
| Controllers | Recebem e processam requisições HTTP. |
| Services | Centralizam as regras de negócio. |
| Repositories | Gerenciam a persistência com Spring Data JPA. |
| Entities | Representam os dados persistidos no banco. |
| Templates | Renderizam as páginas com Thymeleaf. |
| Spring Security | Gerencia autenticação e autorização. |
| Flyway | Versiona e executa as migrações do banco de dados. |

O Hibernate utiliza `ddl-auto=validate`, garantindo que o esquema do banco seja validado em relação às entidades, enquanto as alterações estruturais são controladas por migrations versionadas.

---

## 🚀 Como executar localmente

### Pré-requisitos

- Java 17 ou superior.
- Docker e Docker Compose v2.
- Git.

Para executar a aplicação diretamente pelo Maven, também é necessário ter o Maven instalado.

### Opção 1: Docker Compose

A maneira mais simples de executar o projeto é utilizar o Docker Compose.

**1. Clone o repositório**

```bash
git clone https://github.com/adanwilliamdev/awosbank.git
cd awosbank
```

**2. Configure as variáveis de ambiente**

Se necessário, copie o arquivo de exemplo e ajuste as configurações:

```bash
cp .env.example .env
```

No PowerShell do Windows, utilize:

```powershell
Copy-Item .env.example .env
```

**3. Inicie a aplicação**

```bash
docker compose up -d --build
```

**4. Acesse os serviços**

| Serviço | Endereço |
|---|---|
| Aplicação web | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |

Para interromper os serviços:

```bash
docker compose down
```

Para interromper os serviços e remover também os volumes persistentes:

```bash
docker compose down -v
```

> **Atenção:** o comando `docker compose down -v` remove os volumes associados ao projeto, podendo apagar os dados persistidos do banco.

### Opção 2: Maven + PostgreSQL no Docker

Nesta opção, apenas o banco de dados é executado em um contêiner.

**1. Inicie o PostgreSQL**

```bash
docker compose up -d db
```

**2. Execute a aplicação**

```bash
mvn spring-boot:run
```

No perfil `dev`, a aplicação executa as migrations do Flyway e inicializa o usuário de demonstração conforme a configuração do projeto.

---

## ⚙️ Configuração por variáveis de ambiente

| Variável | Padrão de desenvolvimento | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Define o perfil de execução: `dev` ou `prod`. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/awosbank` | URL JDBC do PostgreSQL. |
| `DB_USERNAME` | `awosbank` | Usuário do banco no ambiente de desenvolvimento. |
| `DB_PASSWORD` | `awosbank` | Senha do banco no ambiente de desenvolvimento. |
| `FORWARD_HEADERS_STRATEGY` | `none` | Configura o tratamento de cabeçalhos encaminhados por proxy reverso. |

No perfil `prod`, as credenciais do banco devem ser fornecidas explicitamente, o Swagger é desativado e o usuário de demonstração não é criado.

**Recomendações para produção:**

- Utilize HTTPS.
- Configure credenciais fortes e exclusivas.
- Execute a aplicação atrás de um proxy reverso confiável, quando aplicável.
- Configure corretamente os cabeçalhos encaminhados para registrar o IP de origem na auditoria.
- Mantenha os segredos fora do repositório.

### Migrações do banco de dados

As migrations ficam em:

```text
src/main/resources/db/migration
```

Para alterar o esquema, crie uma nova migration, por exemplo:

```text
V3__descricao_da_alteracao.sql
```

Não edite migrations que já foram aplicadas em ambientes persistentes.

> **Migração de instalações antigas:** o H2 em memória não exige migração de dados persistentes. Para bancos PostgreSQL antigos criados pelo Hibernate, avalie o uso de `baseline-version=1` e `baseline-on-migrate=true` para aplicar somente as migrations pendentes. Faça backup e valide o esquema antes de executar o procedimento.

---

## 📖 Documentação da API

Com a aplicação em execução no perfil `dev`, acesse:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

A API utiliza a sessão de autenticação da aplicação web. Para testar endpoints protegidos:

1. Entre em `/login` no mesmo navegador.
2. Autentique-se com uma conta válida.
3. Abra o Swagger UI.
4. Utilize a opção `Try it out` nos endpoints disponíveis.

Os endpoints estão organizados por funcionalidades, incluindo transações, chaves PIX e auditoria.

---

## 🧪 Testes automatizados

O projeto utiliza JUnit 5, Mockito, Spring Boot Test, Testcontainers e JaCoCo para validar regras de negócio, persistência, segurança e integração.

**Executar os testes:**

```bash
mvn test
```

**Executar a verificação completa e gerar o relatório de cobertura:**

```bash
mvn verify
```

O relatório de cobertura do JaCoCo fica disponível em:

```text
target/site/jacoco/index.html
```

### Cobertura de testes

| Tipo | Classes e verificações |
|---|---|
| Testes unitários | `TransacaoServiceTest`, `UsuarioServiceTest`, `ContaServiceTest`, `PixKeyServiceTest`, `AuditoriaServiceTest`, `AuditoriaAspectTest`, `DocumentoValidatorTest` |
| Repositórios | `RepositoriosJpaTest`, com `@DataJpaTest` |
| Integração | `ApiIntegrationTest`, `AuditoriaIntegrationTest`, `SegurancaWebTest`, `TransferenciaConcorrenciaTest` |
| PostgreSQL real | `PostgresMigrationTest`, utilizando Testcontainers |

Os testes com PostgreSQL real validam migrations, compatibilidade entre entidades e esquema, locks, constraints e o trigger de auditoria. O teste baseado em Testcontainers é ignorado automaticamente quando o Docker não está disponível, conforme a configuração do projeto.

---

## 🔄 CI/CD

O workflow `.github/workflows/ci.yml` automatiza etapas de integração e entrega contínuas.

### Pipeline de integração

A cada push ou pull request, o pipeline executa:

- Build e testes com `mvn verify`, utilizando Java 17.
- Geração de relatórios de testes e cobertura como artefatos.
- Smoke tests com Docker Compose.
- Verificações dos endpoints `/actuator/health`, `/login` e da documentação OpenAPI.

### Publicação de imagens Docker

Em pushes na branch `main` ou em tags `v*`, o workflow publica a imagem no GitHub Container Registry (GHCR), utilizando tags como `latest`, SHA do commit e versão.

Exemplo de referência da imagem:

```text
ghcr.io/adanwilliamdev/awosbank:latest
```

Para implantar uma imagem publicada em outro servidor, configure a variável `APP_IMAGE` no `.env` e execute:

```bash
docker compose pull app
docker compose up -d
```

O Dependabot, configurado em `.github/dependabot.yml`, verifica dependências do Maven, GitHub Actions e Docker e pode abrir pull requests semanais.

---

## 🎯 Objetivos do projeto

O AWOS Bank foi desenvolvido para praticar e demonstrar conhecimentos em:

- Desenvolvimento backend com Java e Spring Boot.
- Arquitetura MVC e separação de responsabilidades.
- Autenticação e autorização com Spring Security.
- Persistência de dados com JPA e Hibernate.
- Versionamento de esquema com Flyway.
- Desenvolvimento de interfaces com Thymeleaf e Bootstrap.
- Integração entre frontend e backend.
- Requisições HTTP assíncronas.
- Validação de dados e tratamento de exceções.
- Auditoria de operações financeiras.
- Testes unitários, testes de integração e testes de concorrência.
- Conteinerização e automação de pipelines CI/CD.
- Documentação de APIs com OpenAPI e Swagger.

---

## ✅ Melhorias implementadas

- [x] Migração do H2 para PostgreSQL com migrations Flyway.
- [x] Testes automatizados com JUnit e Mockito.
- [x] Testes de integração com PostgreSQL via Testcontainers.
- [x] Dockerização com Dockerfile multi-stage e Docker Compose.
- [x] Documentação da API com Swagger/OpenAPI.
- [x] Histórico de auditoria das operações.
- [x] Pipeline de CI/CD com GitHub Actions.
- [x] Publicação automatizada de imagens Docker no GHCR.
- [x] Implantação de demonstração online no Render.

---

## 🔮 Próximos passos

- [ ] Implementar autenticação JWT para a API, além da sessão web.
- [ ] Adicionar rate limiting e bloqueio temporário após tentativas de login malsucedidas.
- [ ] Desenvolver uma interface web para consulta do histórico de auditoria.

---

## ⚠️ Aviso

Este projeto tem finalidade educacional e demonstrativa. Não utilize dados bancários reais nem o disponibilize como serviço financeiro de produção sem implementar e auditar os controles de segurança, as regras de negócio e os requisitos regulatórios aplicáveis.

---

## 👨‍💻 Autor

**Adan William Oliveira Santos**

Desenvolvedor com foco em Java, Spring Boot e desenvolvimento backend.

- GitHub: [@adanwilliamdev](https://github.com/adanwilliamdev)
- Portfólio: [adanwilliamdev.github.io](https://adanwilliamdev.github.io/)

---

<p align="center">
  ⭐ Se o projeto foi útil ou interessante, considere deixar uma estrela no repositório!
</p>
