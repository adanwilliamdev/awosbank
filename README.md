# 🏦 AWOS Bank — Internet Banking

Sistema de **Internet Banking desenvolvido em Java e Spring Boot**, com interface web moderna, autenticação segura e funcionalidades para gerenciamento de conta bancária, transações e chaves PIX.

O projeto simula a experiência de um banco digital, combinando processamento no backend com uma interface responsiva desenvolvida com Thymeleaf e Bootstrap.

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-005F0F?style=for-the-badge&logo=thymeleaf)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=for-the-badge&logo=bootstrap)
![JavaScript](https://img.shields.io/badge/JavaScript-Vanilla-F7DF1E?style=for-the-badge&logo=javascript)
![License](https://img.shields.io/badge/Status-Em%20Desenvolvimento-blue?style=for-the-badge)

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
| Banco de dados | H2 Database |
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

### Pré-requisitos

- Java 17 ou superior.
- Maven instalado ou Maven Wrapper disponível no projeto.
- Git.

### 1. Clone o repositório

```bash
git clone https://github.com/adanwilliamdev/awosbank.git
```

### 2. Acesse a pasta do projeto

```bash
cd awosbank
```

### 3. Execute a aplicação

Com Maven instalado:

```bash
mvn spring-boot:run
```

Se o projeto incluir Maven Wrapper, utilize:

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

### 4. Acesse a aplicação

Abra o navegador e acesse:

```text
http://localhost:8080
```

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

## 🔮 Possíveis melhorias futuras

- [ ] Migração do H2 para PostgreSQL.
- [ ] Testes automatizados com JUnit e Mockito.
- [ ] Implementação de testes de integração.
- [ ] Dockerização da aplicação.
- [ ] Documentação da API com Swagger/OpenAPI.
- [ ] Histórico de auditoria das operações.
- [ ] Pipeline de integração e entrega contínuas (CI/CD).

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
