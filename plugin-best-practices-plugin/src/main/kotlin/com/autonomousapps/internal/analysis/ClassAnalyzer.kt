package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.asm.ClassVisitor
import com.autonomousapps.internal.asm.MethodVisitor
import com.autonomousapps.internal.asm.Opcodes
import org.gradle.api.logging.Logger

private const val ASM_VERSION = Opcodes.ASM9

internal class ClassAnalyzer(private val logger: Logger) : ClassVisitor(ASM_VERSION) {

  val issues = mutableSetOf<Issue>()

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    logger.quiet("Visiting $name")
  }

  override fun visitMethod(
    access: Int, name: String?, descriptor: String, signature: String?, exceptions: Array<out String>?
  ): MethodVisitor {
    logger.quiet("- visitMethod: $name; $descriptor")
    return MethodAnalyzer(logger, issues)
  }

  internal class MethodAnalyzer(
    private val logger: Logger,
    private val issues: MutableSet<Issue>
  ) : MethodVisitor(ASM_VERSION) {

    // visitMethod: apply; (Lorg/gradle/api/Project;)V
    // visitMethodInsn: owner=org/gradle/api/Project name=subprojects
    // visitMethodInsn: owner=org/gradle/api/Project name=getSubprojects
    // visitMethodInsn: owner=org/gradle/api/Project name=allprojects
    // visitMethodInsn: owner=org/gradle/api/Project name=getAllprojects
    override fun visitMethodInsn(
      opcode: Int,
      owner: String?,
      name: String?,
      descriptor: String?,
      isInterface: Boolean
    ) {
      logger.quiet("  - visitMethodInsn: owner=$owner name=$name")
      val issue = if (owner == "org/gradle/api/Project") {
        when (name) {
          "subprojects" -> Issue("Uses subprojects {}")
          "getSubprojects" -> Issue("Uses getSubprojects()")
          "allprojects" -> Issue("Uses allprojects {}")
          "getAllprojects" -> Issue("Uses getAllprojects()")
          else -> null
        }
      } else {
        null
      }

      issue?.let { issues.add(it) }
    }
  }
}
