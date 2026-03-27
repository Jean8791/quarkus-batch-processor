# Fluxo de Processamento

Este documento descreve como um job batch é executado, da configuração até o resultado final.

O objetivo é explicar o comportamento do motor em tempo de execução, passo a passo.

## Visão geral

Um job segue este fluxo em alto nível:

Código da aplicação
↓
Configuração do processor
↓
`run()`
↓
Execução da query
↓
Iteração em streaming
↓
Formação de chunks
↓
Processamento
↓
Tratamento de erro e retry
↓
Métricas e resultado final

O modelo de execução é sequencial e orientado a chunks.

## Etapa 1 — Configuração do job

O processo começa quando o código da aplicação configura uma instância do processor por meio da API fluente.

A configuração típica inclui:

- definição de query HQL
- definição de query nativa
- `rowMapper` para query nativa
- parâmetros da query
- tamanho do chunk
- modo transacional
- callback de processamento
- estratégia de erro
- estratégia de retry
- listener de conclusão de chunk
- comportamento de logging
- nome do processor

Nesta etapa, o job ainda está apenas sendo descrito. Nenhum registro foi processado.

## Etapa 2 — Início da execução com `run()`

O processamento de fato começa quando `run()` é chamado.

Neste ponto, o motor:

- valida as opções configuradas
- resolve valores padrão quando necessário
- prepara o contexto de execução
- inicializa logging e coleta de métricas

Se alguma configuração obrigatória estiver ausente, a execução para antes de acessar o banco.

## Etapa 3 — Preparação da query

Depois da validação, o motor prepara a origem dos dados para iteração.

Dependendo da configuração, ele pode executar:

- uma query HQL
- uma query SQL nativa

Se o modo nativo for usado, o `rowMapper` transforma a linha bruta no tipo alvo do processamento.

Os parâmetros são aplicados antes do início da iteração.

## Etapa 4 — Iteração em streaming

Quando a query está pronta, o motor passa a ler os registros progressivamente, em vez de materializar todo o resultado em memória.

Nesta fase, ele:

- abre a origem iterável
- lê os registros um a um
- expõe esses registros por meio de um iterador

Esse é um ponto central do comportamento de memória do motor, pois apenas um conjunto limitado de registros fica em memória a cada momento.

## Etapa 5 — Formação do chunk

À medida que os registros são lidos, eles são acumulados em um buffer até que o tamanho configurado do chunk seja atingido.

Exemplo:

- se o chunk for `1000`, o motor acumula até 1000 registros
- quando o chunk enche, ele é enviado para processamento
- depois do processamento, um novo buffer é criado
- se a entrada acabar antes de encher o chunk, o chunk parcial final também é processado

Esse modelo mantém a memória limitada sem abrir mão de eficiência.

## Etapa 6 — Processamento do chunk

Quando um chunk fica pronto, o motor delega sua execução ao processador de chunk.

Nesta etapa:

- cada registro do chunk é enviado ao callback configurado
- contadores como processados, erros e retries são atualizados
- hooks de conclusão podem ser acionados ao final do chunk

É aqui que a lógica de negócio do consumidor realmente acontece.

## Etapa 7 — Retry

Se a tentativa de processar um registro falhar, o motor pode tentar novamente conforme a política configurada.

O comportamento típico de retry inclui:

- verificar se a exceção é elegível para retry
- limitar o número de tentativas
- aguardar entre tentativas, quando houver delay

Se o processamento tiver sucesso após retry, a execução segue normalmente e o contador de retries é atualizado.

Se o retry se esgotar, a falha é tratada conforme a estratégia de erro.

## Etapa 8 — Tratamento de erro

Quando um registro não pode ser processado com sucesso, o motor aplica a estratégia de erro configurada.

Resultados típicos:

- interromper a execução imediatamente
- continuar para o próximo registro
- notificar um handler de erro por registro

Isso permite atender tanto jobs estritos quanto jobs tolerantes a falhas.

Exemplos:

- migrações podem optar por continuar e registrar falhas
- conciliações críticas podem optar por falhar imediatamente

## Etapa 9 — Tratamento transacional

A execução do chunk pode ser envolvida em transação conforme o modo transacional configurado.

Conceitualmente, isso significa que o motor pode:

- processar sem transação explícita
- processar um chunk inteiro sob uma mesma transação

Esse controle é aplicado de forma consistente ao redor do chunk para manter o comportamento explícito e previsível.

Isso é especialmente importante em jobs que alteram estado no banco.

## Etapa 10 — Liberação de referências e conclusão do chunk

Depois que um chunk é processado, o motor executa tarefas de housekeeping.

Normalmente isso inclui:

- liberar referências ligadas ao chunk recém-processado
- notificar listeners de conclusão
- atualizar observação interna de progresso
- registrar progresso em log quando habilitado

Esse passo ajuda a manter execuções longas estáveis ao longo do tempo.

## Etapa 11 — Chunk final e fim da entrada

Quando a iteração chega ao fim do stream:

- qualquer registro restante no buffer é processado como chunk final parcial
- se nenhum registro tiver sido lido, a execução ainda assim termina corretamente
- os contadores finais são consolidados em um único resultado

Isso garante comportamento correto para:

- datasets completos
- último chunk parcial
- resultados vazios

## Etapa 12 — Resultado final

Ao final da execução, o motor retorna um objeto com o resumo do processamento.

Esse resultado normalmente inclui:

- total de registros processados
- total de erros
- total de retries
- instante de início
- instante de fim
- duração total

Esse resultado pode ser usado para:

- logging operacional
- monitoramento
- alertas
- relatórios
- orquestração de jobs

## Resumo do fluxo

O fluxo completo pode ser resumido assim:

Configurar processor
↓
Chamar `run()`
↓
Validar configuração
↓
Preparar query
↓
Ler registros em streaming
↓
Acumular chunk
↓
Processar cada registro
↓
Aplicar retry e tratamento de erro
↓
Finalizar chunk
↓
Repetir até o fim da entrada
↓
Retornar `ProcessingResult`

## Características operacionais

Esse fluxo possui algumas propriedades importantes:

- a execução é sequencial
- o uso de memória permanece limitado pelo streaming e pelo buffer de chunk
- o tamanho do chunk influencia fortemente throughput e custo transacional
- políticas de retry e erro afetam diretamente a resiliência
- logs de progresso podem ser habilitados para jobs longos

## Documentação relacionada

Para mais contexto, veja:

- [Visão geral da arquitetura](architecture-overview.md)
- [Organização dos pacotes](package-organization.md)
