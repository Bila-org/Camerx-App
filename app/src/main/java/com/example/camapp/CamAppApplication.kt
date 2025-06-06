package com.example.camapp

import android.app.Application
import com.example.camapp.data.CameraRepository

class CamAppApplication : Application() {

    val cameraRepository: CameraRepository by lazy {
        CameraRepository(applicationContext)
    }
}