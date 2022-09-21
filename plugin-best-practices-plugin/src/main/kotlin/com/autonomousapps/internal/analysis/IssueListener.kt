package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.utils.dotty
import com.autonomousapps.issue.AllprojectsIssue
import com.autonomousapps.issue.GetAllprojectsIssue
import com.autonomousapps.issue.GetSubprojectsIssue
import com.autonomousapps.issue.Issue
import com.autonomousapps.issue.SubprojectsIssue
import com.autonomousapps.issue.Trace

internal interface IssueListener {

  fun computeIssues(): Set<Issue>

  fun visitMethodCall(trace: List<String>, owner: String, name: String, descriptor: String) = Unit
  fun visitAnnotation(trace: List<String>, descriptor: String) = Unit
}

internal class CompositeIssueListener(
  private val listeners: List<IssueListener>,
) : IssueListener {

  override fun computeIssues(): Set<Issue> {
    return listeners.asSequence().flatMap { it.computeIssues() }.toSet()
  }

  override fun visitAnnotation(trace: List<String>, descriptor: String) {
    listeners.forEach { it.visitAnnotation(trace, descriptor) }
  }

  override fun visitMethodCall(trace: List<String>, owner: String, name: String, descriptor: String) {
    listeners.forEach { it.visitMethodCall(trace, owner, name, descriptor) }
  }
}

internal class AllProjectsListener : IssueListener {

  private val issues = mutableSetOf<Issue>()

  override fun computeIssues(): Set<Issue> = issues

  override fun visitMethodCall(trace: List<String>, owner: String, name: String, descriptor: String) {
    val thisTrace = ArrayList(trace).apply {
      add("${owner.dotty()}#$name")
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

  override fun visitMethodCall(trace: List<String>, owner: String, name: String, descriptor: String) {
    val thisTrace = ArrayList(trace).apply {
      add("${owner.dotty()}#$name")
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

// TODO main issue with this new pattern is that this listener is now global, vs the old Visitor was created anew for
//  each class under analysis
internal class GetProjectListener : IssueListener {

  private val issues = mutableSetOf<Issue>()

  private var isTaskAction = false

  override fun computeIssues(): Set<Issue> {
    // TODO "Not yet implemented"
    return issues
  }

  override fun visitAnnotation(trace: List<String>, descriptor: String) {
    // TODO look at tasks that have a hierarchy (AGP does this a lot)
    isTaskAction = descriptor == "Lorg/gradle/api/tasks/TaskAction;"
  }

  // TODO:
  //  See https://stackoverflow.com/questions/47000699/how-to-extract-access-flags-of-a-field-in-asm-visitfield-method
  //  FancyTask has a TaskAction that calls FancyTask#doAction, implemented by FancyTask$ReallyFancyTask, which calls
  //  getProject(), a method that returns Project. I can see that ReallyFancyTask extends FancyTask, so that means the
  //  method doAction() (with identical signature) must override doAction() in the super. From there I can make a
  //  connection.
  //  =>
  //  ClassAnalyzer#visit: com/test/FancyTask$ReallyFancyTask super=com/test/FancyTask
  //  - visitMethod: name=doAction descriptor=()V signature=null access=4 (4 == protected)
  //    - visitMethodInsn: owner=com/test/FancyTask$ReallyFancyTask name=getProject descriptor=()Lorg/gradle/api/Project;
  //  =>
  //  ClassAnalyzer#visit: com/test/FancyTask super=org/gradle/api/DefaultTask
  //  - visitMethod: name=doAction descriptor=()V signature=null access=1028 (1028 == abstract)
  //  - visitMethod: name=action descriptor=()V signature=null access=1
  //    - visitAnnotation: descriptor=Lorg/gradle/api/tasks/TaskAction; visible=true
  //    - visitMethodInsn: owner=com/test/FancyTask name=doAction descriptor=()V
  override fun visitMethodCall(trace: List<String>, owner: String, name: String, descriptor: String) {
    // if (name == "getProject" && descriptor == "()Lorg/gradle/api/Project;") {
    //   // this is suspect, but not immediately bad. We need to know if this is inside a task action
    //   if (isTaskAction) {
    //     // this may literally be in a task action
    //     val thisTrace = ArrayList(trace).apply {
    //       add("${owner.dotty()}#$name")
    //     }
    //     issues.add(GetProjectInTaskActionIssue(name, thisTrace))
    //   } else {
    //     // or it may be in a method called from a task action via some chain
    //     val thisTrace = ArrayList(trace).apply {
    //       add("${owner.dotty()}#$name")
    //     }
    //     listener.possibleIssues.add(GetProjectInTaskActionIssue(name, thisTrace))
    //   }
    // }
  }
}
