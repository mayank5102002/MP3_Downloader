package com.example.mp3downloader.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File


object Convertor {

    private val handler = Handler(Looper.getMainLooper())
    private var conversionListener: ConversionListener? = null

    fun convertMediaToMP3(context: Context, inputFilePath: File, listener: ConversionListener) {
        this.conversionListener = listener

        // Delete converted file if it exists
        val outputFilePath = File(context.cacheDir, "converted_audio.mp3").absolutePath
        if (File(outputFilePath).exists()) File(outputFilePath).delete()

        val ffmpegCommand = arrayOf(
            "-i", inputFilePath.absolutePath,
            "-vn", // Extract audio only
            "-acodec", "libmp3lame", // Specify MP3 codec
            "-b:a", "128k", // Specify audio bitrate
            outputFilePath
        )

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inputFilePath.absolutePath)

        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val videoLength = durationString?.toLongOrNull() ?: 0L

        retriever.release()

        Config.enableStatisticsCallback { statistics ->
            val progress: Float =
                java.lang.String.valueOf(statistics.time).toFloat() / videoLength
            val currentTime = convertMillisecondsToFormattedString(statistics.time.toLong())
            val totalTime = convertMillisecondsToFormattedString(videoLength)
            Log.i("Times", "$currentTime / $totalTime")
            val progressFinal = progress * 100
            val progressString = "$currentTime / $totalTime"
            updateProgress(progressString, progressFinal.toInt())
        }

        FFmpeg.executeAsync(ffmpegCommand) { executionId, returnCode ->
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                conversionListener?.onConversionComplete(File(outputFilePath))
            } else {
                conversionListener?.onConversionFailed()
            }
        }
    }
    private fun convertMillisecondsToFormattedString(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateProgress(progressString : String, progress: Int) {
        handler.post {
            conversionListener?.onProgressUpdate(progressString, progress)
        }
    }

    // Cancel conversion
    fun stopCall(){
        FFmpeg.cancel()
    }

    interface ConversionListener {
        fun onProgressUpdate(progress: String, percentage: Int)
        fun onConversionComplete(outputFile: File)
        fun onConversionFailed()
    }
}