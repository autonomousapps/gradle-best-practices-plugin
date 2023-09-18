package com.autonomousapps.logging

/** Used by [com.autonomousapps.GradleBestPracticesExtension] and tasks. */
@Suppress("EnumEntryName") // improved Gradle DSL support
enum class LogLevel {
  default,
  reporting,
  debug
}
