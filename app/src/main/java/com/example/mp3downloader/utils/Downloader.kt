package com.example.mp3downloader.utils

import android.content.Context
import okhttp3.*
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

object Downloader {

    val client = OkHttpClient()

    const val NUM_CONNECTIONS = 5

    fun download(url: String, context: Context, numConnections: Int, listener: DownloadProgressListener) {
        val request = Request.Builder()
            .url(url)
            .build()

        // Start a new thread for downloading
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body
                responseBody?.let {
                    try {
                        val totalBytes = it.contentLength()

                        val tempDir = context.cacheDir // Use cache directory for temporary storage
                        val tempFiles = ArrayList<File>()
                        val latch = CountDownLatch(numConnections)
                        val downloadedBytes = AtomicLong(0)

                        val chunkSize = totalBytes / numConnections

                        for (i in 0 until numConnections) {
                            val startOffset = i * chunkSize
                            val endOffset = if (i == numConnections - 1) totalBytes - 1 else (i + 1) * chunkSize - 1

                            val tempFile = File(tempDir, "temp_media_$i.mp3")
                            if (tempFile.exists()) tempFile.delete()
                            tempFiles.add(tempFile)

                            val rangeRequest = Request.Builder()
                                .url(url)
                                .header("Range", "bytes=$startOffset-$endOffset")
                                .build()

                            client.newCall(rangeRequest).enqueue(object : Callback {
                                override fun onResponse(call: Call, response: Response) {
                                    val responseBody1 = response.body
                                    responseBody1?.let {
                                        try {
                                            val inputStream = it.byteStream()
                                            val outputStream = tempFile.outputStream()

                                            val bufferedSource = inputStream.source().buffer()
                                            val bufferedSink = outputStream.sink().buffer()

                                            var readBytes: Long

                                            // Read bytes from server
                                            while (bufferedSource.read(bufferedSink.buffer, 8192).also { readBytes = it } != -1L) {
                                                bufferedSink.emit()
                                                downloadedBytes.addAndGet(readBytes)
                                                val progress = (downloadedBytes.get() * 100 / totalBytes).toInt()
                                                listener.onProgressUpdate(progress, downloadedBytes.get(), totalBytes)
                                            }

                                            bufferedSink.flush()
                                            bufferedSink.close()
                                            bufferedSource.close()

                                            latch.countDown()
                                        } catch (e: IOException) {
                                            listener.onDownloadError(e)
                                            latch.countDown()
                                        }
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    listener.onDownloadError(e)
                                    latch.countDown()
                                }
                            })
                        }

                        try {
                            // Wait for all requests to complete
                            latch.await()

                            // All requests have completed, perform any final processing
                            combineDownloadedFiles(tempFiles, listener, context)
                        } catch (e: InterruptedException) {
                            listener.onDownloadError(e)
                        }
                    } catch (e: IOException) {
                        listener.onDownloadError(e)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                listener.onDownloadError(e)
            }
        })
    }

    private fun combineDownloadedFiles(tempFiles: List<File>, listener: DownloadProgressListener, context : Context) {
        try {
            // Merge the downloaded files
            val tempDir = context.cacheDir
            val outputFile = File(tempDir, "temp_media.mp3")
            val outputStream = outputFile.outputStream()

            for (tempFile in tempFiles) {
                val inputStream = tempFile.inputStream()
                inputStream.copyTo(outputStream)
                inputStream.close()
            }

            outputStream.close()

            // Cleanup temporary files
            for (tempFile in tempFiles) {
                tempFile.delete()
            }

            listener.onDownloadComplete(outputFile)
        } catch (e: IOException) {
            listener.onDownloadError(e)
        }
    }

    // Stop the download
    fun stopCall() {
        client.dispatcher.cancelAll()
    }

    interface DownloadProgressListener {
        fun onProgressUpdate(progress: Int, downloadedBytes: Long, totalBytes: Long)
        fun onDownloadComplete(file: File)
        fun onDownloadError(exception: Exception)
    }
}
