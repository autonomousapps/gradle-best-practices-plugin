package com.autonomousapps.internal.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshix.sealed.reflect.MetadataMoshiSealedJsonAdapterFactory

internal object Json {
  private val MOSHI: Moshi by lazy {
    Moshi.Builder()
      .add(MetadataMoshiSealedJsonAdapterFactory())
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  private inline fun <reified T> getJsonAdapter(): JsonAdapter<T> {
    return MOSHI.adapter(T::class.java)
  }

  private inline fun <reified T> getJsonListAdapter(): JsonAdapter<List<T>> {
    val type = Types.newParameterizedType(List::class.java, T::class.java)
    return MOSHI.adapter(type)
  }

  inline fun <reified T> T.toJson(): String {
    return getJsonAdapter<T>().toJson(this)
  }

  private inline fun <reified T> T.toPrettyString(): String {
    return getJsonAdapter<T>().indent("  ").toJson(this)
  }

  inline fun <reified T> String.fromJson(): T {
    return getJsonAdapter<T>().fromJson(this)!!
  }
}
