package com.autonomousapps.issue

import kotlinx.serialization.Serializable

@Serializable
sealed class Issue {
  abstract val name: String
  abstract val trace: Trace
}

@Serializable
data class SubprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@Serializable
data class GetSubprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@Serializable
data class AllprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@Serializable
data class GetAllprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@Serializable
data class GetProjectInTaskActionIssue(
  override val name: String,
  override val trace: Trace
) : Issue()
