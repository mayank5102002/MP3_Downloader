package com.example.mp3downloader.viewmodels

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainVM : ViewModel() {

    private val _destinationFolder = MutableLiveData<Uri>()
    val destinationFolder : LiveData<Uri>
        get() = _destinationFolder

    val destinationFolderSelected = MutableLiveData(false)

    private val _videoUrl = MutableLiveData<String>()
    val videoUrl : LiveData<String>
        get() = _videoUrl

    val error = MutableLiveData(false)
    val infoFetched = MutableLiveData(false)

    private val _title = MutableLiveData<String>()
    val title : LiveData<String>
        get() = _title

    private val _description = MutableLiveData<String>()
    val description : LiveData<String>
        get() = _description

    private val _thumbnail = MutableLiveData<String>()
    val thumbnail : LiveData<String>
        get() = _thumbnail

    private val _viewCount = MutableLiveData<String>()
    val viewCount : LiveData<String>
        get() = _viewCount

    private val _likeCount = MutableLiveData<String>()
    val likeCount : LiveData<String>
        get() = _likeCount

    private val _mp3Url = MutableLiveData<String>()
    val mp3Url : LiveData<String>
        get() = _mp3Url

    fun setDestinationFolder(path : Uri){
        _destinationFolder.value = path
        destinationFolderSelected.value = true
    }

    fun reset(){
        _destinationFolder.value = null
        destinationFolderSelected.value = false
        _videoUrl.value = null
    }

    fun getVideoContext(videoUrl : String, context : Context) {

        viewModelScope.launch{
            withContext(Dispatchers.IO){
                try {
                    if (!Python.isStarted()) {
                        Python.start(AndroidPlatform(context))
                    }

                    val python = Python.getInstance()
                    val ytDlpScript = python.getModule("yt_dlp_script")
                    val getVideoContext: PyObject? = ytDlpScript["get_video_context"]?.call(videoUrl)

                    val title = getVideoContext?.asList()?.get(0)?.toString()
                    val description = getVideoContext?.asList()?.get(1)?.toString()
                    val thumbnail = getVideoContext?.asList()?.get(2)?.toString()
                    val viewCount = getVideoContext?.asList()?.get(3)?.toString()?.toInt()
                    val likeCount = getVideoContext?.asList()?.get(4)?.toString()?.toInt()
                    val mp3Url = getVideoContext?.asList()?.get(5)?.toString()

                    _title.postValue(title)
                    _description.postValue(description)
                    _thumbnail.postValue(thumbnail)
                    _viewCount.postValue(viewCount.toString())
                    _likeCount.postValue(likeCount.toString())
                    _mp3Url.postValue(mp3Url)

                    infoFetched.postValue(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    error.postValue(true)
                }
            }
        }
    }

}