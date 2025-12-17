# jam-core

Core types, encoding, and primitives for the JAM protocol implementation.

## Overview

The `jam-core` module provides the foundational types and serialization infrastructure for the JAM protocol. This module has no dependencies on other JAM modules and serves as the base layer for the entire implementation.

**Package:** `io.forge.jam.core`

## Key Features

- **Complete JAM Type System**: All protocol types from the Gray Paper
- **Binary Serialization**: JAM-compliant encoding/decoding via type classes
- **Primitive Types**: Hash types, unsigned integers, fixed-size arrays
- **JSON Support**: Test vector parsing and configuration loading
- **Constants**: Protocol constants and configuration values

## Codec System

### scodec Library

The codec system uses **scodec**, a combinator library for working with binary data. scodec provides bidirectional `Codec[A]` instances that handle both encoding and decoding.

```scala
import scodec.Codec
import scodec.codecs._

// Codec handles both directions
trait Codec[A]:
  def encode(value: A): Attempt[BitVector]
  def decode(bits: BitVector): Attempt[DecodeResult[A]]
```

### Custom JAM Codecs

JAM-specific encodings are implemented as custom codecs:

```scala
// Compact integer encoding
val compactCodec: Codec[UInt] = new Codec[UInt]:
  def sizeBound = SizeBound.unknown

  def encode(n: UInt) =
    val bytes = encodeCompact(n)
    Attempt.successful(BitVector(bytes))

  def decode(bits: BitVector) =
    decodeCompact(bits.bytes.toArray) match
      case Right((value, remainder)) =>
        Attempt.successful(DecodeResult(value, BitVector(remainder)))
      case Left(err) =>
        Attempt.failure(Err(err))
```

### Codec Composition

Codecs compose using scodec operators:

```scala
// Product types with :: operator
given Codec[Block] = (
  Codec[Header] ::
  Codec[Extrinsic]
).as[Block]
```

## Primitive Types

### Hash Types

```scala
type Hash = Array[Byte]  // 32 bytes
type HashShort = Array[Byte]  // 28 bytes (Bandersnatch public key)
```

### Unsigned Integers

Uses Spire library:
```scala
import spire.math.{UByte, UShort, UInt, ULong}

val u8: UByte = UByte(255)
val u16: UShort = UShort(65535)
val u32: UInt = UInt(4294967295L)
val u64: ULong = ULong("18446744073709551615")
```

### Fixed-Size Types

```scala
type ServiceId = UInt        // 4 bytes
type CoreIndex = UShort      // 2 bytes
type Gas = ULong             // 8 bytes
type Balance = ULong         // 8 bytes
type TimeSlot = UInt         // 4 bytes
```