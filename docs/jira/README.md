# Importar as tarefas no Jira

O arquivo [tarefas-jira.csv](tarefas-jira.csv) tem as **27 tarefas** das 3 duplas (uma por requisito e por parte, backend ou frontend). Ele é gerado a partir da mesma lista que alimenta os arquivos em [../tarefas/](../tarefas/), então os dois dizem a mesma coisa.

## Colunas

| Coluna | Conteúdo |
|---|---|
| Summary | `[D1-BE-01] Título` — o código identifica dupla (D1–D3), parte (BE backend, FE frontend) e número |
| Issue Type | `Task` |
| Priority | `High` ou `Medium` (se o seu Jira usar outros nomes, mapeie na importação) |
| Labels (3 colunas) | `dupla-N`, `backend`/`frontend` e o primeiro requisito (ex.: `RF001`) |
| Description | o que fazer, requisitos, arquivos, dependências e critério de pronto |

## Como importar

1. No projeto do Jira, use a importação de CSV (o caminho do menu varia conforme o projeto seja *team-managed* ou *company-managed*; procure por "Importar"/"Import" nas configurações do projeto ou em Sistema → Importação externa).
2. Escolha `tarefas-jira.csv`, codificação **UTF-8**, delimitador vírgula.
3. Mapeie as colunas pelos nomes iguais. As 3 colunas `Labels` mapeiam para o campo Labels.
4. Confira a prévia (acentos e quebras de linha na descrição) e importe.

Depois de importar, o Jira dá a chave real a cada card (`KAN-1`, `KAN-2`…). Se a chave do projeto não for `KAN`, troque nos exemplos de [CONTRIBUTING.md](../../CONTRIBUTING.md) e do `CLAUDE.md`.

Sugestão: crie um **Epic por dupla** (ou um filtro pelo label `dupla-N`) e atribua cada card ao responsável. O código `[D1-BE-01]` no título deixa as dependências citadas nos arquivos de tarefas fáceis de achar.
