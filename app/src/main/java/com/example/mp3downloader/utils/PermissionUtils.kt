package com.example.mp3downloader.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData

object PermissionUtils {

    const val STORAGE_PERMISSION_REQUEST_CODE = 1

    val storagePermissionGranted = MutableLiveData(false)

    fun checkStoragePermission(context : Context) : Boolean{
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission(activity : Activity){
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    fun afterStoragePermission(){
        storagePermissionGranted.value = false
    }
}