package com.example.cartproject.presentation.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cartproject.R
import com.example.cartproject.domain.settings.ReminderStatus
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.components.ZapomniSwitch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.koin.compose.viewmodel.koinViewModel

/** Раздел настроек повторения с системным запросом разрешения. */
@Composable
internal fun ReminderSettings() {
    val viewModel = koinViewModel<ReminderSettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var askingPermission by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            askingPermission = false
            viewModel.changeEnabled(true)
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Text(
        text = stringResource(R.string.reminder_settings_heading),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    val editable = state.preference != null && !state.saving && !askingPermission
    ZapomniSwitch(
        label = stringResource(R.string.reminder_settings_label),
        checked = state.enabled,
        onCheckedChange = { enabled ->
            if (enabled && needsNotificationPermission(context)) {
                askingPermission = true
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.changeEnabled(enabled)
            }
        },
        enabled = editable,
        modifier = Modifier.fillMaxWidth(),
    )
    val is24Hour = DateFormat.is24HourFormat(context)
    val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
    val time = state.time.format(DateTimeFormatter.ofPattern(timePattern))
    ZapomniButton(
        label = stringResource(R.string.reminder_settings_time, time),
        onClick = { showTimePicker = true },
        enabled = editable,
        modifier = Modifier.fillMaxWidth(),
        secondary = true,
    )

    if (state.status == ReminderStatus.PermissionRequired) {
        StatusMessage(
            kind = StatusKind.Error,
            title = stringResource(R.string.reminder_settings_permission_denied),
            action = StatusAction(stringResource(R.string.reminder_settings_open_android)) {
                openNotificationSettings(context)
            },
        )
    }
    state.error?.let { failure ->
        val message = when (failure) {
            ReminderSettingsError.Read -> R.string.reminder_settings_read_error
            ReminderSettingsError.Write -> R.string.reminder_settings_write_error
            ReminderSettingsError.Schedule -> R.string.reminder_settings_schedule_error
        }
        StatusMessage(
            kind = StatusKind.Error,
            title = stringResource(message),
            action = StatusAction(stringResource(R.string.retry), viewModel::retry),
        )
    }

    if (showTimePicker && state.preference != null) {
        DisposableEffect(context, state.time) {
            val dialog = TimePickerDialog(
                context,
                { _, hour, minute -> viewModel.changeTime(LocalTime.of(hour, minute)) },
                state.time.hour,
                state.time.minute,
                is24Hour,
            )
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
            onDispose {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
            }
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    val appDetails =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri(),
        )
    try {
        val intent = if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                Settings.EXTRA_APP_PACKAGE,
                context.packageName,
            )
        } else {
            appDetails
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(appDetails)
    }
}
