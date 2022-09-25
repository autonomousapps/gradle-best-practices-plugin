package com.autonomousapps.issue

import com.autonomousapps.internal.graphs.MethodNode
import com.autonomousapps.internal.utils.dotty

internal object IssueRenderer {

  fun renderIssue(issue: Issue, pretty: Boolean = false): String {
    return if (pretty) {
      renderTracePretty(issue.trace)
    } else {
      renderTrace(issue.trace)
    }
  }

  private fun renderTracePretty(trace: Trace) = buildString {
    val nodes = trace.trace
    append(renderMethodNode(nodes.first()))
    appendLine(" ->")

    for (i in 1 until nodes.size) {
      append("  ")
      append(renderMethodNode(nodes[i]))
      if (i < nodes.size - 1) {
        appendLine(" ->")
      }
    }
  }

  private fun renderTrace(trace: Trace): String {
    return trace.trace.joinToString(separator = " -> ") { renderMethodNode(it) }
  }

  private fun renderMethodNode(node: MethodNode) = buildString {
    with(node) {
      append(owner)
      append("#")
      append(name)
      append(descriptor)
    }
  }.dotty()
}