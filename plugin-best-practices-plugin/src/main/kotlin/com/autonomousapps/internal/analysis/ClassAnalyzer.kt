package com.autonomousapps.internal.analysis

import com.autonomousapps.internal.asm.AnnotationVisitor
import com.autonomousapps.internal.asm.ClassVisitor
import com.autonomousapps.internal.asm.MethodVisitor
import com.autonomousapps.internal.asm.Opcodes
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
    interfaces: Array<String>?
  ) {
    logger.log("ClassAnalyzer#visit: $name super=$superName")
    trace.add(name)
    listener.visitClass(name, superName, interfaces?.toList() ?: emptyList())
  }

  override fun visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    logger.log("- visitMethod: name=$name descriptor=$descriptor signature=$signature access=$access")

    val thisTrace = ArrayList(trace).apply { add(name) }
    listener.visitMethod(name, descriptor)
    return MethodAnalyzer(logger, listener, thisTrace)
  }

  internal class MethodAnalyzer(
    private val logger: Logger,
    private val listener: IssueListener,
    private val trace: MutableList<String>
  ) : MethodVisitor(ASM_VERSION) {

    override fun visitEnd() {
      listener.visitMethodEnd()
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      logger.log("  - visitAnnotation: descriptor=$descriptor visible=$visible")
      listener.visitMethodAnnotation(trace, descriptor)
      return null
    }

    override fun visitMethodInsn(
      opcode: Int,
      owner: String,
      name: String,
      descriptor: String,
      isInterface: Boolean
    ) {
      logger.log("  - visitMethodInsn: owner=$owner name=$name descriptor=$descriptor opcode=$opcode")
      listener.visitMethodInstruction(trace, owner, name, descriptor)
    }
  }
}

private fun Logger.log(msg: String) {
  if (System.getProperty("best-practices-logging") == "quiet") {
    quiet(msg)
  } else {
    debug(msg)
  }
}
