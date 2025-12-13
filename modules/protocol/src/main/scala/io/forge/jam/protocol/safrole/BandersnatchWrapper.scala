package io.forge.jam.protocol.safrole

import io.forge.jam.core.JamBytes
import io.forge.jam.core.primitives.{BandersnatchPublicKey, Hash}
import io.forge.jam.core.types.tickets.TicketMark
import io.forge.jam.crypto.BandersnatchVrf
import spire.math.UByte

/**
 * Protocol-level wrapper for Bandersnatch VRF operations.
 */
object BandersnatchWrapper:

  /** Size of the ring commitment in bytes */
  val RingCommitmentSize: Int = BandersnatchVrf.RingCommitmentSize

  /** Size of a Bandersnatch public key in bytes */
  val PublicKeySize: Int = BandersnatchVrf.PublicKeySize

  /** Size of a ring VRF signature in bytes */
  val RingVrfSignatureSize: Int = BandersnatchVrf.RingVrfSignatureSize

  /**
   * Generate the ring root (commitment) from a list of Bandersnatch public keys.
   *
   * @param keys List of Bandersnatch public keys (32 bytes each)
   * @param ringSize The size of the ring (should match keys.size for full ring)
   * @return The 144-byte ring commitment, or None if generation fails
   */
  def generateRingRoot(
    keys: List[BandersnatchPublicKey],
    ringSize: Int
  ): Option[JamBytes] = BandersnatchVrf.generateRingRoot(keys, ringSize)

  /**
   * Verify a ring VRF proof and extract the ticket ID if valid.
   *
   * @param signature The 784-byte ring VRF signature from the ticket envelope
   * @param ringCommitment The 144-byte ring commitment (gamma_z)
   * @param entropy The 32-byte entropy value (eta[2] - tickets entropy)
   * @param attempt The ticket attempt index (0-255)
   * @param ringSize The ring size for verification
   * @return Some(TicketMark) with the ticket ID and attempt if verification succeeds, None otherwise
   */
  def verifyRingProof(
    signature: JamBytes,
    ringCommitment: JamBytes,
    entropy: Hash,
    attempt: UByte,
    ringSize: Int
  ): Option[TicketMark] =
    BandersnatchVrf.verifyRingProof(signature, ringCommitment, entropy, attempt, ringSize)
      .map(result => TicketMark(result.ticketId, result.attempt))

  /**
   * Alternative verification method that takes raw byte arrays.
   * Useful for testing and direct integration.
   */
  def verifyRingProofRaw(
    entropy: Array[Byte],
    attempt: Int,
    signature: Array[Byte],
    commitment: Array[Byte],
    ringSize: Int
  ): Option[Array[Byte]] = BandersnatchVrf.verifyRingProofRaw(entropy, attempt, signature, commitment, ringSize)

  /**
   * Check if the native library is loaded and operational.
   */
  def isAvailable: Boolean = BandersnatchVrf.isAvailable
