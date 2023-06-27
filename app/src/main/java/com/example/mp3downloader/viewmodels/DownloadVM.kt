package com.example.mp3downloader.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mp3downloader.utils.Convertor
import com.example.mp3downloader.utils.Downloader
import com.example.mp3downloader.utils.FileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

class DownloadVM : ViewModel() {

    private var _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private var _thumbnailUrl = MutableLiveData<String>()
    val thumbnailUrl : LiveData<String>
        get() = _thumbnailUrl

    private var _videoViews = MutableLiveData<Int>()
    val videoViews : LiveData<Int>
        get() = _videoViews

    private var _videoLikes = MutableLiveData<Int>()
    val videoLikes : LiveData<Int>
        get() = _videoLikes

    private var _mp3Url = MutableLiveData<String>()
    val mp3Url : LiveData<String>
        get() = _mp3Url

    private var _destinationFolder = MutableLiveData<Uri>()
    val destinationFolder : LiveData<Uri>
        get() = _destinationFolder

    var currentProcess = ""

    val isDownloaded = MutableLiveData(false)
    val bufferFile = MutableLiveData<File>()

    val downloadProgressPercentage = MutableLiveData(0)
    val downloadProgressBytes = MutableLiveData<String>()
    val downloadTotalBytes = MutableLiveData<String>()

    val convertProgressPercentage = MutableLiveData(0)
    val convertProgressBytes = MutableLiveData<String>()
    val converted = MutableLiveData(false)

    val saveProcessPercentage = MutableLiveData(0)
    val fileSaved = MutableLiveData(false)

    val processFailed = MutableLiveData(false)
    val failedMessage = MutableLiveData<String>()

    fun setFields(title : String, thumbnail : String, views : Int, likes : Int, mp3 : String, destination : Uri){
        _title.value = title
        _thumbnailUrl.value = thumbnail
        _videoViews.value = views
        _videoLikes.value = likes
        _mp3Url.value = mp3
        _destinationFolder.value = destination
    }

    fun downloadMedia(context : Context){
        currentProcess = "Download"
        viewModelScope.launch {
            withContext(Dispatchers.IO){

                //Downloading the file
                Downloader.download(mp3Url.value!!, context, Downloader.NUM_CONNECTIONS, object : Downloader.DownloadProgressListener {
                    // This is where you will receive the progress of the download
                    override fun onProgressUpdate(progress: Int, downloadedBytes: Long, totalBytes: Long) {
                        // Update UI with progress information (e.g., update progress bar)
                        val percentage = "$progress%"
                        val downloaded = formatBytes(downloadedBytes)
                        val total = formatBytes(totalBytes)
                        Log.i("Download", "Progress: $percentage Downloaded: $downloaded/$total")
                        downloadProgressPercentage.postValue(progress)
                        downloadProgressBytes.postValue(downloaded)
                        downloadTotalBytes.postValue(total)
                    }

                    // This is where you will receive the result of the download
                    override fun onDownloadComplete(file: File) {
                        // Handle download completion
                        Log.i("Download", "Download complete: ${file.absolutePath}")
                        bufferFile.postValue(file)
                        isDownloaded.postValue(true)
                    }

                    // This is where you will receive the error of the download
                    override fun onDownloadError(exception: Exception) {
                        // Handle download error
                        Log.i("Download", "Download failed!")
                        failedMessage.postValue("Please check your internet connection")
                        processFailed.postValue(true)
                        exception.printStackTrace()
                    }
                })
            }

        }
    }

    fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1] + "B"
        return String.format("%.1f %s", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }

    fun convertFile(context : Context){
        currentProcess = "Convert"
        viewModelScope.launch{
            withContext(Dispatchers.IO){

                //Converting the file
                Convertor.convertMediaToMP3(context, bufferFile.value!!, object : Convertor.ConversionListener {
                    // This is where you will receive the progress of the conversion
                    override fun onProgressUpdate(progress: String, percentage: Int) {
                        // Update UI with progress information (e.g., update progress bar)
                        Log.i("Convert", "Progress: $percentage")
                        convertProgressPercentage.postValue(percentage)
                        convertProgressBytes.postValue(progress)
                    }

                    // This is where you will receive the result of the conversion
                    override fun onConversionComplete(outputFile: File) {
                        // Handle successful conversion
                        bufferFile.value!!.delete()
                        bufferFile.postValue(outputFile)
                        Log.i("Convert", "Conversion complete: ${outputFile.absolutePath}")
                        converted.postValue(true)
                    }

                    // This is where you will receive the error of the conversion
                    override fun onConversionFailed() {
                        // Handle unsuccessful conversion
                        Log.i("Convert", "Conversion failed!")
                        failedMessage.postValue("Conversion failed!")
                        processFailed.postValue(true)
                    }
                })
            }
        }

    }

    fun saveFile(context : Context){
        currentProcess = "Save"
        viewModelScope.launch{
            withContext(Dispatchers.IO){

                //Getting file name
                var fileName = ""
                val fileExtension = ".mp3"
                val filePath = title.value

                for (i in 0 until filePath!!.length) {
                    if (filePath[i] == ' ') {
                        fileName = fileName.plus(" ")
                    } else if (filePath[i] in 'a'..'z' || filePath[i] in 'A'..'Z' || filePath[i] in '0'..'9') {
                        fileName = fileName.plus(filePath[i])
                    }
                }

                fileName += fileExtension

                Log.i("Save", "File name: $fileName")

                //Saving the file
                FileSaver.saveMp3FileToDirectory(context, destinationFolder.value!!, fileName, bufferFile.value!!, object : FileSaver.SaveFileCallback {
                    // This is where you will receive the progress of the save
                    override fun onProgress(progress: Int) {
                        Log.i("Save", "Progress: $progress")
                        saveProcessPercentage.postValue(progress)
                    }

                    // This is where you will receive the result of the save
                    override fun onSaveComplete(savedFileUri: Uri?) {
                        Log.i("Save", "File save complete: ${savedFileUri?.path}")
                        fileSaved.postValue(true)
                    }

                    // This is where you will receive the error of the save
                    override fun onError(e: Exception?) {
                        e?.printStackTrace()
                        failedMessage.postValue("File save failed!")
                        processFailed.postValue(true)
                    }
                })
            }
        }
    }

    //This function is called when the user cancels the process
    fun delete(){
        if(currentProcess == "Download"){
            Downloader.stopCall()
        }
        else if(currentProcess == "Convert"){
            Convertor.stopCall()
        }
    }
}