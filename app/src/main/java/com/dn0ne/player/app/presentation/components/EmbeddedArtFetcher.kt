package com.dn0ne.player.app.presentation.components

import android.content.Context
import android.media.MediaMetadataRetriever
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import okio.Buffer
import okio.FileSystem

@OptIn(ExperimentalCoilApi::class)
class EmbeddedArtFetcher(
    private val model: EmbeddedArtModel,
    private val context: Context,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, model.trackUri)
            val picture = retriever.embeddedPicture ?: return null
            val buffer = Buffer()
            buffer.write(picture)
            SourceFetchResult(
                source = ImageSource(buffer, FileSystem.SYSTEM),
                mimeType = "image/jpeg",
                dataSource = DataSource.MEMORY,
            )
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<EmbeddedArtModel> {
        override fun create(
            data: EmbeddedArtModel,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            return EmbeddedArtFetcher(data, context)
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
class EmbeddedArtInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): coil3.request.ImageResult {
        val result = chain.proceed()
        if (result !is ErrorResult) return result
        val model = chain.request.data as? EmbeddedArtModel ?: return result
        val fallbackRequest = ImageRequest.Builder(chain.request.context)
            .data(model.fallbackUri)
            .memoryCacheKey(chain.request.memoryCacheKey)
            .build()
        return chain.withRequest(fallbackRequest).proceed()
    }
}
