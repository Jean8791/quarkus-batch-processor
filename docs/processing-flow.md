# Processing Flow

This document describes how a batch job is executed from configuration to final result.

The goal is to explain the runtime behavior of the engine in a step-by-step way.

## Overview

A processing job follows this high-level flow:

Application code ↓ Processor configuration ↓ run() ↓ Query execution ↓ Streaming iteration ↓ Chunk formation ↓ Chunk processing ↓ Error handling and retries ↓ Metrics and final result


The execution model is sequential and chunk-oriented.

## Step 1 — Job configuration

The process begins when application code configures a processor instance through the fluent API.

Typical configuration includes:

- query definition
- native query definition
- row mapping for native query scenarios
- query parameters
- chunk size
- transaction mode
- processing callback
- error strategy
- retry strategy
- chunk completion listener
- logging behavior
- processor name

At this stage, the job is only being described. No records are processed yet.

## Step 2 — Execution starts with `run()`

The actual processing begins when `run()` is called.

At this point, the engine:

- validates the configured options
- resolves default values where necessary
- prepares the execution context
- initializes logging and metrics collection

If required configuration is missing, execution stops before any database work begins.

## Step 3 — Query preparation

After validation, the engine prepares the data source for iteration.

Depending on configuration, it may execute:

- an HQL query
- a native SQL query

If native query mode is used, row mapping is applied so raw database rows can be transformed into the target processing type.

Query parameters are applied before iteration begins.

## Step 4 — Streaming record iteration

Once the query is ready, the engine starts reading records progressively instead of materializing the entire result set in memory.

This stage is responsible for:

- opening the iteration source
- reading records one by one
- exposing records through an iterator abstraction

This is a key part of the engine’s memory behavior, since only a limited working set is kept in memory at any time.

## Step 5 — Chunk formation

As records are read, they are accumulated into an in-memory buffer until the configured chunk size is reached.

For example:

- if chunk size is `1000`, the engine collects up to 1000 records
- once the chunk is full, it is sent for processing
- after processing, a new chunk buffer is created
- if the input ends before the chunk is full, the final partial chunk is still processed

This model keeps memory bounded while still allowing efficient batch execution.

## Step 6 — Chunk processing

When a chunk is ready, the engine delegates it to the chunk processor.

At this stage:

- each record in the chunk is passed to the configured processing callback
- counters such as processed items, errors, and retries are updated
- chunk-level completion hooks may be triggered after processing

Chunk processing is the stage where the user’s business logic is actually executed.

## Step 7 — Retry behavior

If a record processing attempt fails, the engine may retry it depending on the configured retry policy.

Typical retry behavior includes:

- evaluating whether the exception is retryable
- limiting retry attempts
- waiting between attempts if a delay is configured

If processing succeeds after a retry, execution continues normally and retry counters are updated.

If retry conditions are exhausted, the failure is handled according to the configured error strategy.

## Step 8 — Error handling

When a record cannot be processed successfully, the engine applies the configured error strategy.

Typical outcomes include:

- stopping execution immediately
- continuing with the next record
- notifying a record-level error handler

This makes it possible to support both strict jobs and tolerant jobs.

Examples:

- migration jobs may choose to continue and log failures
- financial or critical reconciliation jobs may choose to fail fast

## Step 9 — Transaction handling

Chunk execution may be wrapped in a transaction depending on the configured transaction mode.

Conceptually, this means the engine can:

- process records without transaction wrapping
- process a full chunk under a single transaction strategy

Transaction handling is applied consistently around chunk execution so the behavior remains explicit and predictable.

This is especially important for jobs that update database state.

## Step 10 — Reference release and chunk completion

After a chunk is processed, the engine performs post-chunk housekeeping.

This usually includes:

- releasing references associated with the just-processed chunk
- notifying chunk completion listeners
- updating internal progress observation
- logging progress when enabled

This step helps keep long-running jobs stable over time.

## Step 11 — Final chunk and end-of-input behavior

When the iteration reaches the end of the result stream:

- any remaining buffered records are processed as the final partial chunk
- if no records were read, the engine still completes cleanly
- final counters are consolidated into a single execution result

This ensures that the engine behaves correctly for:

- full datasets
- partial final chunks
- empty result sets

## Step 12 — Final result

At the end of execution, the engine returns a final result object summarizing the run.

Typical result information includes:

- total processed records
- total errors
- total retries
- execution start time
- execution end time
- total duration

This result can be used by the caller for:

- operational logging
- monitoring
- alerting
- reporting
- job orchestration

## Flow summary

The complete runtime flow can be summarized as:

Configure processor 
↓ 
Call run() 
↓ 
Validate configuration 
↓ 
Prepare query 
↓ 
Read records as a stream 
↓ 
Accumulate chunk 
↓ 
Process each record 
↓ 
Apply retry and error handling 
↓ 
Finalize chunk 
↓ 
Repeat until input ends 
↓ 
Return final ProcessingResult


## Operational characteristics

This flow has a few important runtime properties:

- execution is sequential
- memory usage remains bounded by streaming and chunk buffering
- chunk size strongly affects throughput and transaction cost
- retry and error policies directly affect resilience behavior
- progress logging can be enabled for long-running workloads

## Related documentation

For more context, see:

- [Architecture overview](architecture-overview.md)
- [Package organization](package-organization.md)