package com.example.camapp.presentation

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.camapp.PhotoBottomSheetContent
import com.example.camapp.ui.theme.CamAppTheme
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamApp(
    controller: LifecycleCameraController,
    bitmaps: List<Bitmap>,
    onOpenGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            PhotoBottomSheetContent(
                bitmaps = bitmaps,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CameraPreview(
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )

            CameraScreen(
                onCameraSelectorClick =
                {
                    controller.cameraSelector =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else CameraSelector.DEFAULT_BACK_CAMERA
                },
                //onOpenGalleryClick = onOpenGalleryClick,
                onOpenGalleryClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                },
                onTakePhotoClick = onTakePhotoClick,
                onRecordVideoClick = onRecordVideoClick,
                modifier = Modifier
            )
        }
    }


}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CamAppPreview() {
    CamAppTheme {
        CameraScreen(
            onCameraSelectorClick = {},
            onOpenGalleryClick = {},
            onTakePhotoClick = {},
            onRecordVideoClick = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Composable
fun CameraScreen(
    onCameraSelectorClick: () -> Unit,
    onOpenGalleryClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRecordVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                onCameraSelectorClick()
            },
            modifier = Modifier
                .offset(16.dp, 25.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Cameraswitch,
                contentDescription = "Switch camera"
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(
                onClick = {
                    onOpenGalleryClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Photo,
                    contentDescription = "Open the gallery"
                )
            }

            IconButton(
                onClick = {
                    onTakePhotoClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Take photo"
                )
            }

            IconButton(
                onClick = {
                    onRecordVideoClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "Record video"
                )
            }
        }
    }
}