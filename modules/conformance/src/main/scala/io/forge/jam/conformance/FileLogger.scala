package io.forge.jam.conformance

import cats.effect.{IO, Resource}
import java.io.{BufferedWriter, FileWriter, PrintWriter, StringWriter}
import java.nio.file.Path
import java.time.{Instant, Duration}
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

/**
 * Simple append-only file logger for conformance testing sessions.
 */
class FileLogger(
  private[conformance] val writer: BufferedWriter,
  sessionStart: AtomicReference[Instant],
  errorCount: AtomicLong
):
  private val timestampFormat = DateTimeFormatter.ISO_INSTANT

  /**
   * Log a message with timestamp, direction, type, size, and key fields.
   */
  def log(direction: String, msgType: String, size: Int, keyFields: String = ""): IO[Unit] =
    IO.blocking {
      val timestamp = timestampFormat.format(Instant.now())
      val fields = if keyFields.nonEmpty then s" [$keyFields]" else ""
      writer.write(s"[$timestamp] [$direction] $msgType (${size}b)$fields\n")
      writer.flush()
    }

  /**
   * Log a received message.
   */
  def logReceived(msgType: String, size: Int, keyFields: String = ""): IO[Unit] =
    log("RX", msgType, size, keyFields)

  /**
   * Log a sent message.
   */
  def logSent(msgType: String, size: Int, keyFields: String = ""): IO[Unit] =
    log("TX", msgType, size, keyFields)

  /**
   * Log session start.
   */
  def logSessionStart(): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      sessionStart.set(now)
      errorCount.set(0)
      val timestamp = timestampFormat.format(now)
      writer.write(s"[$timestamp] [SESSION] Connected\n")
      writer.flush()
    }

  /**
   * Log session end with duration and error counts.
   */
  def logSessionEnd(): IO[Unit] =
    IO.blocking {
      val now = Instant.now()
      val start = sessionStart.get()
      val duration = if start != null then Duration.between(start, now) else Duration.ZERO
      val errors = errorCount.get()
      val timestamp = timestampFormat.format(now)
      writer.write(s"[$timestamp] [SESSION] Disconnected (duration=${duration.toMillis}ms, errors=$errors)\n")
      writer.flush()
    }

  /**
   * Log an error with full stack trace.
   */
  def logError(message: String, error: Throwable): IO[Unit] =
    IO.blocking {
      errorCount.incrementAndGet()
      val timestamp = timestampFormat.format(Instant.now())
      val sw = new StringWriter()
      error.printStackTrace(new PrintWriter(sw))
      writer.write(s"[$timestamp] [ERROR] $message\n$sw\n")
      writer.flush()
    }

  /**
   * Log a warning message.
   */
  def logWarning(message: String): IO[Unit] =
    IO.blocking {
      val timestamp = timestampFormat.format(Instant.now())
      writer.write(s"[$timestamp] [WARN] $message\n")
      writer.flush()
    }

  /**
   * Log an info message.
   */
  def logInfo(message: String): IO[Unit] =
    IO.blocking {
      val timestamp = timestampFormat.format(Instant.now())
      writer.write(s"[$timestamp] [INFO] $message\n")
      writer.flush()
    }

  /**
   * Close the writer.
   */
  def close(): Unit = writer.close()

object FileLogger:
  /**
   * Create a FileLogger resource that manages the file lifecycle.
   */
  def resource(logPath: Path): Resource[IO, FileLogger] =
    Resource.make(
      IO.blocking {
        val writer = new BufferedWriter(new FileWriter(logPath.toFile, true))
        new FileLogger(
          writer,
          new AtomicReference[Instant](),
          new AtomicLong(0)
        )
      }
    ) { logger =>
      IO.blocking {
        logger.close()
      }
    }

  /**
   * Create a no-op logger for testing.
   */
  def noop: FileLogger =
    new FileLogger(
      new BufferedWriter(new StringWriter()),
      new AtomicReference[Instant](),
      new AtomicLong(0)
    )
