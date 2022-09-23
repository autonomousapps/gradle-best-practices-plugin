@file:Suppress("UnstableApiUsage") // Guava Graphs

package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.graph.Class
import com.autonomousapps.internal.graph.Method
import com.autonomousapps.internal.graph.MethodNode
import com.autonomousapps.internal.graph.ShortestPath
import com.autonomousapps.issue.AllprojectsIssue
import com.autonomousapps.issue.GetAllprojectsIssue
import com.autonomousapps.issue.GetProjectInTaskActionIssue
import com.autonomousapps.issue.GetSubprojectsIssue
import com.autonomousapps.issue.Issue
import com.autonomousapps.issue.SubprojectsIssue
import com.autonomousapps.issue.Trace
import com.google.common.collect.MultimapBuilder
import com.google.common.graph.ElementOrder
import com.google.common.graph.GraphBuilder

internal interface IssueListener {

  fun computeIssues(): Set<Issue>

  fun visitClass(name: String, superName: String?, interfaces: List<String>) = Unit
  fun visitMethod(name: String, descriptor: String) = Unit
  fun visitMethodEnd() = Unit
  fun visitMethodInstruction(trace: List<String>, owner: String, name: String, descriptor: String) = Unit
  fun visitMethodAnnotation(trace: List<String>, descriptor: String) = Unit
}

internal class CompositeIssueListener(
  private val listeners: List<IssueListener>,
) : IssueListener {

  override fun computeIssues(): Set<Issue> {
    return listeners.flatMapTo(HashSet()) { it.computeIssues() }
  }

  override fun visitClass(name: String, superName: String?, interfaces: List<String>) {
    listeners.forEach { it.visitClass(name, superName, interfaces) }
  }

  override fun visitMethod(name: String, descriptor: String) {
    listeners.forEach { it.visitMethod(name, descriptor) }
  }

  override fun visitMethodEnd() {
    listeners.forEach { it.visitMethodEnd() }
  }

  override fun visitMethodAnnotation(trace: List<String>, descriptor: String) {
    listeners.forEach { it.visitMethodAnnotation(trace, descriptor) }
  }

  override fun visitMethodInstruction(trace: List<String>, owner: String, name: String, descriptor: String) {
    listeners.forEach { it.visitMethodInstruction(trace, owner, name, descriptor) }
  }
}

internal class AllProjectsListener : IssueListener {

  private val issues = mutableSetOf<Issue>()

  override fun computeIssues(): Set<Issue> = issues

  override fun visitMethodInstruction(trace: List<String>, owner: String, name: String, descriptor: String) {
    val thisTrace = ArrayList(trace).apply {
      add("$owner#$name")
    }

    val issue = if (owner == "org/gradle/api/Project") {
      when (name) {
        "allprojects" -> AllprojectsIssue(name, Trace(thisTrace))
        "getAllprojects" -> GetAllprojectsIssue(name, Trace(thisTrace))
        else -> null
      }
    } else {
      null
    }

    issue?.let { issues.add(it) }
  }
}

internal class SubProjectsListener : IssueListener {

  private val issues = mutableSetOf<Issue>()

  override fun computeIssues(): Set<Issue> = issues

  override fun visitMethodInstruction(trace: List<String>, owner: String, name: String, descriptor: String) {
    val thisTrace = ArrayList(trace).apply {
      add("${owner}#$name")
    }

    val issue = if (owner == "org/gradle/api/Project") {
      when (name) {
        "subprojects" -> SubprojectsIssue(name, Trace(thisTrace))
        "getSubprojects" -> GetSubprojectsIssue(name, Trace(thisTrace))
        else -> null
      }
    } else {
      null
    }

    issue?.let { issues.add(it) }
  }
}

internal class GetProjectListener : IssueListener {

  private var isTaskAction = false

  private lateinit var currentClass: Class
  private var currentMethod: Method? = null

  private val parentPointers = MultimapBuilder.hashKeys().hashSetValues().build<String, String>()
  private val graph = GraphBuilder
    .directed()
    .incidentEdgeOrder<MethodNode>(ElementOrder.stable())
    .allowsSelfLoops(true)
    .build<MethodNode>()

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    GetProjectInTaskActionIssue("getProject", trace)
  }

  override fun visitClass(name: String, superName: String?, interfaces: List<String>) {
    currentClass = Class(name, superName)
    superName?.let { parent ->
      parentPointers.put(name, parent)
    }
    interfaces.forEach { parent ->
      parentPointers.put(name, parent)
    }
  }

  override fun visitMethod(name: String, descriptor: String) {
    currentMethod = Method(name, descriptor)
  }

  override fun visitMethodEnd() {
    currentMethod = null
    isTaskAction = false
  }

  // TODO maybe instead of doing this, we pass some metadata to the visitMethodCall method that contains this boolean
  //  (and potentially others). Then there's less state to maintain here.
  override fun visitMethodAnnotation(trace: List<String>, descriptor: String) {
    isTaskAction = descriptor == "Lorg/gradle/api/tasks/TaskAction;"
  }

  override fun visitMethodInstruction(trace: List<String>, owner: String, name: String, descriptor: String) {
    putEdge(owner, name, descriptor)
  }

  private fun computeTraces(): Set<Trace> {
    hydrateGraph()

    val suspectNodes = graph.nodes().filter(::callsGetProject)
    if (suspectNodes.isEmpty()) return emptySet()

    val taskActions = graph.nodes().filter(::isTaskAction)
    if (taskActions.isEmpty()) return emptySet()

    // We have valid entry points and suspect exit points. Is there a path between any of them?

    return taskActions.flatMapTo(HashSet()) { actionNode ->
      val paths = ShortestPath(graph, actionNode)
      suspectNodes.asSequence()
        .map { paths.pathTo(it) }
        .filter { it.isNotEmpty() }
        .map { methodNodes -> methodNodes.map { it.string() } }
        .map { Trace(it) }
    }
  }

  /**
   * TODO ensure this kdoc is accurate.
   *
   * Hydrate the graph with artificial nodes and edges to account for class hierarchies and the many paths code may take
   * to reach suspect method calls.
   *
   * Overview of algorithm:
   * 1. For each child -> parent relationship, look for source nodes (`MethodNode`s) in the parent that are missing in
   *    the child. E.g., `Parent#action() -> Parent#doAction()`, where the `Child` class has no `action()` method.
   * 2. Create new "virtual" edges: for each parent edge that starts at a node not found in the child, create an edge
   *    with its source in the child (i.e., update the `owner` property) and also update the target's `owner` to be the
   *    child _if_ the original owner is the parent.
   * 3. Add these virtual edges to the graph. From the example above, imagine adding a virtual edge
   *    `Child#action() -> Child#doAction()`.
   */
  private fun hydrateGraph() {
    val edges = graph.edges()
    parentPointers.forEach { child, parent ->
      val parentEdges = edges.filter { it.source().owner == parent }
      val missingInChild = parentEdges.filter { edge ->
        val childEquivalent = edge.source().copy(owner = child)
        edges.find { it.source() == childEquivalent } == null
      }
      missingInChild.forEach { parentEdge ->
        val newSource = parentEdge.source().copy(owner = child)

        val oldTarget = parentEdge.target()
        val newTarget = if (oldTarget.owner == parent) {
          oldTarget.virtualOwner(child)
        } else {
          oldTarget
        }

        if (oldTarget != newTarget) {
          graph.putEdge(oldTarget, newTarget)
        }
//        graph.putEdge(oldTarget, newSource)
//        graph.putEdge(newSource, newTarget)
      }
    }
  }

  private fun isTaskAction(methodNode: MethodNode): Boolean {
    return methodNode.metadata.isTaskAction
  }

  private fun callsGetProject(methodNode: MethodNode): Boolean {
    return methodNode.name == "getProject" && methodNode.descriptor == "()Lorg/gradle/api/Project;"
  }

  private fun putEdge(owner: String, name: String, descriptor: String) {
    val source = methodNode()
    val target = methodInstructionNode(owner, name, descriptor)
    graph.putEdge(source, target)
  }

  private fun methodNode(): MethodNode {
    val currentMethod = checkNotNull(currentMethod)
    return MethodNode(
      owner = currentClass.name,
      name = currentMethod.name,
      descriptor = currentMethod.descriptor,
      metadata = MethodNode.Metadata(isTaskAction)
    )
  }

  private fun methodInstructionNode(
    owner: String,
    name: String,
    descriptor: String
  ) = MethodNode(
    owner = owner,
    name = name,
    descriptor = descriptor
  )
}
