package com.autonomousapps.internal.graph

internal class Class(
  val name: String,
  val superName: String?
)

internal class Method(
  val name: String,
  val descriptor: String
)

data class MethodNode(
  val owner: String,
  val name: String,
  val descriptor: String,
  val metadata: Metadata = Metadata.EMPTY
) {

  data class Metadata(
    /** The associated [MethodNode] is annotated with `@TaskAction`. */
    val isTaskAction: Boolean = false,

    /** The associated [MethodNode] does not really exist. It exists for algorithmic purposes only. */
    val isVirtual: Boolean = false,
  ) {
    companion object {
      val EMPTY = Metadata()
    }
  }

  fun withVirtualOwner(owner: String) = copy(
    owner = owner,
    metadata = metadata.copy(isVirtual = true)
  )

  fun signatureMatches(other: MethodNode): Boolean {
    return name == other.name && descriptor == other.descriptor
  }

  /*
   * Custom equals and hashCode because we don't want to include Metadata in the calculation.
   */

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MethodNode

    if (owner != other.owner) return false
    if (name != other.name) return false
    if (descriptor != other.descriptor) return false

    return true
  }

  override fun hashCode(): Int {
    var result = owner.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + descriptor.hashCode()
    return result
  }
}
