# Package Organization

This document explains how the project is organized at the package level and what responsibility belongs to each part of the codebase.

The goal of this structure is to keep the public API small, isolate the execution internals, and make the project easier to evolve over time.

## Overview

The codebase is organized into a few main package groups:

- `config`
- `contract`
- `engine`
- `result`
- `example`

Each package has a clear role in the architecture.

## `config`

This package contains configuration types that define how processing behaves.

Typical responsibilities in this package include:

- transaction behavior configuration
- error handling strategy configuration
- retry policy definition

Examples of concepts that belong here:

- transaction mode
- error strategy
- retry strategy

### What should go here

Types that describe execution options and processing policies.

### What should not go here

- execution logic
- database access code
- orchestration code
- result objects

## `contract`

This package contains the public contracts used by consumers of the library and by the internal engine to interact through stable abstractions.

Typical responsibilities in this package include:

- processing interfaces
- callback contracts
- extension points
- mapping contracts for custom result handling

Examples of concepts that belong here:

- processor contract
- chunk listener
- record-level error handler
- row mapper
- transaction abstraction

### What should go here

Interfaces and callback types that define how external code interacts with the engine.

### What should not go here

- concrete engine implementations
- internal orchestration logic
- runtime metrics aggregation
- package-private processing details

## `engine`

This package contains the internal implementation of the batch processing engine.

It is the core of the project and is responsible for coordinating execution.

Typical responsibilities in this package include:

- processor implementation
- streaming iteration
- chunk formation and execution
- transaction coordination
- internal error propagation
- execution progress logging

Examples of concepts that belong here:

- processing entry point implementation
- processing loop
- metrics logger
- chunk execution result
- engine-specific exception types

### What should go here

Concrete classes that implement the processing workflow and execution mechanics.

### What should not go here

- user-facing documentation examples
- generic configuration enums
- external-facing API-only contracts
- final result DTOs meant for consumers

## `result`

This package contains the objects returned after processing finishes.

Its role is to expose stable output structures to callers without leaking internal engine details.

Typical responsibilities in this package include:

- final execution summaries
- aggregate counters
- execution timing information

### What should go here

Result objects intended to be consumed by application code after job execution completes.

### What should not go here

- intermediate engine state
- processing loop internals
- transaction implementation details
- logging infrastructure

## `example`

This package contains demonstration code intended to show how the library can be used.

Its purpose is educational rather than architectural.

Typical responsibilities in this package include:

- sample entities
- sample jobs
- usage demonstrations

### What should go here

Minimal examples that help developers understand how to integrate the processor.

### What should not go here

- production engine logic
- reusable framework internals
- core configuration types

## Public API vs internal implementation

A useful way to think about the package structure is:

### Public-facing packages

These are the packages most relevant to users of the library:

- `config`
- `contract`
- `result`

These packages define how callers configure and consume the processor.

### Internal implementation package

This package contains the processing internals:

- `engine`

This separation helps reduce accidental coupling between library consumers and implementation details.

## Dependency direction

The intended dependency flow is conceptually:

Application code 
↓ 
contract / config 
↓ 
engine 
↓ 
result


More practically:

- application code configures processing through public contracts
- the engine executes the workflow
- the final result is returned as a stable output object

The internal engine should depend on public contracts where appropriate, but external code should avoid depending directly on internal engine details unless explicitly intended by the project design.

## Design benefits of this structure

This package organization provides several advantages:

- clearer separation of responsibilities
- easier onboarding for contributors
- safer evolution of implementation internals
- reduced coupling between API and engine code
- better maintainability as the project grows

## Maintenance guidelines

When adding new code, use the following rules:

- put policy and behavior definitions in `config`
- put public abstractions and extension points in `contract`
- put processing internals and orchestration in `engine`
- put final externally consumed outputs in `result`
- put demonstration code in `example`

If a class does more than one of these things, it may need to be split.

## Related documentation

For more context, see:

- [Architecture overview](architecture-overview.md)
- [Processing flow](processing-flow.md)