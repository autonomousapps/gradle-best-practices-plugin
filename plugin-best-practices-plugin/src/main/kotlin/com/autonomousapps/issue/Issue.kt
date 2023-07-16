package com.autonomousapps.issue

import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

class IssuesReport(val issues: List<Issue>)

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Issue {
  abstract val name: String
  abstract val trace: Trace
}

@TypeLabel("subprojects")
@JsonClass(generateAdapter = false)
data class SubprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@TypeLabel("get_subprojects")
@JsonClass(generateAdapter = false)
data class GetSubprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@TypeLabel("allprojects")
@JsonClass(generateAdapter = false)
data class AllprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@TypeLabel("get_allprojects")
@JsonClass(generateAdapter = false)
data class GetAllprojectsIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@TypeLabel("get_project")
@JsonClass(generateAdapter = false)
data class GetProjectInTaskActionIssue(
  override val name: String,
  override val trace: Trace
) : Issue()

@TypeLabel("eager_api")
@JsonClass(generateAdapter = false)
data class EagerApiIssue(
  override val name: String,
  override val trace: Trace
) : Issue()
