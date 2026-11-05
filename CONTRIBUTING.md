# Como contribuir (fork + Pull Request)

O repositório oficial é `Igorzon1/SmartProd`. O Igor revisa e junta os Pull Requests e faz a entrega da A3. **Ninguém faz push direto nele**: cada pessoa trabalha no seu *fork* e abre um PR.

As tarefas ficam no **Jira** (chave `KAN`). Antes de começar, leia [docs/09-divisao-de-tarefas.md](docs/09-divisao-de-tarefas.md) e o arquivo da sua dupla em [docs/tarefas/](docs/tarefas/).

## 1. Preparar o ambiente (uma vez)

1. Conta no GitHub, **Git** e **JDK 21** (backend). Para o frontend basta um navegador e Node ou Python para servir os arquivos.
2. Na página do repositório oficial, clique em **Fork**. Isso cria `SEU-USUARIO/SmartProd`.
3. Clone o **seu fork** e registre o oficial como `upstream`:

```bash
git clone https://github.com/SEU-USUARIO/SmartProd.git
cd SmartProd
git remote add upstream https://github.com/Igorzon1/SmartProd.git
```

## 2. A cada tarefa

**a) Atualize sua `main` com a do repositório oficial:**

```bash
git fetch upstream
git switch main
git merge --ff-only upstream/main
git push origin main
```

**b) Crie a branch com a chave do Jira:**

```bash
git switch -c KAN-5-login-backend
```

**c) Trabalhe só nos arquivos da sua dupla** (veja a tabela de donos em [docs/09](docs/09-divisao-de-tarefas.md)). Commits pequenos e descritivos, com a chave no começo:

```bash
git commit -m "KAN-5 feat: login com token JWT"
```

**d) Teste antes de abrir o PR:**

```bash
cd backend && ./mvnw verify      # backend (no Windows: .\mvnw.cmd verify)
```

No frontend, sirva a pasta (`npx serve -l 5500 frontend`) e confira a tela no navegador, sem erros no console.

**e) Envie para o seu fork e abra o PR:**

```bash
git push -u origin KAN-5-login-backend
```

No GitHub, abra o Pull Request **do seu fork para `Igorzon1/SmartProd`, branch `main`**, com o título `KAN-5 login com token` e o template preenchido.

**f) Revisão.** O Igor comenta no PR. Para ajustar, faça novos commits na mesma branch e dê `git push` de novo; o PR atualiza sozinho. Depois do merge, apague a branch e repita o passo (a).

## 3. Jira

Mova o card conforme avança: **A fazer → Em andamento** (ao criar a branch) → **Em revisão** (ao abrir o PR) → **Concluído** (depois do merge). Um PR por card; mantenha o PR pequeno.

## 4. Regras para evitar conflito

- Edite só as pastas da sua dupla. Precisa mexer em arquivo de outra dupla ou compartilhado (`pom.xml`, `application*.yml`, `SecurityConfig`, `js/core/`, `css/base.css`)? Abra um PR **separado e pequeno** só com isso e avise o grupo.
- **Migrações do banco:** crie sempre arquivo novo, nomeado com data e hora: `V202610271430__criar_insumos.sql`. Nunca edite migração já mergeada.
- Controle de acesso por endpoint com `@PreAuthorize`; não edite o `SecurityConfig`.
- Nunca commite senhas, tokens, `.env` nem dados pessoais (o repositório é público).
- O CI roda no PR. Na primeira vez de cada pessoa, o Igor precisa aprovar a execução do workflow.

## 5. Deu conflito?

```bash
git fetch upstream
git rebase upstream/main
# resolva os arquivos marcados, depois:
git add .
git rebase --continue
git push --force-with-lease
```

Se travar, chame o Igor antes de tentar de tudo.

## 6. Usando IA

Cole no começo da conversa: *"Leia CLAUDE.md, docs/09-divisao-de-tarefas.md e docs/tarefas/<arquivo da minha dupla>. Trabalhe só nos arquivos da minha dupla e rode ./mvnw verify antes de terminar."*
