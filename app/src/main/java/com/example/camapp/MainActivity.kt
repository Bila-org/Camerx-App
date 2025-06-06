package com.example.camapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camapp.model.CameraViewModel
import com.example.camapp.model.PermissionViewModel
import com.example.camapp.presentation.CamApp
import com.example.camapp.presentation.CameraPermissionTextProvider
import com.example.camapp.presentation.PermissionDialog
import com.example.camapp.presentation.RecordAudioPermissionTextProvider
import com.example.camapp.presentation.WriteStoragePermissionTextProvider
import com.example.camapp.ui.theme.CamAppTheme

class MainActivity : ComponentActivity() {

    companion object {
        private val basePermissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
        )
        private val storagePermission = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            basePermissions + storagePermission
        } else {
            basePermissions
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamAppTheme {

                val cameraViewModel: CameraViewModel by viewModels { CameraViewModel.Factory }
                val bitmap = cameraViewModel.bitmaps.collectAsState().value

                val permissionViewModel = viewModel<PermissionViewModel>()
                val dialogQueue = permissionViewModel.visiblePermissionDialogQueue
                val permissionsToRequest = remember { getRequiredPermissions().toList() }

                val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        permissionsToRequest.forEach { permission ->
                            permissionViewModel.onPermissionResult(
                                permission = permission,
                                isGranted = perms[permission] == true
                            )
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (permissionsToRequest.any {
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                it
                            ) != PackageManager.PERMISSION_GRANTED
                        }) {
                        multiplePermissionResultLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                dialogQueue
                    .reversed()
                    .forEach { permission ->
                        PermissionDialog(
                            permissionTextProvider = when (permission) {
                                Manifest.permission.CAMERA -> {
                                    CameraPermissionTextProvider()
                                }

                                Manifest.permission.RECORD_AUDIO -> {
                                    RecordAudioPermissionTextProvider()
                                }

                                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                                    WriteStoragePermissionTextProvider()
                                }

                                else -> return@forEach
                            },
                            isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                                permission
                            ),
                            onDismiss = permissionViewModel::dismissDialog,
                            onOKClick = {
                                permissionViewModel.dismissDialog()
                                multiplePermissionResultLauncher.launch(
                                    arrayOf(permission)
                                )
                            },
                            onGoToAppSettingsClick = ::openAppSettings
                        )
                    }


                val controller = remember {
                    LifecycleCameraController(this).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or
                                    CameraController.VIDEO_CAPTURE
                        )
                    }
                }

                CamApp(
                    controller = controller,
                    bitmaps = bitmap,
                    onOpenGalleryClick = {},
                    onTakePhotoClick = {
                        cameraViewModel.takePhoto(
                            controller = controller,
                        )
                    },

                    onRecordVideoClick = {
                        cameraViewModel.startRecording(
                            controller = controller
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}