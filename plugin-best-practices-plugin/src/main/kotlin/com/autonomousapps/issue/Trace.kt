package com.autonomousapps.issue

import com.autonomousapps.internal.graph.MethodNode
import com.autonomousapps.internal.utils.dotty

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

  fun string() = buildString {
    append(trace.first().string())
    appendLine(" ->")
    trace.drop(1).forEach {
      append("  ")
      append(it.string())
      appendLine(" ->")
    }
  }

//  fun string(): String {
//    return trace.joinToString(separator = " -> ") { it.string() }
//  }
}
