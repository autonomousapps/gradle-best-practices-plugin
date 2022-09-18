package com.autonomousapps.issue

interface Issue {
  val name: String
  val trace: List<String>

  fun description(): String = trace.joinToString(separator = " -> ")
}

data class SubprojectsIssue(
  override val name: String,
  override val trace: List<String>
) : Issue

data class GetSubprojectsIssue(
  override val name: String,
  override val trace: List<String>
) : Issue

data class AllprojectsIssue(
  override val name: String,
  override val trace: List<String>
) : Issue

data class GetAllprojectsIssue(
  override val name: String,
  override val trace: List<String>
) : Issue
