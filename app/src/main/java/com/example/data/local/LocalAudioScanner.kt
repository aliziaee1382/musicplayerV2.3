package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class LocalAudioScanner(private val context: Context) {

    fun scanLocalTracksFlow(
        existingTrackIds: Set<Long> = emptySet(),
        chunkSize: Int = 15
    ): Flow<List<Track>> = flow {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        // Do not restrict IS_MUSIC or DURATION strictly in SQL query as MediaStore index flags vary across devices
        val selection: String? = null

        val chunkBuffer = mutableListOf<Track>()
        var index = 0

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val trackId = id + 500000L

                    // Skip tracks that are already saved in local database for fast incremental scanning
                    if (existingTrackIds.contains(trackId)) {
                        index++
                        continue
                    }

                    val durationMs = cursor.getInt(durationColumn)
                    val filePath = cursor.getString(dataColumn) ?: ""
                    val rawTitle = cursor.getString(titleColumn) ?: ""

                    // Extract file name if title is empty
                    val title = when {
                        rawTitle.isNotBlank() && rawTitle != "<unknown>" -> rawTitle
                        filePath.isNotBlank() -> {
                            val fileName = filePath.substringAfterLast('/')
                            if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
                        }
                        else -> "Track $index"
                    }

                    // Extract extension safely from filePath without java.io.File existence check
                    val extension = if (filePath.contains('.')) {
                        filePath.substringAfterLast('.').lowercase()
                    } else ""

                    if (extension.isNotEmpty() && extension !in VALID_MUSIC_EXTENSIONS) {
                        continue
                    }

                    // Skip system or hidden paths (e.g. /.cache/, /android/data/)
                    if (isSystemOrHiddenPath(filePath)) {
                        continue
                    }

                    // Extract folder name safely
                    val folderName = extractFolderName(filePath)

                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Local Songs"
                    val albumId = cursor.getLong(albumIdColumn)

                    val dateAddedSec = if (dateAddedColumn >= 0) cursor.getLong(dateAddedColumn) else 0L
                    val dateModifiedSec = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
                    val dateAddedMs = if (dateAddedSec > 0) dateAddedSec * 1000L else System.currentTimeMillis()
                    val dateModifiedMs = if (dateModifiedSec > 0) dateModifiedSec * 1000L else System.currentTimeMillis()

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    // Safe album art resolution
                    val albumArtUri = resolveAlbumArtUri(
                        context = context,
                        filePath = filePath,
                        contentUri = contentUri,
                        trackId = trackId,
                        albumId = albumId
                    )

                    val cleanArtist = if (artist == "<unknown>" || artist.isBlank()) "Local Artist" else artist
                    val cleanAlbum = if (album == "<unknown>" || album.isBlank()) "Local Album" else album
                    val calculatedDurationSec = if (durationMs > 0) durationMs / 1000 else 180

                    chunkBuffer.add(
                        Track(
                            id = trackId,
                            title = title,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            durationSeconds = calculatedDurationSec.coerceAtLeast(1),
                            audioUrl = if (contentUri.isNotBlank()) contentUri else filePath,
                            category = folderName,
                            coverGradientIndex = (index % 5),
                            albumArtUri = albumArtUri,
                            isLocal = true,
                            folderName = folderName,
                            dateAddedTimestamp = dateAddedMs,
                            dateModifiedTimestamp = dateModifiedMs
                        )
                    )
                    index++

                    if (chunkBuffer.size >= chunkSize) {
                        emit(chunkBuffer.toList())
                        chunkBuffer.clear()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (chunkBuffer.isNotEmpty()) {
            emit(chunkBuffer.toList())
            chunkBuffer.clear()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun scanLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Track>()
        scanLocalTracksFlow(chunkSize = 100).collect { chunk ->
            result.addAll(chunk)
        }
        result
    }

    companion object {
        private val VALID_MUSIC_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "opus", "wma", "3gp")

        private fun isSystemOrHiddenPath(filePath: String): Boolean {
            if (filePath.isBlank()) return false
            val lowerPath = filePath.lowercase()

            // Hidden directory check (directory segment starts with '.')
            if (lowerPath.contains("/.")) {
                return true
            }

            // Android system data / obb directories
            if (lowerPath.contains("/android/data/") || lowerPath.contains("/android/obb/")) {
                return true
            }

            // Internal app cache / temp directories
            if (lowerPath.contains("/cache/") || lowerPath.contains("/.cache/")) {
                return true
            }

            // Check if directory contains a .nomedia file
            try {
                if (filePath.contains('/')) {
                    val file = File(filePath)
                    val parentDir = file.parentFile
                    if (parentDir != null && File(parentDir, ".nomedia").exists()) {
                        return true
                    }
                }
            } catch (e: Exception) {
                // Ignore exception
            }

            return false
        }

        private fun extractFolderName(filePath: String): String {
            if (filePath.isBlank() || !filePath.contains('/')) return "Phone Storage"
            return try {
                val parts = filePath.split('/')
                if (parts.size >= 2) {
                    val folder = parts[parts.size - 2]
                    if (folder.isNotBlank()) folder else "Phone Storage"
                } else {
                    "Phone Storage"
                }
            } catch (e: Exception) {
                "Phone Storage"
            }
        }

        fun resolveAlbumArtUri(
            context: Context,
            filePath: String,
            contentUri: String,
            trackId: Long,
            albumId: Long
        ): String? {
            // 1. Primary: Extract embedded picture directly from audio metadata
            val embeddedUri = extractEmbeddedPicture(context, filePath, contentUri, trackId)
            if (!embeddedUri.isNullOrEmpty()) {
                return embeddedUri
            }

            // 2. Fallback: MediaStore album art URI if available
            if (albumId > 0) {
                val mediaStoreUriString = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val isValid = try {
                    context.contentResolver.openFileDescriptor(Uri.parse(mediaStoreUriString), "r")?.use { true } ?: false
                } catch (e: Exception) {
                    false
                }

                if (isValid) {
                    return mediaStoreUriString
                }
            }

            return null
        }

        private fun extractEmbeddedPicture(
            context: Context,
            filePath: String,
            contentUri: String,
            trackId: Long
        ): String? {
            val retriever = MediaMetadataRetriever()
            try {
                if (contentUri.isNotBlank()) {
                    retriever.setDataSource(context, Uri.parse(contentUri))
                } else if (filePath.isNotBlank()) {
                    retriever.setDataSource(filePath)
                } else {
                    return null
                }

                val artBytes = retriever.embeddedPicture
                if (artBytes != null && artBytes.isNotEmpty()) {
                    val cacheDir = File(context.cacheDir, "album_covers")
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs()
                    }
                    val coverFile = File(cacheDir, "cover_$trackId.jpg")
                    if (!coverFile.exists() || coverFile.length() == 0L) {
                        coverFile.writeBytes(artBytes)
                    }
                    return Uri.fromFile(coverFile).toString()
                }
            } catch (e: Exception) {
                // Ignore corrupt or unreadable files cleanly so scan continues smoothly
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
            return null
        }
    }
}
