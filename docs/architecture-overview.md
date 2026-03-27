# Visão Geral da Arquitetura

O `quarkus-batch-processor` é um motor de processamento batch para workloads relacionais de grande volume em aplicações Quarkus.

A arquitetura foi pensada para priorizar uso previsível de memória, fluxo de processamento explícito e fronteiras transacionais controladas.

## Objetivos da arquitetura

O motor foi desenhado para:

- processar datasets muito grandes com segurança
- evitar carregamento completo do resultado em memória
- manter o escopo transacional explícito e previsível
- suportar resiliência com retry e tratamento configurável de erro
- oferecer uma API simples, mantendo os detalhes internos isolados

## Ideia central

Em vez de materializar toda a query em memória, o motor consome os registros como stream.

Em alto nível, ele:

1. executa uma query no banco
2. lê o resultado progressivamente
3. agrupa os registros em chunks
4. processa cada chunk conforme a estratégia configurada
5. gera métricas e um resumo final da execução

Esse modelo é adequado para jobs longos, nos quais estabilidade de memória e throughput importam mais do que latência de request/respose.

## Pipeline principal

Banco de dados
↓
Resultado em streaming
↓
Abstração de iterador
↓
`ProcessingLoop`
↓
Formação de chunks
↓
Execução do chunk
↓
Métricas e resultado final

Cada etapa possui responsabilidade reduzida, o que ajuda na manutenção e evolução do código.

## Componentes principais

### `FastRecordProcessor`

Ponto de entrada público da biblioteca.

Sua função é expor a API fluente usada para configurar o processamento.

### `FastRecordProcessorImpl`

Coordenador interno da execução batch.

Ele é responsável por:

- validar a configuração
- criar o iterador de resultados
- selecionar a estratégia de execução
- coordenar o processamento dos chunks
- produzir o resultado final

### `ProcessingLoop`

Loop central do motor.

Ele é responsável por:

- iterar sobre os registros em streaming
- agrupar registros em chunks
- invocar o processador de chunk
- acionar notificações de conclusão
- liberar referências entre execuções

### `ProcessingMetricsLogger`

Encapsula o logging da execução.

Ele registra:

- início da execução
- progresso por chunk
- resumo final
- falhas de processamento

### `ChunkExecutionResult`

Representa o resultado de um chunk individual, incluindo contadores como processados, erros e retries.

### `ProcessingResult`

Representa o resumo final da execução batch.

É o principal objeto retornado ao chamador no fim do processamento.

## Organização em pacotes

A arquitetura está dividida em áreas principais:

- `config` — tipos de configuração que definem o comportamento
- `contract` — contratos públicos e pontos de extensão
- `engine` — lógica interna e orquestração da execução
- `result` — objetos de resultado retornados ao consumidor
- `report` — abstrações e implementações de geração de relatório

Essa separação mantém a API pública menor e isola os detalhes internos do motor.

## Estratégia de acesso a dados

Uma decisão central da arquitetura é o consumo em estilo streaming.

O objetivo é evitar:

- carregar todo o resultado em memória
- acumular estado ORM por muito tempo
- aumentar excessivamente o contexto de persistência em jobs grandes

Isso torna o motor mais apropriado para cenários com centenas de milhares ou milhões de registros.

## Modelo de execução em chunks

Os registros não são processados todos de uma vez. Eles são acumulados em chunks e executados em lote.

Esse desenho busca um equilíbrio prático entre:

- throughput
- estabilidade de memória
- tamanho da transação
- previsibilidade operacional

### Trade-offs do tamanho do chunk

O tamanho do chunk é um dos parâmetros mais importantes do motor.

- chunks menores reduzem custo de rollback e duração da transação
- chunks maiores podem aumentar throughput
- chunks muito grandes elevam pressão transacional e impacto de falhas

Por isso, o tamanho ideal deve ser ajustado conforme a carga e o comportamento do banco.

## Tratamento de erro e resiliência

O motor suporta tratamento controlado de falhas durante a execução batch.

Dependendo da configuração, o processamento pode:

- falhar imediatamente
- continuar após erro por registro
- tentar novamente falhas transitórias antes de desistir

Isso permite atender desde jobs estritos até cenários mais tolerantes a falhas.

## Fronteiras transacionais

O controle de transação é parte explícita da arquitetura.

O modelo de execução permite aplicar transação de forma consistente em torno do chunk, deixando o comportamento mais observável e previsível.

Isso é importante principalmente para:

- atualizações em grande volume
- jobs longos
- workloads com falhas parciais
- estratégias de recuperação operacional

## Comportamento de memória

Uma preocupação central do projeto é manter o uso de memória estável em execuções prolongadas.

O motor evita o padrão clássico de falha batch em que o consumo cresce continuamente à medida que mais registros são processados.

O comportamento desejado é:

- leitura progressiva
- acúmulo limitado em memória
- liberação recorrente de referências entre chunks

## Pontos de extensão

A arquitetura inclui pontos de extensão para customização, como:

- callbacks de processamento
- handlers de erro por registro
- listeners de chunk
- política de retry
- parametrização de query
- `rowMapper` para queries nativas

Esses pontos permitem customização sem alterar o núcleo do motor.

## Para que esta arquitetura foi otimizada

Este desenho é otimizado para:

- jobs batch baseados em banco de dados
- migração e reprocessamento
- pipelines offline
- execução agendada em background

## Para que esta arquitetura não foi otimizada

Este desenho não foi pensado para:

- fluxos request/response
- endpoints transacionais de baixa latência
- tarefas pequenas em memória
- workloads altamente interativos

## Documentação relacionada

Para mais detalhes, veja:

- [Organização dos pacotes](package-organization.md)
- [Fluxo de processamento](processing-flow.md)
