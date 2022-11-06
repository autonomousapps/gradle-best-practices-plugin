@file:Suppress("UnstableApiUsage", "HasPlatformType") // Guava Graphs

package com.autonomousapps.internal.analysis

import com.autonomousapps.graph.ShortestPath
import com.autonomousapps.internal.graphs.Class
import com.autonomousapps.internal.graphs.Method
import com.autonomousapps.internal.graphs.MethodNode
import com.autonomousapps.issue.AllprojectsIssue
import com.autonomousapps.issue.EagerApiIssue
import com.autonomousapps.issue.GetAllprojectsIssue
import com.autonomousapps.issue.GetProjectInTaskActionIssue
import com.autonomousapps.issue.GetSubprojectsIssue
import com.autonomousapps.issue.Issue
import com.autonomousapps.issue.SubprojectsIssue
import com.autonomousapps.issue.Trace
import com.google.common.collect.MultimapBuilder
import com.google.common.graph.ElementOrder
import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder

internal interface IssueListener {

  fun computeIssues(): Set<Issue>

  fun visitClass(name: String, superName: String?, interfaces: List<String>) = Unit
  fun visitMethod(name: String, descriptor: String) = Unit
  fun visitMethodEnd() = Unit
  fun visitMethodInstruction(owner: String, name: String, descriptor: String) = Unit
  fun visitMethodAnnotation(descriptor: String) = Unit
}

internal abstract class AbstractIssueListener : IssueListener {

  private lateinit var currentClass: Class
  private var currentMethod: Method? = null

  protected val parentPointers = MultimapBuilder.hashKeys().hashSetValues().build<String, String>()
  protected val graph = GraphBuilder
    .directed()
    .incidentEdgeOrder<MethodNode>(ElementOrder.stable())
    .allowsSelfLoops(true)
    .build<MethodNode>()

  final override fun visitClass(name: String, superName: String?, interfaces: List<String>) {
    currentClass = Class(name, superName)
    superName?.let { parent ->
      parentPointers.put(name, parent)
    }
    interfaces.forEach { parent ->
      parentPointers.put(name, parent)
    }
    onVisitClass(name, superName, interfaces)
  }

  protected open fun onVisitClass(name: String, superName: String?, interfaces: List<String>) {
    // do nothing by default
  }

  final override fun visitMethod(name: String, descriptor: String) {
    currentMethod = Method(name, descriptor)
  }

  final override fun visitMethodEnd() {
    currentMethod = null
    onVisitMethodEnd()
  }

  protected open fun onVisitMethodEnd() {
    // do nothing by default
  }

  final override fun visitMethodAnnotation(descriptor: String) {
    onVisitMethodAnnotation(descriptor)
  }

  protected open fun onVisitMethodAnnotation(descriptor: String) {
    // do nothing by default
  }

  final override fun visitMethodInstruction(owner: String, name: String, descriptor: String) {
    // put an edge in the graph
    val source = methodNode()
    val target = methodInstructionNode(owner, name, descriptor)
    graph.putEdge(source, target)

    onVisitMethodInstruction(owner, name, descriptor)
  }

  protected open fun onVisitMethodInstruction(owner: String, name: String, descriptor: String) {
    // do nothing by default
  }

  protected fun computeTraces(): Set<Trace> {
    preComputeTraces()

    val suspectNodes = graph.nodes().filter { isSuspectNode(graph, it) }
    if (suspectNodes.isEmpty()) return emptySet()

    val entryPoints = graph.nodes().filter { isEntryPointNode(graph, it) }
    if (entryPoints.isEmpty()) return emptySet()

    // We have valid entry points and suspect exit points. Is there a path between any of them?

    return entryPoints.flatMapTo(HashSet()) { entryNode ->
      val paths = ShortestPath(graph, entryNode)
      suspectNodes.asSequence()
        .map { paths.pathTo(it) }
        .map { it.toList() }
        .filter { it.isNotEmpty() }
        .map { Trace(it) }
    }
  }

  protected open fun preComputeTraces() {
    // do nothing by default
  }

  protected open fun isEntryPointNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    return graph.inDegree(methodNode) == 0
  }

  abstract fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean

  private fun methodNode(): MethodNode {
    val currentMethod = checkNotNull(currentMethod)
    return MethodNode(
      owner = currentClass.name,
      name = currentMethod.name,
      descriptor = currentMethod.descriptor,
      metadata = methodMetadata(),
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

  protected open fun methodMetadata(): MethodNode.Metadata = MethodNode.Metadata.EMPTY
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

  override fun visitMethodAnnotation(descriptor: String) {
    listeners.forEach { it.visitMethodAnnotation(descriptor) }
  }

  override fun visitMethodInstruction(owner: String, name: String, descriptor: String) {
    listeners.forEach { it.visitMethodInstruction(owner, name, descriptor) }
  }
}

/** Calls `Project.allprojects()`. */
internal class AllProjectsListener : AbstractIssueListener() {

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    AllprojectsIssue("allprojects", trace)
  }

  override fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    return methodNode.owner == "org/gradle/api/Project" && methodNode.name == "allprojects"
  }
}

/** Calls `Project.getAllprojects()`. */
internal class GetAllprojectsListener : AbstractIssueListener() {

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    GetAllprojectsIssue("getAllprojects", trace)
  }

  override fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    return methodNode.owner == "org/gradle/api/Project" && methodNode.name == "getAllprojects"
  }
}

/** Calls `Project.subprojects()`. */
internal class SubprojectsListener : AbstractIssueListener() {

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    SubprojectsIssue("subprojects", trace)
  }

  override fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    return methodNode.owner == "org/gradle/api/Project" && methodNode.name == "subprojects"
  }
}


/** Calls `Project.getSubprojects()`. */
internal class GetSubprojectsListener : AbstractIssueListener() {

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    GetSubprojectsIssue("getSubprojects", trace)
  }

  override fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    return methodNode.owner == "org/gradle/api/Project" && methodNode.name == "getSubprojects"
  }
}

/** Calls `Project.getProject()`. */
internal class GetProjectListener : AbstractIssueListener() {

  private var isTaskAction = false

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    GetProjectInTaskActionIssue("getProject", trace)
  }

  override fun onVisitMethodEnd() {
    isTaskAction = false
  }

  override fun onVisitMethodAnnotation(descriptor: String) {
    isTaskAction = descriptor == "Lorg/gradle/api/tasks/TaskAction;"
  }

  override fun isEntryPointNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean = isTaskAction(methodNode)

  override fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    return callsGetProject(methodNode)
  }

  private fun isTaskAction(methodNode: MethodNode): Boolean {
    return methodNode.metadata.isTaskAction
  }

  private fun callsGetProject(methodNode: MethodNode): Boolean {
    return methodNode.name == "getProject" && methodNode.descriptor == "()Lorg/gradle/api/Project;"
  }

  override fun preComputeTraces() {
    hydrateGraph()
  }

  /**
   * Hydrate the graph with artificial nodes and edges to account for class hierarchies and the many paths code may take
   * to reach suspect method calls. Example:
   * ```
   * // Real method traces
   * Parent#action -> Parent#doAction   // an abstract method
   * Child#doAction -> Child#getProject // Child is a Task, and so getProject is suspect _only if_ called from an action
   *
   * // What we want
   * Parent#action -> Parent#doAction   // a real method call
   * Parent#doAction -> Child#doAction  // a "virtual" method call
   * Child#doAction -> Child#getProject // a real method call
   * ```
   */
  private fun hydrateGraph() {
    val edges = graph.edges()
    parentPointers.forEach { child, parent ->
      // Get all edges in the graph that start at a parent (of the current child) node
      val parentEdges = edges.filter { it.source().owner == parent }

      // Find edges in the parent that don't have a matching source in the child. E.g., Child has no `action` method.
      val missingInChild = parentEdges.filter { edge ->
        edges.any { it.source().signatureMatches(edge.source()) }
      }

      // For every "missing method" in the child, we create a virtual edge in the graph.
      missingInChild.forEach { parentEdge ->
        maybeCreateVirtualEdge(child = child, parent = parent, parentEdge = parentEdge)
      }
    }
  }

  private fun maybeCreateVirtualEdge(
    child: String,
    parent: String,
    parentEdge: EndpointPair<MethodNode>
  ) {
    val oldTarget = parentEdge.target()
    var newTarget = if (oldTarget.owner == parent) {
      oldTarget.withVirtualOwner(child)
    } else {
      oldTarget
    }

    // If there's already a matching node in the graph, just use that one.
    graph.nodes().find { it == newTarget }?.let {
      newTarget = it
    }

    if (oldTarget != newTarget) {
      graph.putEdge(oldTarget, newTarget)
    }
  }

  override fun methodMetadata(): MethodNode.Metadata {
    return MethodNode.Metadata(isTaskAction)
  }
}

/** Invokes Eager APIs instead of Lazy ones.
 * @see <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html">Lazy Configuration</a>
 * @see <a href="https://docs.gradle.org/current/userguide/task_configuration_avoidance.html#sec:old_vs_new_configuration_api_overview">Old vs New API Overview</a>
 */
internal class EagerApisListener : AbstractIssueListener() {

  private val eagerApis = mapOf(
    "org/gradle/api/tasks/TaskContainer" to setOf("all", "create", "getByName")
  )

  override fun computeIssues(): Set<Issue> = computeTraces().mapTo(HashSet()) { trace ->
    EagerApiIssue("eagerApis", trace)
  }

  override fun isSuspectNode(graph: Graph<MethodNode>, methodNode: MethodNode): Boolean {
    val methodOwner = methodNode.owner
    val methodName = methodNode.name
    return eagerApis[methodOwner]?.contains(methodName) ?: false
  }
}
