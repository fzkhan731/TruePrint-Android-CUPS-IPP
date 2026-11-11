package com.trueprint.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trueprint.app.viewmodel.PrinterSettingField
import com.trueprint.app.viewmodel.PrinterSettingsUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PrinterSettingsScreen(
    settings: PrinterSettingsUiState,
    notification: String?,
    onBack: () -> Unit,
    onFieldChanged: (PrinterSettingField, String) -> Unit,
    onSave: () -> Unit,
    onResetRequested: () -> Unit,
    onResetDismissed: () -> Unit,
    onResetConfirmed: () -> Unit,
    onNotificationShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    BackHandler(onBack = onBack)

    LaunchedEffect(notification) {
        notification?.let { message ->
            snackbarHostState.showSnackbar(message)
            onNotificationShown()
        }
    }

    if (settings.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = onResetDismissed,
            title = { Text("Reset printer settings?") },
            text = { Text("This restores the default printer profile saved on this phone.") },
            confirmButton = { TextButton(onClick = onResetConfirmed) { Text("RESET") } },
            dismissButton = { TextButton(onClick = onResetDismissed) { Text("CANCEL") } }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Printer Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enter the details for the CUPS printer you want to use. These settings stay on this phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("What to enter", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Printer name: a label for this printer, such as Office printer.")
                    Text("CUPS server: a private IP address or hostname, such as 100.x.x.x or cups.example.tailnet.ts.net.")
                    Text("Port: normally 631 for IPP. Printer queue: the queue name shown by your CUPS server.")
                    Text("Use Test connection after saving to confirm that TruePrint can reach CUPS.")
                }
            }
            SettingsSection("Printer")
            SettingsTextField(
                label = "Printer Name",
                value = settings.printerName,
                error = settings.validation.printerName,
                placeholder = "For example, Office printer",
                onValueChange = { onFieldChanged(PrinterSettingField.PrinterName, it) }
            )
            SettingsTextField(
                label = "Protocol",
                value = settings.protocol,
                error = settings.validation.protocol,
                placeholder = "IPP",
                onValueChange = { onFieldChanged(PrinterSettingField.Protocol, it) }
            )
            SettingsSection("Server")
            SettingsTextField(
                label = "Server / IP Address",
                value = settings.server,
                error = settings.validation.server,
                placeholder = "100.x.x.x or cups.example.tailnet.ts.net",
                onValueChange = { onFieldChanged(PrinterSettingField.Server, it) }
            )
            SettingsSection("Connection")
            SettingsTextField(
                label = "Port",
                value = settings.port,
                error = settings.validation.port,
                placeholder = "631",
                keyboardType = KeyboardType.Number,
                onValueChange = { onFieldChanged(PrinterSettingField.Port, it) }
            )
            SettingsTextField(
                label = "Printer Queue",
                value = settings.queue,
                error = settings.validation.queue,
                placeholder = "Queue name from CUPS",
                onValueChange = { onFieldChanged(PrinterSettingField.Queue, it) }
            )

            Text(
                text = "Generated Printer URI",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = settings.generatedUri,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("SAVE") }
            OutlinedButton(
                onClick = onResetRequested,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("RESET TO DEFAULTS") }
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    error: String?,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
