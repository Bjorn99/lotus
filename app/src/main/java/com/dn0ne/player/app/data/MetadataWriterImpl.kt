package com.dn0ne.player.app.data

import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.os.Build
import android.util.Log
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.format
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.NoWritePermissionsException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.valuepair.ImageFormats
import org.jaudiotagger.tag.images.AndroidArtwork
import org.jaudiotagger.tag.reference.PictureTypes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MetadataWriterImpl(
    private val context: Context
) : MetadataWriter {
    private val logTag = "Metadata Writer"

    override val unsupportedWriteFormats: List<String>
        get() = listOf("flac", "ogg")

    override fun writeMetadata(
        track: Track,
        metadata: Metadata,
        onSecurityError: (IntentSender) -> Unit
    ): Result<Unit, DataError.Local> {
        try {
            var file: File? = null
            context.contentResolver.openInputStream(track.uri)?.use { input ->
                // jaudiotagger dispatches readers/writers by file extension
                // and has no opus mapping. OPUS uses the same OGG container
                // and VorbisComment tags as Vorbis, so the OGG reader/writer
                // handles it correctly (the identification header is
                // preserved byte-for-byte).
                val ext = if (track.format == "opus") ".ogg" else ".${track.format}"
                val temp = File.createTempFile("temp_audio", ext, context.cacheDir)
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
                file = temp
            }

            if (file == null) return Result.Error(DataError.Local.NoReadPermission)

            val audioFile =
                AudioFileIO.read(file) ?: return Result.Error(DataError.Local.FailedToRead)
            val tag = audioFile.tagAndConvertOrCreateAndSetDefault
                ?: return Result.Error(DataError.Local.FailedToRead)

            metadata.run {
                title?.let {
                    tag.setField(FieldKey.TITLE, it)
                }

                album?.let {
                    tag.setField(FieldKey.ALBUM, it)
                }

                artist?.let {
                    tag.setField(FieldKey.ARTIST, it)
                }

                albumArtist?.let {
                    tag.setField(FieldKey.ALBUM_ARTIST, it)
                }

                genre?.let {
                    tag.setField(FieldKey.GENRE, it)
                }

                year?.let {
                    tag.setField(FieldKey.YEAR, it)
                }

                trackNumber?.let {
                    tag.setField(FieldKey.TRACK, it)
                }

                coverArtBytes?.let { artBytes ->
                    val cover = AndroidArtwork.createArtworkFromFile(file)
                    cover.binaryData = artBytes
                    cover.mimeType =
                        ImageFormats.getMimeTypeForBinarySignature(artBytes)
                    cover.pictureType = PictureTypes.DEFAULT_ID
                    cover.description = ""
                    tag.deleteArtworkField()
                    tag.setField(cover)
                }

                lyrics?.let {
                    tag.setField(FieldKey.LYRICS, it)
                }
            }

            try {
                context.contentResolver.openOutputStream(track.uri)?.use { output ->
                    AudioFileIO.write(audioFile)
                    FileInputStream(audioFile.file).use { input ->
                        input.copyTo(output)
                    }
                }
                MediaScannerConnection.scanFile(context, arrayOf(track.data), null, null)
                context.cacheDir?.deleteRecursively()
                return Result.Success(Unit)
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = e as?
                            RecoverableSecurityException ?: throw NoWritePermissionsException(e.message, e)

                    val intentSender =
                        recoverableSecurityException.userAction.actionIntent.intentSender

                    onSecurityError(intentSender)
                } else {
                    throw NoWritePermissionsException(e.message, e)
                }
            }

            return Result.Error(DataError.Local.NoWritePermission)
        } catch (e: CannotWriteException) {
            Log.d(logTag, e.message, e)
            val clipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            val clip =
                ClipData.newPlainText(
                    null,
                    e.message + "\n" + e.stackTrace.joinToString("\n")
                )
            clipboardManager?.setPrimaryClip(clip)
            return Result.Error(DataError.Local.NoWritePermission)
        } catch (e: CannotReadException) {
            Log.d(logTag, e.message, e)
            val clipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            val clip =
                ClipData.newPlainText(
                    null,
                    e.message + "\n" + e.stackTrace.joinToString("\n")
                )
            clipboardManager?.setPrimaryClip(clip)
            return Result.Error(DataError.Local.NoReadPermission)
        } catch (e: NoWritePermissionsException) {
            Log.d(logTag, e.message, e)
            val clipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            val clip =
                ClipData.newPlainText(
                    null,
                    e.message + "\n" + e.stackTrace.joinToString("\n")
                )
            clipboardManager?.setPrimaryClip(clip)
            return Result.Error(DataError.Local.NoWritePermission)
        } catch (e: Exception) {
            Log.d(logTag, e.message, e)
            val clipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            val clip =
                ClipData.newPlainText(
                    null,
                    e.message + "\n" + e.stackTrace.joinToString("\n")
                )
            clipboardManager?.setPrimaryClip(clip)
            return Result.Error(DataError.Local.Unknown)
        } catch (e: java.lang.Exception) {
            Log.d(logTag, e.message, e)
            val clipboardManager =
                context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
            val clip =
                ClipData.newPlainText(
                    null,
                    e.message + "\n" + e.stackTrace.joinToString("\n")
                )
            clipboardManager?.setPrimaryClip(clip)
            return Result.Error(DataError.Local.Unknown)
        }
    }
}