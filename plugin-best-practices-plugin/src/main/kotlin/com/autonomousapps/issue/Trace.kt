package com.autonomousapps.issue

import com.autonomousapps.internal.graphs.MethodNode
import kotlinx.serialization.Serializable

/**
 * Code path to problematic method call. Never empty. See also [Issue].
 */
@Serializable
data class Trace(
  /** Code path to problematic method call. Never empty. See also [Issue]. */
  val trace: List<MethodNode>
) {

  init {
    check(trace.size > 1) { "Trace must have at least two elements. Was $trace" }
  }
}
