package com.autonomousapps.issue

import com.autonomousapps.internal.graphs.MethodNode

/**
 * Code path to problematic method call. Never empty. See also [Issue].
 */
data class Trace(
  /** Code path to problematic method call. Never empty. See also [Issue]. */
  val trace: List<MethodNode>
) {

  init {
    check(trace.size > 1) { "Trace must have at least two elements. Was $trace" }
  }
}
