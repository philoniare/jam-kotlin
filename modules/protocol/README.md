# jam-protocol

State transition functions and block pipeline for the JAM protocol implementation.

## Overview

The `jam-protocol` module implements the state transition functions (STFs) that define how the JAM blockchain evolves. Each STF handles a specific aspect of the protocol, from block production (Safrole) to work package execution (Accumulation).

**Package:** `io.forge.jam.protocol`

## Key Features

- **State Transition Functions**: All JAM STFs from the Gray Paper
- **Block Import Pipeline**: End-to-end block validation and execution
- **State Management**: Complete JAM state representation