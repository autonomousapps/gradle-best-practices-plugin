package com.autonomousapps.issue

class Trace(
  /** Code path to problematic method call. Never empty. See also [Issue]. */
  val trace: List<String>
) {

  init {
    check(trace.isNotEmpty()) { "Cannot have an empty trace" }
  }

  fun string() = trace.joinToString(separator = " -> ")
  fun head() = trace.first()
  fun tail() = trace.last()
}
