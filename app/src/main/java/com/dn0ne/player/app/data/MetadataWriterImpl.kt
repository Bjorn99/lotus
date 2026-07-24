package com.dn0ne.player.app.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.dn0ne.player.app.data.db.TrackMetadataDao
import com.dn0ne.player.app.data.db.TrackMetadataEntity
import com.dn0ne.player.app.domain.metadata.Metadata
import com.dn0ne.player.app.domain.result.DataError
import com.dn0ne.player.app.domain.result.Result
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.format
import com.kyant.taglib.Picture
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MetadataWriterImpl(
    private val context: Context,
    private val trackMetadataDao: TrackMetadataDao,
) : MetadataWriter {
    private val logTag = "Metadata Writer"

    override val unsupportedWriteFormats: List<String>
        get() = emptyList()

    override fun writeMetadata(
        track: Track,
        metadata: Metadata,
        onSecurityError: (IntentSender) -> Unit
    ): Result<Unit, DataError.Local> {
        try {
            var file: File? = null
            context.contentResolver.openInputStream(track.uri)?.use { input ->
                val temp = File.createTempFile("temp_audio", ".${track.format}", context.cacheDir)
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
                file = temp
            }

            if (file == null) return Result.Error(DataError.Local.NoReadPermission)

            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
                .use { pfd ->
                    val existing = TagLib.getMetadata(pfd.dup().detachFd(), readPictures = false)
                    val propertyMap = existing?.propertyMap ?: PropertyMap()

                    metadata.title?.let { propertyMap["TITLE"] = arrayOf(it) }
                    metadata.album?.let { propertyMap["ALBUM"] = arrayOf(it) }
                    metadata.artist?.let { propertyMap["ARTIST"] = arrayOf(it) }
                    metadata.albumArtist?.let { propertyMap["ALBUMARTIST"] = arrayOf(it) }
                    metadata.genre?.let { propertyMap["GENRE"] = arrayOf(it) }
                    metadata.year?.let { propertyMap["DATE"] = arrayOf(it) }
                    metadata.trackNumber?.let { propertyMap["TRACKNUMBER"] = arrayOf(it) }
                    metadata.lyrics?.let { propertyMap["LYRICS"] = arrayOf(it) }
                    metadata.mbAlbumId?.let { propertyMap["MUSICBRAINZ_RELEASEID"] = arrayOf(it) }
                    metadata.mbReleaseGroupId?.let { propertyMap["MUSICBRAINZ_RELEASEGROUPID"] = arrayOf(it) }
                    metadata.mbAlbumArtistId?.let { propertyMap["MUSICBRAINZ_ALBUMARTISTID"] = arrayOf(it) }

                    TagLib.savePropertyMap(pfd.dup().detachFd(), propertyMap)

                    metadata.coverArtBytes?.let { artBytes ->
                        TagLib.savePictures(
                            pfd.dup().detachFd(), arrayOf(
                                Picture(
                                    data = artBytes,
                                    description = "",
                                    pictureType = "Front Cover",
                                    mimeType = detectImageMimeType(artBytes),
                                )
                            )
                        )
                    }
                }

            try {
                context.contentResolver.openOutputStream(track.uri)?.use { output ->
                    FileInputStream(file).use { input ->
                        input.copyTo(output)
                    }
                }
                MediaScannerConnection.scanFile(context, arrayOf(track.data), null, null)

                if (metadata.mbAlbumId != null || metadata.mbReleaseGroupId != null
                    || metadata.mbAlbumArtistId != null
                ) {
                    runBlocking {
                        trackMetadataDao.upsert(
                            TrackMetadataEntity(
                                trackData = track.data,
                                mbAlbumId = metadata.mbAlbumId,
                                mbReleaseGroupId = metadata.mbReleaseGroupId,
                                mbAlbumArtistId = metadata.mbAlbumArtistId,
                            )
                        )
                    }
                }

                file.delete()
                return Result.Success(Unit)
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = e as?
                            RecoverableSecurityException ?: throw RuntimeException(e.message, e)

                    val intentSender =
                        recoverableSecurityException.userAction.actionIntent.intentSender

                    onSecurityError(intentSender)
                } else {
                    throw RuntimeException(e.message, e)
                }
            }

            return Result.Error(DataError.Local.NoWritePermission)
        } catch (e: Exception) {
            Log.w(logTag, "Unexpected failure while editing metadata", e)
            return Result.Error(DataError.Local.Unknown)
        }
    }

    companion object {
        internal fun detectImageMimeType(bytes: ByteArray): String {
            return when {
                bytes.size >= 2 &&
                    bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte() -> "image/jpeg"

                bytes.size >= 4 &&
                    bytes[0] == 0x89.toByte() &&
                    bytes[1] == 'P'.code.toByte() &&
                    bytes[2] == 'N'.code.toByte() &&
                    bytes[3] == 'G'.code.toByte() -> "image/png"

                bytes.size >= 3 &&
                    bytes[0] == 'G'.code.toByte() &&
                    bytes[1] == 'I'.code.toByte() &&
                    bytes[2] == 'F'.code.toByte() -> "image/gif"

                bytes.size >= 2 &&
                    bytes[0] == 'B'.code.toByte() &&
                    bytes[1] == 'M'.code.toByte() -> "image/bmp"

                bytes.size >= 4 &&
                    bytes[0] == 'R'.code.toByte() &&
                    bytes[1] == 'I'.code.toByte() &&
                    bytes[2] == 'F'.code.toByte() &&
                    bytes[3] == 'F'.code.toByte() -> "image/webp"

                else -> "image/jpeg"
            }
        }
    }
}
