package com.crochetquest.app.ui.util

import android.content.ContentResolver
import android.net.Uri
import com.crochetquest.app.domain.backup.BackupFormatException
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024

/** Reads a chosen backup file as text, refusing anything unreasonably large. */
fun ContentResolver.readBackupText(uri: Uri): String {
    val input = openInputStream(uri) ?: throw IOException("Couldn't open the file.")
    input.use { stream ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BACKUP_BYTES) {
                throw BackupFormatException("That file is too large to be a Crochet Quest backup.")
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }
}

/** Writes [bytes] to the file the player chose, replacing anything already in it. */
fun ContentResolver.writeBackupBytes(uri: Uri, bytes: ByteArray) {
    val output = openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file.")
    output.use { it.write(bytes) }
}
