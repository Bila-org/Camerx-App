package com.example.camapp.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CameraRepository(
    private val context: Context
) {

    private var recording: Recording? = null


    //    @SuppressLint("UnsafeOptInUsageError")
    fun takePhoto(
        controller: LifecycleCameraController,
        onSuccess: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            image.toBitmap()
                        } else {
                            val matrix = Matrix().apply {
                                postRotate(image.imageInfo.rotationDegrees.toFloat())
                                postScale(-1f, 1f)
                            }
                            Bitmap.createBitmap(
                                image.toBitmap(), 0, 0, image.width, image.height, matrix, true
                            )
                        }
                    onSuccess(bitmap)
                    image.close()
                }
            }
        )
    }


    @SuppressLint("MissingPermission")
    suspend fun recordVideo(
        controller: LifecycleCameraController,
        onRecordingEvent: (VideoRecordEvent) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            if (recording != null) {
                recording?.stop()
                recording = null
                return@withContext Result.failure(IllegalStateException("Recording already in progress"))
            }

            val outputFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "my-recording.mp4")
            }
            val deferred = CompletableDeferred<Result<File>>()

            recording = controller.startRecording(
                FileOutputOptions.Builder(outputFile).build(),
                AudioConfig.create(true),
                ContextCompat.getMainExecutor(context),
            ) { event ->
                onRecordingEvent(event)
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            recording?.close()
                            outputFile.delete()
                            recording = null
                            deferred.complete(Result.failure(Exception("Recording failed: ${event.error}")))

                        } else {
                            recording = null
                            deferred.complete(Result.success(outputFile))
                        }
                    }
                }
            }
            deferred.await()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveImageToGallery(bitmap: Bitmap, filename: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val finalFilename = if(filename.endsWith(".jpg")){
                    filename
                }else {
                    "$filename.jpg"
                }

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageUsingMediaStore(bitmap, filename)
                } else {
                    saveImageLegacy(bitmap, filename)
                }
                Result.success(uri)
            } catch (e: Exception) {
                // Log.e("CameraRepository", "Failed to save image", e)
                Result.failure(e)
            }
        }

    suspend fun saveVideoToGallery(file: File, filename: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val finalFilename = if(filename.endsWith(".mp4")){
                    filename
                }else {
                    "$filename.mp4"
                }
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveVideoUsingMediaStore(file, filename)
                } else {
                    saveVideoLegacy(file, filename)
                }
                Result.success(uri)
            } catch (e: Exception) {
                file.delete() // Clean up temp file on failure
                // Log.e("CameraRepository", "Failed to save video", e)
                Result.failure(e)
            }
        }

    private fun saveImageUsingMediaStore(bitmap: Bitmap, filename: String): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )?.also { uri ->
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
        } ?: throw Exception("Failed to create image file")
    }

    private fun saveImageLegacy(bitmap: Bitmap, filename: String): Uri {
        val imagesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(imagesDir, filename)

        FileOutputStream(imageFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }

    private fun saveVideoUsingMediaStore(file: File, filename: String): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }

        return context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )?.also { uri ->
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().copyTo(outputStream)
            }
            file.delete() // Clean up temp file
            // Notify Medistore
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        } ?: throw Exception("Failed to create video file")
    }

    private fun saveVideoLegacy(file: File, filename: String): Uri {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val destFile = File(moviesDir, filename)

        file.copyTo(destFile, overwrite = true)
        file.delete() // Clean up temp file

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            destFile
        )
    }

}