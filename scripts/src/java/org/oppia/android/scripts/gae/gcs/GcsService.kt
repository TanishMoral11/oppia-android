package org.oppia.android.scripts.gae.gcs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.Request
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit

class GcsService(private val baseUrl: String, private val gcsBucket: String) {
  private val retrofit by lazy { Retrofit.Builder().baseUrl(baseUrl).build() }
  private val apiService by lazy { retrofit.create(GcsEndpointApi::class.java) }

  fun fetchImageContentLengthAsync(
    imageContainerType: ImageContainerType,
    imageType: ImageType,
    entityId: String,
    imageFilename: String
  ): Deferred<Long?> {
    return apiService.fetchImageData(
      gcsBucket,
      imageContainerType.httpRepresentation,
      entityId,
      imageType.httpRepresentation,
      imageFilename
    ).resolveAsync(
      transform = { request, response ->
        checkNotNull(response.body()) {
          "Failed to receive body for request: $request."
        }.use { it.contentLength() }
      },
      default = { _, _, -> null }
//      default = { request, response ->
//        error("Failed to call: $request. Encountered failure:\n$response")
//      }
    )
  }

  fun fetchImageContentDataAsync(
    imageContainerType: ImageContainerType,
    imageType: ImageType,
    entityId: String,
    imageFilename: String
  ): Deferred<ByteArray?> {
    return apiService.fetchImageData(
      gcsBucket,
      imageContainerType.httpRepresentation,
      entityId,
      imageType.httpRepresentation,
      imageFilename
    ).resolveAsync(
      transform = { request, response ->
        checkNotNull(response.body()) { "Failed to receive body for request: $request." }.use {
          it.byteStream().readBytes()
        }
      },
      default = { _, _ -> null }
//      default = { request, response ->
//        error("Failed to call: $request. Encountered failure:\n$response")
//      }
    )
  }

  fun computeImageUrl(
    imageContainerType: ImageContainerType,
    imageType: ImageType,
    entityId: String,
    imageFilename: String
  ): String {
    val containerTypeHttpRep = imageContainerType.httpRepresentation
    val imgTypeHttpRep = imageType.httpRepresentation
    return "${baseUrl.removeSuffix("/")}/$gcsBucket/$containerTypeHttpRep/$entityId/assets" +
      "/$imgTypeHttpRep/$imageFilename"
  }

  enum class ImageContainerType(val httpRepresentation: String) {
    EXPLORATION(httpRepresentation = "exploration"),
    SKILL(httpRepresentation = "skill"),
    TOPIC(httpRepresentation = "topic"),
    STORY(httpRepresentation = "story"),
    CLASSROOM(httpRepresentation = "unknown") // TODO: Figure out what this should be.
  }

  enum class ImageType(val httpRepresentation: String) {
    HTML_IMAGE(httpRepresentation = "image"),
    THUMBNAIL(httpRepresentation = "thumbnail")
  }

  private companion object {
    private fun <I, O> Call<I>.resolveAsync(
      transform: (Request, Response<I>) -> O,
      default: (Request, Response<I>) -> O
    ): Deferred<O> {
      // Use the I/O dispatcher for blocking HTTP operations (since it's designed to handle blocking
      // operations that might otherwise stall a coroutine dispatcher).
      return CoroutineScope(Dispatchers.IO).async {
        val result = execute()
        return@async if (result.isSuccessful) {
          transform(request(), result)
        } else default(request(), result)
      }
    }
  }
}
