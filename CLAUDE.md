# SmartProd — contexto para IAs e desenvolvedores

Projeto A3 da UC HUB (Ânima). Equipe: Ana, Breno, Giovanne, Igor, Ledson, Marcelo.
**Idioma:** código de domínio, interface, commits e documentação em **português (pt-BR)**; termos técnicos (Controller, Service, Repository) ficam em inglês.

## O que é

Sistema web de gestão de produção para padarias e confeitarias pequenas e médias. Transforma a cozinha artesanal numa esteira guiada: ficha técnica com **Porcentagem do Padeiro**, cálculo proporcional de insumos, **Kanban de Ordens de Produção (OPs)** com passo a passo e temporizadores, **registro de perdas/refugos** e **dashboard** de rendimento e desperdício para o gerente.

Perfis: **Gerente** (custos, relatórios, cadastros) e **Funcionário** (cozinha, touchscreen, registra perdas).

## Leia antes de implementar

| Preciso de… | Arquivo |
|---|---|
| Problema, escopo e fora de escopo | [docs/01-visao-e-escopo.md](docs/01-visao-e-escopo.md) |
| Requisitos funcionais (RF001–RF015) e não-funcionais | [docs/02-requisitos.md](docs/02-requisitos.md) |
| Fluxos, telas previstas, concorrentes | [docs/03-pre-projeto.md](docs/03-pre-projeto.md) |
| Canvas acadêmico (12 blocos) e referências | [docs/04-canvas-uc-hub.md](docs/04-canvas-uc-hub.md) |
| Regras, prazos e entregas da A3 | [docs/05-regras-a3.md](docs/05-regras-a3.md) |
| Arquitetura e convenções técnicas | [docs/06-arquitetura.md](docs/06-arquitetura.md) |
| Entidades, fórmulas e regras de negócio | [docs/07-dominio.md](docs/07-dominio.md) |
| Dúvidas e inconsistências ainda abertas | [docs/08-questoes-em-aberto.md](docs/08-questoes-em-aberto.md) |
| Quem é dono de cada pasta e contratos entre as duplas | [docs/09-divisao-de-tarefas.md](docs/09-divisao-de-tarefas.md) |
| Tarefas de cada dupla (também no Jira) | [docs/tarefas/](docs/tarefas/) |
| Como contribuir (fork + Pull Request + Jira) | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Por que Java/Spring Boot e não Node.js | [docs/decisoes/0001-backend-java-spring-boot.md](docs/decisoes/0001-backend-java-spring-boot.md) |

Em conflito entre documentos, vale esta ordem: **ADRs em `docs/decisoes/` > `docs/02-requisitos.md` > demais docs**. Se algo estiver em `docs/08-questoes-em-aberto.md`, **pergunte ao usuário** em vez de decidir sozinho.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | **Java 21**, **Spring Boot 3.5.x**, Maven (use o wrapper `mvnw`) |
| Starters | Web, Data JPA, Validation, Security, Actuator; springdoc-openapi (Swagger UI) |
| Banco | **PostgreSQL** em produção (nuvem, free tier); **H2 em memória** em dev e testes; migrações **Flyway** |
| Frontend | HTML5 + CSS3 + JavaScript puro, em `frontend/` (sem framework por enquanto) |
| Testes | JUnit 5, AssertJ, Mockito, MockMvc, `@DataJpaTest`, spring-security-test |

> **Atenção:** os PDFs originais (Requisitos, Canvas) ainda citam **Node.js/Express**. Isso foi **substituído** por Java/Spring Boot (ADR-0001). **Nunca gere backend em Node.js.**

## Comandos

```bash
cd backend
./mvnw spring-boot:run     # API em http://localhost:8080 (perfil dev, H2)
./mvnw test                # testes
./mvnw verify              # build + testes (é o que o CI roda)
```

No Windows/PowerShell use `.\mvnw.cmd`. Exige **JDK 21**: se `java -version` mostrar outra versão, aponte `JAVA_HOME` para um JDK 21.
Swagger UI (dev): http://localhost:8080/swagger-ui.html · Saúde: http://localhost:8080/actuator/health

## Estrutura

```
backend/    API Spring Boot (pacote raiz br.com.smartprod)
frontend/   HTML/CSS/JS estático (consome a API REST)
docs/       documentação do projeto (fonte da verdade)
.github/    CI (GitHub Actions)
```

Backend organizado **por funcionalidade** (`usuario`, `auth`, `insumo`, `receita`, `producao`, `perda`, `dashboard`), mais `config` e `shared`. Cada pacote tem seus `Controller`, `Service`, `Repository`, entidade e `dto/` (records). Só `usuario` tem código por enquanto; os demais têm só o `package-info.java` com o dono.

Frontend em **páginas separadas**: `frontend/pages/<tela>/index.html` + `main.js`, com a base comum em `frontend/js/core/` (`api.js`, `auth.js`, `menu.js`, `config.js`) e `frontend/css/base.css`. Scripts são ES modules (precisa servir por HTTP).

**Trabalho em 3 duplas (1 back + 1 front cada), cada uma dona de pastas específicas** — veja [docs/09-divisao-de-tarefas.md](docs/09-divisao-de-tarefas.md). **Quando alguém pedir uma tarefa, edite só os arquivos da dupla dela**; arquivos compartilhados (`pom.xml`, `application*.yml`, `SecurityConfig`, `js/core/`, `css/base.css`) só por PR separado e pequeno. Controle de acesso por endpoint com `@PreAuthorize`, sem mexer no `SecurityConfig`. Migrações novas: `V<aaaaMMddHHmm>__descricao.sql`.

## Convenções

- **Banco:** só muda via migração Flyway em `backend/src/main/resources/db/migration` (`V<n>__descricao.sql`). Nunca edite migração já commitada; nunca use `ddl-auto=update`. SQL deve rodar em PostgreSQL **e** H2.
- **Dinheiro e quantidades:** `BigDecimal` (nunca `double`). Moeda R$ com 2 casas; quantidades com 3.
- **Datas:** `Instant` (UTC) no banco e na API; a interface exibe em `America/Sao_Paulo`.
- **API:** prefixo `/api`, JSON, recursos em português no plural (`/api/insumos`, `/api/fichas-tecnicas`, `/api/ordens-producao`, `/api/perdas`, `/api/dashboard`). Erros em RFC 7807 (`ProblemDetail`).
- **Entidades JPA nunca saem na API:** use DTOs (records) e `@Valid` nas entradas.
- **Segurança:** senhas com BCrypt (força ≥ 10, projeto usa 12). Dados financeiros (custos de insumos, perdas em R$, dashboard) **só para GERENTE** (`@PreAuthorize("hasRole('GERENTE')")`) e DTOs de Funcionário **não** carregam campos de custo.
- **Interface operacional (Funcionário):** botões ≥ 48px, alto contraste, fonte grande, no máximo 3 cliques por ação (RNF-U001/U002); progresso da etapa salvo em `localStorage` (RNF-C002).
- **Desempenho:** cálculo proporcional e carga do Kanban ≤ 1,5 s (RNF-D001); índices nas colunas de consulta frequente.

## Regras de negócio críticas

- **Porcentagem do Padeiro:** farinha = 100%; cada ingrediente = `quantidade / quantidade_farinha × 100`.
- **Recálculo por demanda:** `fator = unidades_desejadas / rendimento_padrao`; cada quantidade × fator.
- **Recálculo por estoque:** fator = menor `estoque_i / quantidade_i` entre os ingredientes.
- **OP:** `PENDENTE → EM_ANDAMENTO → CONCLUIDA`. Detalhes em [docs/07-dominio.md](docs/07-dominio.md).

## Fora do escopo (não implementar)

Financeiro e notas fiscais · PDV · compras/fornecedores · análises preditivas/IA · ponto e RH · app mobile nativo. Login social (Google/Facebook) aparece no mockup mas **não** está no escopo (só e-mail e senha).

## Testes

A A3 exige testes no projeto (unitários obrigatórios; integração desejável). Toda regra de negócio nova leva teste unitário; todo endpoint novo leva teste de integração com MockMvc. Rode `./mvnw verify` antes de commitar.

## Git

- **Fork + Pull Request:** cada pessoa trabalha no seu fork e abre PR para `Igorzon1/SmartProd` (`main`); ninguém dá push direto no repositório oficial. Passo a passo em [CONTRIBUTING.md](CONTRIBUTING.md).
- **Jira** (chave `KAN`): uma branch e um PR por card. Branch `KAN-5-login-backend`, commit `KAN-5 feat: login com token JWT`, título do PR `KAN-5 login com token`.
- Commits **descritivos** em pt-BR, no estilo `<chave> tipo: resumo`. Isso é critério de aceitação do projeto.
- Nunca commite segredos (`.env`, senhas, URLs de banco com credencial) nem dados pessoais da equipe (matrícula, e-mail). O repositório é **público**.
