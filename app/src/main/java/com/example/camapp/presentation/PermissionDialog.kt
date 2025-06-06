package com.example.camapp.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.camapp.ui.theme.CamAppTheme


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PermissionDialogPreview() {
    CamAppTheme {
        PermissionDialog(
            permissionTextProvider = WriteStoragePermissionTextProvider(),
            isPermanentlyDeclined = true,
            onDismiss = {},
            onOKClick = {},
            onGoToAppSettingsClick = {},
        )
    }
}

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOKClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider()
                TextButton(
                    onClick = {
                        if (isPermanentlyDeclined) {
                            onGoToAppSettingsClick()
                        } else {
                            onOKClick()
                        }
                    }
                ) {
                    Text(
                        text = if (isPermanentlyDeclined) {
                            "Grant permission"
                        } else {
                            "OK"
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        title = {
            Text(
                text = "Permission required"
            )
        },
        text = {
            Text(
                text = permissionTextProvider.getDescription(
                    isPermanentlyDeclined = isPermanentlyDeclined
                )
            )
        },
        modifier = modifier,
    )
}


interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class CameraPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined camera permission. " +
                    "You can go the app settings to grant the camera permission."
        } else {
            "This app need access to device camera to take picture and record video."
        }
    }
}

class RecordAudioPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined microphone permission. " +
                    "You can go to the app setting to grant the microphone permission."
        } else {
            "This app need access to microphone to record audio."
        }
    }
}

class WriteStoragePermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "It seems you permanently declined data write to storage. " +
                    "You can go to the app setting to grant the storage permission."
        } else {
            "This app need access to storage permission."
        }
    }
}