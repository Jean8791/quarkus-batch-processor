# Architecture Overview

`quarkus-batch-processor` is a batch processing engine designed for large-scale relational data workloads in Quarkus applications.

Its architecture is focused on predictable memory usage, explicit processing flow, and controlled transaction boundaries.

## Architectural goals

The engine was designed around the following goals:

- process very large datasets safely
- avoid loading full result sets into memory
- keep transaction scope explicit and predictable
- support resilient record processing with configurable error handling
- provide a simple API while keeping execution internals isolated

## Core architectural idea

Instead of materializing an entire query result in memory, the engine processes records as a stream.

At a high level, the engine:

1. executes a query against the database
2. reads the result progressively
3. groups records into chunks
4. processes each chunk according to the configured strategy
5. emits execution metrics and a final result summary

This model is appropriate for long-running workloads where throughput and memory stability are more important than request/response latency.

## High-level processing pipeline

Database 
↓ 
Streaming query results 
↓ 
Iterator abstraction 
↓ 
ProcessingLoop 
↓ 
Chunk formation 
↓ 
Chunk execution 
↓ 
Metrics and final result


Each stage has a narrow responsibility, which helps keep the implementation easier to reason about and evolve.

## Main architectural components

### FastRecordProcessor

The main entry point for consumers of the library.

Its responsibility is to expose the fluent API used to configure a batch job.

### FastRecordProcessorImpl

The internal coordinator of batch execution.

It is responsible for:

- validating processing configuration
- creating the result iterator
- selecting the execution strategy
- coordinating chunk processing
- producing the final processing result

### ProcessingLoop

The core loop of the engine.

It is responsible for:

- iterating over streamed records
- grouping records into chunks
- invoking the chunk processor
- triggering chunk completion notifications
- releasing references between chunk executions

### ProcessingMetricsLogger

Encapsulates execution logging.

It is responsible for logging:

- start of execution
- chunk progress
- completion summary
- failure information

### ChunkExecutionResult

Represents the outcome of a single processed chunk, including counters such as processed items, errors, and retries.

### ProcessingResult

Represents the final summary of the batch execution.

It is the main result object returned to the caller after the job finishes.

## Package-level organization

The architecture is divided into a few main areas:

- `config` — configuration types that define execution behavior
- `contract` — public contracts and extension points used by consumers
- `engine` — internal orchestration and processing logic
- `result` — execution result objects returned by the engine

This separation helps keep the public API small while isolating execution mechanics inside the engine layer.

## Data access strategy

A central architectural decision is the use of streaming-style result consumption.

The goal is to avoid:

- loading entire result sets into memory
- long-lived ORM state accumulation
- excessive persistence context growth during large jobs

This makes the engine more suitable for workloads involving hundreds of thousands or millions of rows.

## Chunk-based execution model

Records are not processed all at once. They are accumulated into chunks and executed in batches.

This design provides a practical balance between:

- throughput
- memory stability
- transaction size
- operational predictability

### Trade-offs of chunk size

Chunk size is one of the most important tuning parameters in the architecture.

- Smaller chunks reduce rollback cost and transaction duration
- Larger chunks may improve throughput
- Very large chunks can increase transaction pressure and failure impact

Because of this, chunk size should be chosen according to workload behavior and database characteristics.

## Error handling and resilience

The engine is designed to support controlled failure handling during batch execution.

Depending on configuration, processing may:

- fail fast
- continue after record-level failures
- retry transient failures before giving up

This allows the same architecture to support both strict and tolerant batch scenarios.

## Transaction boundaries

Transaction control is part of the architecture rather than an incidental detail.

The execution model is designed so that transaction handling can be applied consistently to chunk processing, keeping behavior explicit and easier to observe.

This is important for:

- large updates
- long-running jobs
- workloads with partial failures
- operational recovery strategies

## Memory behavior

A primary concern of the architecture is stable memory usage over long execution windows.

The engine avoids the classic batch failure mode where memory usage grows continuously as more records are processed.

The intended behavior is:

- progressive reading
- bounded in-memory accumulation
- regular release of processing references between chunks

## Extensibility points

The architecture includes extension points intended for customization, such as:

- processing callbacks
- record-level error handlers
- chunk listeners
- retry behavior
- query parameterization
- row mapping for native query scenarios

These extension points allow customization without requiring changes to the execution core.

## What this architecture is optimized for

This design is optimized for:

- large database-driven batch jobs
- migration and reprocessing workloads
- offline processing pipelines
- scheduled background execution

## What this architecture is not optimized for

This design is not intended for:

- request/response application flows
- low-latency transactional endpoints
- small in-memory processing tasks
- highly interactive workloads

## Related documentation

For more details, see:

- [Package organization](package-organization.md)
- [Processing flow](processing-flow.md)