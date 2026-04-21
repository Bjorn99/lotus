package com.dn0ne.player.app.data

import android.content.Context
import android.util.Log
import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.format
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream

class LyricsReaderImpl(private val context: Context) : LyricsReader {
    override fun readFromTag(track: Track): Result<Lyrics?, DataError.Local> {
        var temp: File? = null
        return try {
            context.contentResolver.openInputStream(track.uri)?.use { input ->
                val file = File.createTempFile("temp_audio", ".${track.format}", context.cacheDir)
                temp = file
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.Error(DataError.Local.NoReadPermission)

            val audioFile = AudioFileIO.read(temp)
                ?: return Result.Error(DataError.Local.FailedToRead)
            val tag = audioFile.tagAndConvertOrCreateAndSetDefault
                ?: return Result.Error(DataError.Local.FailedToRead)

            val lyricsText = tag.getFirst(FieldKey.LYRICS)
            val lyrics = if (lyricsText?.isNotBlank() == true) {
                Lyrics(
                    uri = track.uri.toString(),
                    plain = lyricsText.split('\n'),
                    synced = null,
                    areFromRemote = false
                )
            } else {
                null
            }

            Result.Success(lyrics)
        } catch (t: Throwable) {
            // jaudiotagger throws a wide variety of checked exceptions for
            // unsupported/malformed files; treat any failure as "couldn't read"
            // rather than crashing the app.
            Log.w(LOG_TAG, "Failed to read lyrics from tag for ${track.uri}", t)
            Result.Error(DataError.Local.FailedToRead)
        } finally {
            // Only remove our own temp file — the cache dir is shared with
            // Coil's image cache and must not be wiped here.
            temp?.delete()
        }
    }

    private companion object {
        const val LOG_TAG = "LyricsReaderImpl"
    }
}
