package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.asm.AnnotationVisitor
import com.autonomousapps.internal.asm.ClassVisitor
import com.autonomousapps.internal.asm.MethodVisitor
import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.utils.dotty
import org.gradle.api.logging.Logger

private const val ASM_VERSION = Opcodes.ASM9

internal class ClassAnalyzer(
  private val logger: Logger,
  private val listener: IssueListener,
) : ClassVisitor(ASM_VERSION) {

  private val trace = mutableListOf<String>()

  override fun visit(
    version: Int,
    access: Int,
    name: String,
    signature: String?,
    superName: String?,
    interfaces: Array<out String>?
  ) {
    logger.quiet("ClassAnalyzer#visit: $name super=$superName")
    trace.add(name.dotty())
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    logger.quiet("- visitMethod: name=$name descriptor=$descriptor signature=$signature access=$access")

    val thisTrace = ArrayList(trace).apply { add(name) }
    return MethodAnalyzer(logger, listener, thisTrace)
  }

  internal class MethodAnalyzer(
    private val logger: Logger,
    private val listener: IssueListener,
    private val trace: MutableList<String>
  ) : MethodVisitor(ASM_VERSION) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      logger.quiet("  - visitAnnotation: descriptor=$descriptor visible=$visible")
      listener.visitAnnotation(trace, descriptor)
      return null
    }

    override fun visitMethodInsn(
      opcode: Int,
      owner: String,
      name: String,
      descriptor: String,
      isInterface: Boolean
    ) {
      logger.quiet("  - visitMethodInsn: owner=$owner name=$name descriptor=$descriptor opcode=$opcode")
      listener.visitMethodCall(trace, owner, name, descriptor)
    }
  }
}
