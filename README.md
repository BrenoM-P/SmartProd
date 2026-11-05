# SmartProd

![Backend CI](https://github.com/Igorzon1/SmartProd/actions/workflows/backend-ci.yml/badge.svg)

Sistema web de gestão de produção para padarias e confeitarias: transforma a cozinha artesanal numa **esteira guiada** — ficha técnica com Porcentagem do Padeiro, Kanban de Ordens de Produção com passo a passo e temporizadores, registro de perdas e dashboard de rendimento e desperdício para o gerente.

Projeto A3 da UC HUB (Ânima). Equipe: Ana, Breno, Giovanne, Igor, Ledson e Marcelo.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Maven |
| Banco | PostgreSQL (produção), H2 (dev/testes), Flyway |
| Frontend | HTML5, CSS3, JavaScript |

## Como rodar

Pré-requisito: **JDK 21**. Se `java -version` mostrar outra versão, aponte `JAVA_HOME` para um JDK 21 (o IntelliJ baixa um em `~/.jdks`).

```bash
cd backend
./mvnw spring-boot:run
```

No Windows/PowerShell use `.\mvnw.cmd spring-boot:run`.

- API: http://localhost:8080 — saúde em `/actuator/health`, documentação em `/swagger-ui.html`
- Frontend: veja [frontend/README.md](frontend/README.md)
- Testes: `./mvnw verify` (dentro de `backend/`)

O perfil `dev` usa H2 em memória, então não precisa instalar banco. Porta 8080 ocupada? Defina `PORT` (ex.: `$env:PORT=8081` no PowerShell) e ajuste `frontend/js/config.js`.

## Estrutura

```
backend/    API Spring Boot
frontend/   interface web estática
docs/       documentação (comece por docs/README.md)
CLAUDE.md   contexto do projeto para IAs
```

## Documentação

Tudo está em [docs/](docs/README.md): visão e escopo, requisitos (RF001–RF015), pré-projeto, canvas, regras da A3, arquitetura, modelo de domínio e questões em aberto. Decisões técnicas ficam em [docs/decisoes/](docs/decisoes/).

> Os PDFs originais ainda citam Node.js; o backend foi trocado para Java/Spring Boot — ver [ADR-0001](docs/decisoes/0001-backend-java-spring-boot.md).

## Como contribuir

O trabalho é dividido em **3 duplas (1 backend + 1 frontend cada)**, cada uma dona de pastas específicas: veja [docs/09-divisao-de-tarefas.md](docs/09-divisao-de-tarefas.md) e as tarefas em [docs/tarefas/](docs/tarefas/). Cada pessoa faz **fork** e abre **Pull Request**; as tarefas ficam no Jira (chave `KAN`). Passo a passo em [CONTRIBUTING.md](CONTRIBUTING.md).

## Status

Esqueleto pronto: API sobe, migração de usuários, segurança base, testes e frontend por páginas. Próximo passo: **RF001** (login e perfis), que destrava as demais duplas. Veja [docs/02-requisitos.md](docs/02-requisitos.md).
