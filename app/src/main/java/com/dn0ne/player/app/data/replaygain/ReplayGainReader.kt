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
import org.jaudiotagger.audio.exceptions.CannotReadException
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
            val kind = tagReaderFor(extension)
            // Nothing can read it, so don't copy it: it's untagged, and gets cached as such.
            if (kind == TagReaderKind.NONE) return@withContext TagState.Untagged
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

                val fields = when (kind) {
                    // jaudiotagger 3.0.1 can't open Opus, even inside a .ogg (see OpusTagEditor)
                    TagReaderKind.OPUS -> OpusTagEditor.readComments(temp)
                    TagReaderKind.OGG -> if (isOpusStream(temp.head())) {
                        OpusTagEditor.readComments(temp)
                    } else {
                        jaudiotaggerFields(temp)
                    }
                    else -> jaudiotaggerFields(temp)
                }
                tagStateFromFields(fields)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CannotReadException) {
                // The copy worked but jaudiotagger can't parse this file. That won't
                // change until the file does, and a new mtime refreshes the cache then.
                Log.w(LOG_TAG, "No readable tags in $mediaId", e)
                TagState.Untagged
            } catch (t: Throwable) {
                // jaudiotagger throws a wide range of checked exceptions for unsupported
                // or malformed files. Unknown means "retry next play", not "untagged".
                Log.w(LOG_TAG, "Failed to read ReplayGain tags from $mediaId", t)
                TagState.Unknown
            } finally {
                temp?.delete()
            }
        }

    // First bytes of the file: enough to hold the first Ogg page's header and payload start.
    // A plain read(), since InputStream.readNBytes needs API 33.
    private fun File.head(): ByteArray = inputStream().use { input ->
        val buffer = ByteArray(512)
        val read = input.read(buffer)
        buffer.copyOf(maxOf(read, 0))
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
