package com.dn0ne.player.app.data.replaygain

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.dn0ne.player.app.data.OpusTagEditor
import com.dn0ne.player.app.domain.replaygain.TagState
import com.dn0ne.player.app.domain.replaygain.tagStateFromFields
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.TagTextField
import org.jaudiotagger.tag.id3.AbstractTagFrame
import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX
import java.io.File
import java.io.FileOutputStream

private const val LOG_TAG = "ReplayGainReader"

class ReplayGainReader(private val context: Context) : ReplayGainSource {

    override suspend fun fileInfo(mediaId: String): MediaFileInfo? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(
                Uri.parse(mediaId),
                arrayOf(
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATA,
                ),
                null, null, null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                MediaFileInfo(
                    dateModified = cursor.getLong(0),
                    size = cursor.getLong(1),
                    extension = cursor.getString(2)
                        ?.substringAfterLast('.', "")?.lowercase().orEmpty(),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // query() throws SecurityException / IllegalArgumentException for revoked or
            // malformed URIs.
            Log.w(LOG_TAG, "No MediaStore row for $mediaId", t)
            null
        }
    }

    override suspend fun readTags(mediaId: String, extension: String): TagState =
        withContext(Dispatchers.IO) {
            var temp: File? = null
            // Everything, temp-file creation included, stays inside the try: in the spike
            // a createTempFile throw outside it took down the playback service.
            try {
                temp = File.createTempFile("replaygain", ".$extension", context.cacheDir)
                // jaudiotagger needs a real File, so copy the content URI the same way
                // LyricsReaderImpl does. The Room cache makes this once per file.
                val copied = context.contentResolver.openInputStream(Uri.parse(mediaId))?.use { input ->
                    FileOutputStream(temp).use { output -> input.copyTo(output) }
                }
                if (copied == null) return@withContext TagState.Unknown

                val fields = if (extension == "opus") {
                    // jaudiotagger 3.0.1 can't open .opus (see OpusTagEditor)
                    OpusTagEditor.readComments(temp)
                } else {
                    jaudiotaggerFields(temp)
                }
                tagStateFromFields(fields)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                // jaudiotagger throws a wide range of checked exceptions for unsupported
                // or malformed files. Unknown means "retry next play", not "untagged".
                Log.w(LOG_TAG, "Failed to read ReplayGain tags from $mediaId", t)
                TagState.Unknown
            } finally {
                temp?.delete()
            }
        }

    private fun jaudiotaggerFields(file: File): List<Pair<String, String>> {
        val tag = AudioFileIO.read(file).tag ?: return emptyList()
        return tag.fields.asSequence().mapNotNull { field ->
            // MP3 keeps ReplayGain in TXXX frames keyed by description. getFirst("TXXX")
            // would return whichever TXXX comes first, so walk them all.
            val txxx = (field as? AbstractTagFrame)?.body as? FrameBodyTXXX
            when {
                txxx != null -> "TXXX:${txxx.description}" to txxx.firstTextValue
                // Vorbis comments (FLAC, Ogg) and MP4 freeform
                // (----:com.apple.iTunes:replaygain_*) both arrive as text fields.
                field is TagTextField -> field.id to field.content
                else -> null
            }
        }.toList()
    }
}
