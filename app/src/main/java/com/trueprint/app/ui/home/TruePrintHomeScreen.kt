package com.trueprint.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trueprint.app.model.PrinterProfile
import com.trueprint.app.R
import com.trueprint.app.ui.theme.TruePrintTheme
import com.trueprint.app.viewmodel.HomeAction
import com.trueprint.app.viewmodel.ConnectionTestState
import com.trueprint.app.viewmodel.MainUiState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TruePrintHomeScreen(
    uiState: MainUiState,
    onActionSelected: (HomeAction) -> Unit,
    onNotificationShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSetupGuide by rememberSaveable { mutableStateOf(!uiState.printer.isConfigured) }

    LaunchedEffect(uiState.notification) {
        uiState.notification?.let { message ->
            snackbarHostState.showSnackbar(message)
            onNotificationShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.ic_trueprint_printer), contentDescription = "TruePrint printer", modifier = Modifier.height(28.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("TruePrint Dev") } }) },
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
                text = "Simple private printing through your configured CUPS server.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.printer.isConfigured) {
                PrinterStatusCard(printer = uiState.printer, connection = uiState.connection)
            } else {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No printer configured", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Connect TruePrint to your CUPS printer to start printing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { onActionSelected(HomeAction.Settings) }, modifier = Modifier.fillMaxWidth()) { Text("SET UP PRINTER") }
                    }
                }
            }

            Text(
                text = if (uiState.printer.isConfigured) "Start printing" else "After setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ActionButton(
                text = "SELECT DOCUMENT",
                onClick = { onActionSelected(HomeAction.PrintPdf) }
            )

            uiState.selectedPdf?.let { pdf ->
                SelectedPdfCard(
                    fileName = pdf.fileName
                )
            }

            ActionButton(
                text = "PRINT IMAGE",
                onClick = { onActionSelected(HomeAction.PrintImage) },
                outlined = true
            )

            Text(
                text = "Printer connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ActionButton(
                text = "TEST CONNECTION",
                onClick = { onActionSelected(HomeAction.TestConnection) },
                outlined = true
            )

            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onActionSelected(HomeAction.Settings) }
                ) { Text("SETTINGS") }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onActionSelected(HomeAction.JobHistory) }
                ) { Text("JOB HISTORY") }
            }
        }
    }

    if (!uiState.printer.isConfigured && showSetupGuide) {
        AlertDialog(
            onDismissRequest = { showSetupGuide = false },
            title = { Text("Set up your CUPS printer") },
            text = {
                Text(
                    "Open Printer Settings to enter your printer name, CUPS server address, port, and queue. " +
                        "You can test the connection before saving."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showSetupGuide = false
                    onActionSelected(HomeAction.Settings)
                }) { Text("SET UP PRINTER") }
            },
            dismissButton = {
                TextButton(onClick = { showSetupGuide = false }) { Text("NOT NOW") }
            }
        )
    }
}

@Composable
private fun PrinterStatusCard(printer: PrinterProfile, connection: ConnectionTestState) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = "Connection status: ${connection.label}" },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Default printer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = (connection as? ConnectionTestState.Connected)?.attributes?.printerName
                    ?: printer.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            StatusRow(connection.label)
            DetailRow(label = "Protocol", value = printer.protocol)
            DetailRow(label = "CUPS server", value = printer.serverAddress)
            DetailRow(label = "Printer queue", value = printer.queue)
            ConnectionDetails(connection)
        }
    }
}

@Composable
private fun SelectedPdfCard(fileName: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Selected PDF",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ConnectionDetails(connection: ConnectionTestState) {
    when (connection) {
        ConnectionTestState.Idle -> Unit
        ConnectionTestState.Testing -> Text(
            text = "Querying CUPS with IPP Get-Printer-Attributes…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        is ConnectionTestState.Connected -> {
            val attributes = connection.attributes
            attributes.printerState?.let { DetailRow("Printer state", it) }
            attributes.acceptingJobs?.let { DetailRow("Accepting jobs", if (it) "Yes" else "No") }
            attributes.printerInfo?.let { DetailRow("Printer info", it) }
            attributes.printerMakeAndModel?.let { DetailRow("Make and model", it) }
            if (attributes.printerStateReasons.isNotEmpty()) {
                DetailRow("State reasons", attributes.printerStateReasons.joinToString())
            }
        }
        is ConnectionTestState.Failed -> Text(
            text = connection.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private val ConnectionTestState.label: String
    get() = when (this) {
        ConnectionTestState.Idle -> "Not tested"
        ConnectionTestState.Testing -> "Testing..."
        is ConnectionTestState.Connected -> "Connected"
        is ConnectionTestState.Failed -> "Connection failed"
    }

@Composable
private fun StatusRow(status: String) {
    val (container, content) = when (status) {
        "Connected" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "Connection failed" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Connection status", style = MaterialTheme.typography.bodyMedium)
        Surface(
            color = container,
            contentColor = content,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = status,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(text) }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(text) }
    }
}

@Preview(showBackground = true)
@Composable
private fun TruePrintHomeScreenLightPreview() {
    TruePrintTheme(darkTheme = false) {
        TruePrintHomeScreen(
            uiState = MainUiState(printer = PrinterProfile.Default),
            onActionSelected = {},
            onNotificationShown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TruePrintHomeScreenDarkPreview() {
    TruePrintTheme(darkTheme = true) {
        TruePrintHomeScreen(
            uiState = MainUiState(printer = PrinterProfile.Default),
            onActionSelected = {},
            onNotificationShown = {}
        )
    }
}
