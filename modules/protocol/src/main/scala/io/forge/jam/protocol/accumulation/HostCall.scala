package io.forge.jam.protocol.accumulation

/**
 * Host call identifiers for accumulation.
 */
object HostCall:

  // ===========================================================================
  // General Host Calls (0-13)
  // ===========================================================================

  /** gas (0): Returns remaining gas in register r7 */
  val GAS: Int = 0

  /** fetch (1): Fetch various data based on selector */
  val FETCH: Int = 1

  /** lookup (2): Look up preimage by hash */
  val LOOKUP: Int = 2

  /** read (3): Read from service storage */
  val READ: Int = 3

  /** write (4): Write to service storage */
  val WRITE: Int = 4

  /** info (5): Get service account info (96 bytes) */
  val INFO: Int = 5

  // ===========================================================================
  // Refine-Only Host Calls (6-13)
  // ===========================================================================

  /** historical_lookup (6): Look up historical preimage data */
  val HISTORICAL_LOOKUP: Int = 6

  /** export (7): Export a segment to the work report output */
  val EXPORT: Int = 7

  /** machine (8): Create a new inner PVM instance */
  val MACHINE: Int = 8

  /** peek (9): Read memory from an inner PVM instance */
  val PEEK: Int = 9

  /** poke (10): Write memory to an inner PVM instance */
  val POKE: Int = 10

  /** pages (11): Modify page access rights of an inner PVM instance */
  val PAGES: Int = 11

  /** invoke (12): Execute an inner PVM instance */
  val INVOKE: Int = 12

  /** expunge (13): Remove an inner PVM instance */
  val EXPUNGE: Int = 13

  // ===========================================================================
  // Accumulate-Specific Host Calls (14-26)
  // ===========================================================================

  /** bless (14): Set privileged services (manager, assigners, delegator, registrar, always-acc) */
  val BLESS: Int = 14

  /** assign (15): Set core assigner and authorization queue (privileged) */
  val ASSIGN: Int = 15

  /** designate (16): Set validator queue (privileged) */
  val DESIGNATE: Int = 16

  /** checkpoint (17): Save current state x to checkpoint y */
  val CHECKPOINT: Int = 17

  /** new (18): Create new service account */
  val NEW: Int = 18

  /** upgrade (19): Upgrade service code hash */
  val UPGRADE: Int = 19

  /** transfer (20): Queue a deferred transfer */
  val TRANSFER: Int = 20

  /** eject (21): Eject (remove) another service account */
  val EJECT: Int = 21

  /** query (22): Query preimage request status */
  val QUERY: Int = 22

  /** solicit (23): Request a preimage */
  val SOLICIT: Int = 23

  /** forget (24): Forget a preimage request */
  val FORGET: Int = 24

  /** yield (25): Set accumulation output hash */
  val YIELD: Int = 25

  /** provide (26): Provide a preimage for another service */
  val PROVIDE: Int = 26

  // ===========================================================================
  // Debug Host Call
  // ===========================================================================

  /** log (100): Debug logging (JIP-1), gas cost = 0 */
  val LOG: Int = 100

  /** Get human-readable name for host call ID */
  def name(id: Int): String = id match
    case GAS => "GAS"
    case FETCH => "FETCH"
    case LOOKUP => "LOOKUP"
    case READ => "READ"
    case WRITE => "WRITE"
    case INFO => "INFO"
    case HISTORICAL_LOOKUP => "HISTORICAL_LOOKUP"
    case EXPORT => "EXPORT"
    case MACHINE => "MACHINE"
    case PEEK => "PEEK"
    case POKE => "POKE"
    case PAGES => "PAGES"
    case INVOKE => "INVOKE"
    case EXPUNGE => "EXPUNGE"
    case BLESS => "BLESS"
    case ASSIGN => "ASSIGN"
    case DESIGNATE => "DESIGNATE"
    case CHECKPOINT => "CHECKPOINT"
    case NEW => "NEW"
    case UPGRADE => "UPGRADE"
    case TRANSFER => "TRANSFER"
    case EJECT => "EJECT"
    case QUERY => "QUERY"
    case SOLICIT => "SOLICIT"
    case FORGET => "FORGET"
    case YIELD => "YIELD"
    case PROVIDE => "PROVIDE"
    case LOG => "LOG"
    case _ => s"UNKNOWN($id)"
