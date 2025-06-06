package com.example.camapp.model

import android.graphics.Bitmap
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.camapp.CamAppApplication
import com.example.camapp.data.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraViewModel(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

    fun takePhoto(
        controller: LifecycleCameraController
    ) {
        cameraRepository.takePhoto(controller) { bitmap ->
            if (bitmap != null) {
                _bitmaps.value += bitmap
                val timestamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                viewModelScope.launch {
                    cameraRepository.saveImageToGallery(bitmap, timestamp)
                }
            }
        }
    }

    fun startRecording(
        controller: LifecycleCameraController
    ) {
        viewModelScope.launch {
            val result = cameraRepository.recordVideo(controller = controller)

            if (result.isSuccess) {
                val videoFile = result.getOrNull()
                if (videoFile != null) {
                    val timestamp =
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    cameraRepository.saveVideoToGallery(videoFile, timestamp)
                }
            }
        }
    }


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY]) as CamAppApplication
                val cameraRepository = application.cameraRepository
                CameraViewModel(cameraRepository = cameraRepository)
            }
        }
    }
}