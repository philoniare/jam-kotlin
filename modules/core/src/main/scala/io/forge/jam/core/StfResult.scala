package io.forge.jam.core

import codec.{JamEncoder, JamDecoder, encode, decodeAs}

/**
 * Type alias for State Transition Function results using standard Either.
 *
 * StfResult[Ok, Err] = Either[Err, Ok]
 * - Right(data) represents success
 * - Left(error) represents failure
 *
 * Binary format (for codec):
 * - 1 byte discriminator (0 = success with Ok data, 1 = error with Err code)
 * - Followed by either Ok data (if discriminator = 0) or Err code (if discriminator = 1)
 */
type StfResult[+Ok, +Err] = Either[Err, Ok]

object StfResult:
  /** Create a successful result */
  def success[Ok, Err](data: Ok): StfResult[Ok, Err] = Right(data)

  /** Create an error result */
  def error[Ok, Err](code: Err): StfResult[Ok, Err] = Left(code)

  /** Check if result is successful */
  extension [Ok, Err](result: StfResult[Ok, Err])
    def isOk: Boolean = result.isRight
    def isErr: Boolean = result.isLeft

  /**
   * Unit codec for STF outputs that have no success data (e.g., PreimageOutput).
   * Encodes to zero bytes, decodes consuming zero bytes.
   */
  given JamEncoder[Unit] with
    def encode(a: Unit): JamBytes = JamBytes.empty

  given JamDecoder[Unit] with
    def decode(bytes: JamBytes, offset: Int): (Unit, Int) = ((), 0)

  /**
   * Generic codec for StfResult types (Either[Err, Ok]).
   *
   * Requires encoders/decoders for both Ok and Err types.
   * Binary format matches the existing JAM protocol specification.
   */
  given stfResultEncoder[Ok: JamEncoder, Err: JamEncoder]: JamEncoder[StfResult[Ok, Err]] =
    new JamEncoder[StfResult[Ok, Err]]:
      def encode(a: StfResult[Ok, Err]): JamBytes =
        val builder = JamBytes.newBuilder
        a match
          case Right(data) =>
            builder += 0.toByte // Success discriminator
            builder ++= data.encode
          case Left(err) =>
            builder += 1.toByte // Error discriminator
            builder ++= err.encode
        builder.result()

  /**
   * Generic decoder for StfResult types (Either[Err, Ok]).
   *
   * Reads the discriminator byte and delegates to the appropriate decoder.
   */
  given stfResultDecoder[Ok: JamDecoder, Err: JamDecoder]: JamDecoder[StfResult[Ok, Err]] =
    new JamDecoder[StfResult[Ok, Err]]:
      def decode(bytes: JamBytes, offset: Int): (StfResult[Ok, Err], Int) =
        val discriminator = bytes.toArray(offset).toInt & 0xff
        if discriminator == 0 then
          // Success case
          val (ok, consumed) = bytes.decodeAs[Ok](offset + 1)
          (Right(ok), 1 + consumed)
        else
          // Error case
          val (err, consumed) = bytes.decodeAs[Err](offset + 1)
          (Left(err), 1 + consumed)
