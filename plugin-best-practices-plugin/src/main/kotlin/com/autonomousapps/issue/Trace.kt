package com.autonomousapps.issue

import com.autonomousapps.internal.utils.dotty

/**
 * Code path to problematic method call. Never empty. See also [Issue].
 */
data class Trace(
  /** Code path to problematic method call. Never empty. See also [Issue]. */
  val trace: List<String>
) {

  init {
    check(trace.isNotEmpty()) { "Cannot have an empty trace" }
  }

  fun string() = trace.joinToString(separator = " -> ") { it.dotty() }
}
