# jam-crypto

Cryptographic operations for the JAM protocol implementation.

## Overview

The `jam-crypto` module provides cryptographic primitives required by the JAM protocol, including Bandersnatch VRF signatures, Ed25519 signatures, erasure coding, and hashing. Most cryptographic operations delegate to native Rust libraries via JNI for correctness and performance.

**Package:** `io.forge.jam.crypto`

## Key Features

- **Bandersnatch Ring VRF**: Ring VRF signatures for Safrole block production
- **Ed25519 Signatures**: Validator signatures with batch verification
- **Erasure Coding**: Reed-Solomon encoding for data availability
- **Hashing**: Blake2b and Keccak implementations

## Module Structure

```
io.forge.jam.crypto/
├── Bandersnatch.scala       # Bandersnatch VRF operations
├── Ed25519.scala            # Ed25519 signature operations
├── ErasureCoding.scala      # Erasure coding operations
├── Hashing.scala            # Hash functions
└── NativeLibraryLoader.scala
```

### Native Libraries

```
modules/crypto/native/
├── bandersnatch-vrfs-wrapper/    # Rust wrapper for bandersnatch-vrfs
├── ed25519-zebra-wrapper/        # Rust wrapper for ed25519-zebra
├── erasure-coding-wrapper/       # Rust wrapper for Reed-Solomon
└── build/{mac,linux,windows}/    # Built libraries
```