package com.autonomousapps.issue

interface Issue {
  val name: String
  val trace: Trace

  fun description(): String = trace.string()
}

data class SubprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue

data class GetSubprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue

data class AllprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue

data class GetAllprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue

data class GetProjectInTaskActionIssue(
  override val name: String,
  override val trace: Trace
) : Issue
