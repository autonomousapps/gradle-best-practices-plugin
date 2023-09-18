package com.autonomousapps.internal.logging

import com.autonomousapps.logging.LogLevel
import org.gradle.api.logging.Logger

internal class ConfigurableLogger(
  private val delegate: Logger,
  private val level: LogLevel = LogLevel.default
) : Logger by delegate {

  fun report(msg: String) {
    if (level == LogLevel.reporting || level == LogLevel.debug) {
      delegate.quiet(msg)
    } else {
      delegate.debug(msg)
    }
  }

  override fun debug(msg: String) {
    if (level == LogLevel.debug) {
      delegate.quiet(msg)
    } else {
      delegate.debug(msg)
    }
  }
}
