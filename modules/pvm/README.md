# jam-pvm

PolkaVM virtual machine implementation for executing JAM work packages.

## Overview

The `jam-pvm` module implements PolkaVM, a RISC-V based virtual machine for executing work packages in the JAM protocol. PolkaVM provides a sandboxed, gas-metered execution environment for service code.

**Package:** `io.forge.jam.pvm`

## Key Features

- **RISC-V Instruction Set**: Full RV32I/RV64I support
- **Type-Safe Instructions**: Scala 3 GADTs for width-polymorphic operations
- **Memory Model**: Page-based memory with access control
- **Gas Metering**: Resource accounting for execution
- **Host Calls**: Service interaction interface
- **Program Loading**: Bytecode parsing and validation
