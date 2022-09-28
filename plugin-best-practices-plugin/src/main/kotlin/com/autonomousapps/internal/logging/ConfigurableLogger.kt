package com.autonomousapps.internal.logging

import org.gradle.api.logging.Logger

class ConfigurableLogger(
  private val delegate: Logger,
  private val level: Level = Level.default
) : Logger by delegate {

  @Suppress("EnumEntryName") // improved Gradle DSL support
  enum class Level {
    default,
    reporting,
    debug
  }

  fun report(msg: String) {
    if (level == Level.reporting || level == Level.debug) {
      delegate.quiet(msg)
    } else {
      delegate.debug(msg)
    }
  }

  override fun debug(msg: String) {
    if (level == Level.debug) {
      delegate.quiet(msg)
    } else {
      delegate.debug(msg)
    }
  }
}