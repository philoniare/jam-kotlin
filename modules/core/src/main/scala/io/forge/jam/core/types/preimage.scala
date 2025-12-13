package io.forge.jam.core.types

import io.forge.jam.core.{JamBytes, codec}
import io.forge.jam.core.codec.{JamEncoder, JamDecoder}
import io.forge.jam.core.primitives.Hash
import io.forge.jam.core.json.JsonHelpers.parseHex
import io.circe.Decoder

/**
 * Preimage-related types shared across STFs.
 */
object preimage:

  /**
   * Preimage hash with blob data.
   * Used by: Preimage STF, Accumulation STF
   *
   * @param hash Hash of the preimage (32 bytes)
   * @param blob The preimage data (variable length)
   */
  final case class PreimageHash(
    hash: Hash,
    blob: JamBytes
  )

  object PreimageHash:
    given JamEncoder[PreimageHash] with
      def encode(a: PreimageHash): JamBytes =
        val builder = JamBytes.newBuilder
        builder ++= a.hash.bytes
        builder ++= codec.encodeCompactInteger(a.blob.length.toLong)
        builder ++= a.blob.toArray
        builder.result()

    given JamDecoder[PreimageHash] with
      def decode(bytes: JamBytes, offset: Int): (PreimageHash, Int) =
        val arr = bytes.toArray
        var pos = offset
        val hash = Hash(arr.slice(pos, pos + Hash.Size))
        pos += Hash.Size
        val (blobLength, blobLengthBytes) = codec.decodeCompactInteger(arr, pos)
        pos += blobLengthBytes
        val blob = JamBytes(arr.slice(pos, pos + blobLength.toInt))
        pos += blobLength.toInt
        (PreimageHash(hash, blob), pos - offset)

    given Decoder[PreimageHash] =
      Decoder.instance { cursor =>
        for
          hash <- cursor.get[String]("hash").map(h => Hash(parseHex(h)))
          blob <- cursor.get[String]("blob").map(b => JamBytes(parseHex(b)))
        yield PreimageHash(hash, blob)
      }
