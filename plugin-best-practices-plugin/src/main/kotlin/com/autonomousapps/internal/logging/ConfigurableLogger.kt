package com.autonomousapps.internal.logging

import org.gradle.api.logging.Logger

class ConfigurableLogger(
  private val delegate: Logger,
  private val level: Level = Level.NORMAL
) : Logger by delegate {

  enum class Level {
    NORMAL,
    REPORTING,
    DEBUG;

    companion object {
      fun of(level: String) = when (level) {
        "reporting" -> REPORTING
        "debug" -> DEBUG
        else -> NORMAL
      }
    }
  }

  fun report(msg: String) {
    if (level == Level.REPORTING || level == Level.DEBUG) {
      delegate.quiet(msg)
    } else {
      delegate.debug(msg)
    }
  }

  override fun debug(msg: String) {
    if (level == Level.DEBUG) {
      delegate.quiet(msg)
    } else {
      delegate.debug(msg)
    }
  }
}