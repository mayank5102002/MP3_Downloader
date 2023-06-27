package com.example.mp3downloader.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object FileSaver {

    interface SaveFileCallback {
        fun onProgress(progress: Int)
        fun onSaveComplete(savedFileUri: Uri?)
        fun onError(e: Exception?)
    }

    fun saveMp3FileToDirectory(
        context: Context,
        folderUri: Uri,
        fileName: String,
        inputFile: File,
        saveFileCallback: SaveFileCallback
    ) {
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)

        // Check if the selected directory exists
        documentFile?.let { directory ->
            // Check if a file with the same name already exists in the destination folder
            var newFileName = fileName
            var fileCounter = 1
            // If a file with the same name already exists, append a counter to the file name
            while (fileExistsInDirectory(directory, newFileName)) {
                // Append a counter to the file name to make it unique
                newFileName = "${fileName.substringBeforeLast('.')}($fileCounter).${fileName.substringAfterLast('.')}"
                fileCounter++
            }

            // Create a new file inside the selected directory
            val newFile = directory.createFile("audio/mpeg", newFileName)

            newFile?.let { file ->
                try {
                    // Open an OutputStream for the new file
                    val outputStream = context.contentResolver.openOutputStream(file.uri)

                    outputStream?.use { stream ->
                        // Calculate the total file size
                        val totalSize = inputFile.length()
                        var bytesWritten: Long = 0

                        // Write the file content to the OutputStream in chunks
                        val bufferSize = 1024 * 8 // 8 KB buffer size
                        val buffer = ByteArray(bufferSize)
                        var bytesRead: Int

                        val progressUpdateInterval = 2 // Update progress every 10% of progress
                        var lastProgress = 0

                        val inputStream = FileInputStream(inputFile)
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            stream.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead

                            // Update progress if necessary
                            val progress = ((bytesWritten * 100) / totalSize).toInt()
                            if (progress - lastProgress >= progressUpdateInterval) {
                                lastProgress = progress
                                saveFileCallback.onProgress(progress)
                            }
                        }

                        stream.flush()
                    }
                } catch (e: IOException) {
                    // Handle any exceptions that occur during file saving
                    saveFileCallback.onError(e)
                }
            }

            saveFileCallback.onSaveComplete(newFile?.uri)
        }
    }

    // Check if a file with the same name already exists in the destination folder
    private fun fileExistsInDirectory(directory: DocumentFile, fileName: String): Boolean {
        val files = directory.listFiles()
        for (file in files) {
            if (file.name == fileName) {
                return true
            }
        }
        return false
    }

}