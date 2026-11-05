# 08 — Questões em aberto

Inconsistências encontradas nos documentos e decisões ainda não tomadas. **IAs: não decidam sozinhas — pergunte ao usuário.** Quando resolver, mova o item para "Resolvidas" com data e decisão.

## Abertas

| # | Questão | Onde aparece | Sugestão |
|---|---|---|---|
| Q1 | Perfis: a Visão fala em 2 perfis (Gerente/Funcionário), os stakeholders têm 3 papéis (Gerente, Padeiro, Ajudante) e o mockup de login tem "Gerente/Padeiro" | Visão §4, Requisitos §2.2, Pré-Projeto | Manter 2 perfis de acesso; Padeiro e Ajudante são `FUNCIONARIO` |
| Q2 | RF007 recalcula "por estoque", mas não há requisito de cadastro/controle de estoque | RF007, Visão F3 | Gerente/padeiro **informa o estoque disponível na própria calculadora**; sem entidade de estoque nesta versão |
| Q4 | Disponibilidade 99,5% (RNF-C001) é incompatível com *free tier* que "dorme" | RNF-C001, restrição de orçamento | Tratar como meta de projeto; documentar a limitação na entrega |
| Q5 | Estados da OP (3: pendente, em andamento, concluída) × colunas do Kanban por etapa (pesagem, mistura, fermentação, forno…) × nome da coluna inicial ("Pendente" vs "A Fazer") | RF009, Canvas, Pré-Projeto | OP tem 3 estados; Kanban v1 usa as 3 colunas. Etapas viram o campo `etapaAtual` (evolução) |
| Q6 | Mockup de login mostra Google/Facebook, mas o escopo é só e-mail e senha | Pré-Projeto §7, Visão F1 | Fora do escopo; ajustar o mockup |
| Q7 | Conversão de unidades (g↔kg, ml↔L, un) no custo e no cálculo | RF004, RF006, RF007 | Normalizar para unidade base por tipo (massa, volume, contagem) |
| Q8 | Canvas lista análises sem RF: perdas por etapa e motivo, atrasos vs horário de entrega, custo real por produto, gargalo | Canvas §9 | Backlog pós-MVP, exceto o que os RF012–RF015 já cobrem |
| Q9 | Critérios completos e prazo final da A3 não estão neste repositório | [05-regras-a3.md](05-regras-a3.md) | Colar o documento de regras lá |
| Q10 | PDFs oficiais (Requisitos e Canvas) ainda dizem Node.js | RNF-Comp002, Restrições, Canvas §8 | Atualizar os PDFs/Canvas antes da entrega |
| Q11 | Como identificar a "farinha" (100%)? Pode haver mais de uma farinha | RF006 | Flag `ehFarinha` por ingrediente da ficha; soma das farinhas = 100% |
| Q12 | A perda é em kg (fluxo diz "peso kg") ou em unidades (RF011 diz "quantidade")? Perde-se ingrediente ou produto? Como converter em R$? | RF011, RF013, fluxo do Pré-Projeto | Registrar peso em kg + opcional unidades; R$ via custo/kg do produto ou do ingrediente |
| Q13 | O que conta como "produzido" no rendimento diário (RF012)? | RF012, docs/09 | Informar `quantidadeProduzida` ao concluir a OP (padrão = quantidade prevista) |

## Pequenas correções nos PDFs

- RF011 estava **sem status** no PDF (assumido "A implementar").
- RF007 tinha texto duplicado ("estoque. estoque").
- RNF-D002 cita "OIDs" (provável erro de digitação de "OPs").

## Resolvidas

- **2026-10-26** — Backend em Java/Spring Boot em vez de Node.js (ADR-0001).
- **2026-11-04** — Q3: autenticação por token JWT (HS256), stateless, com validade padrão de 8 h.
