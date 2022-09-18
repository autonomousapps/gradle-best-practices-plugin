package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.asm.ClassVisitor
import com.autonomousapps.internal.asm.MethodVisitor
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.issue.AllprojectsIssue
import com.autonomousapps.issue.GetAllprojectsIssue
import com.autonomousapps.issue.GetSubprojectsIssue
import com.autonomousapps.issue.Issue
import com.autonomousapps.issue.SubprojectsIssue
import org.gradle.api.logging.Logger

private const val ASM_VERSION = Opcodes.ASM9

internal class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM_VERSION) {

  val issues = mutableSetOf<Issue>()

  private val trace = mutableListOf<String>()

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    logger.debug("Visiting $name super=$superName")
    trace.add(name.dotty())
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    logger.debug("- visitMethod: $name; $descriptor")

    val thisTrace = ArrayList(trace).apply { add(name) }
    return MethodAnalyzer(logger, issues, thisTrace)
  }

  internal class MethodAnalyzer(
    private val logger: Logger,
    private val issues: MutableSet<Issue>,
    private val trace: MutableList<String>
  ) : MethodVisitor(ASM_VERSION) {

    override fun visitMethodInsn(
      opcode: Int,
      owner: String,
      name: String,
      descriptor: String?,
      isInterface: Boolean
    ) {
      logger.debug("  - visitMethodInsn: owner=$owner name=$name")

      val thisTrace = ArrayList(trace).apply {
        add("${owner.dotty()}#$name")
      }

      val issue = if (owner == "org/gradle/api/Project") {
        when (name) {
          "subprojects" -> SubprojectsIssue(name, thisTrace)
          "getSubprojects" -> GetSubprojectsIssue(name, thisTrace)
          "allprojects" -> AllprojectsIssue(name, thisTrace)
          "getAllprojects" -> GetAllprojectsIssue(name, thisTrace)
          else -> null
        }
      } else {
        null
      }

      issue?.let { issues.add(it) }
    }
  }
}

internal fun String.dotty(): String = replace('/', '.')
