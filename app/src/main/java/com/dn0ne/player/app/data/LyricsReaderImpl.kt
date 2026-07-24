package com.dn0ne.player.app.data

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.dn0ne.player.app.domain.lyrics.Lyrics
import com.dn0ne.player.app.domain.lyrics.toSyncedLyrics
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.format
import com.kyant.taglib.TagLib
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

            val lyricsText = ParcelFileDescriptor.open(temp!!, ParcelFileDescriptor.MODE_READ_ONLY)
                .use { pfd ->
                    val lyricsValues = TagLib.getMetadataPropertyValues(
                        pfd.dup().detachFd(), "LYRICS"
                    )
                    if (lyricsValues != null && lyricsValues.isNotEmpty()) {
                        lyricsValues[0]
                    } else null
                }

            if (lyricsText?.isNotBlank() != true) {
                return Result.Error(DataError.Local.NoLyricsFound)
            }

            val lyrics = try {
                val syncedLyrics = lyricsText.toSyncedLyrics()
                Lyrics(
                    uri = track.uri.toString(),
                    synced = syncedLyrics,
                    plain = syncedLyrics.map { it.second },
                    areFromRemote = false
                )
            } catch (_: IllegalArgumentException) {
                Lyrics(
                    uri = track.uri.toString(),
                    plain = lyricsText.split('\n'),
                    areFromRemote = false
                )
            }

            Result.Success(lyrics)
        } catch (t: Throwable) {
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
