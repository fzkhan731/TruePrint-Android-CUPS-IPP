package com.trueprint.app.ui.print

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trueprint.app.model.DuplexMode
import com.trueprint.app.model.PageSelection
import com.trueprint.app.model.PaperType
import com.trueprint.app.model.PrintOrientation
import com.trueprint.app.viewmodel.MainUiState
import com.trueprint.app.viewmodel.PrintSubmissionState
import com.trueprint.app.viewmodel.SupportedPaperSizes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintWorkflowScreen(uiState: MainUiState, onBack: () -> Unit, onChooseDocument: () -> Unit,
    onCopiesChanged: (Int) -> Unit, onPaperSizeChanged: (String) -> Unit, onAllPages: () -> Unit,
    onCustomPagesChanged: (String) -> Unit, onPagesPerSheetChanged: (Int) -> Unit,
    onDuplexChanged: (DuplexMode) -> Unit, onOrientationChanged: (PrintOrientation) -> Unit, onPaperTypeChanged: (PaperType) -> Unit, onPrint: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    var dialog by remember { mutableStateOf<String?>(null) }
    val active = uiState.printSubmission.active
    Scaffold(modifier = modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(if (uiState.selectedPdf?.isImage == true) "Print image" else "Print PDF") }, navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }) }) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            detail("Document", uiState.selectedPdf?.fileName ?: "No document selected")
            OutlinedButton(onClick = onChooseDocument, enabled = !active, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("CHOOSE DOCUMENT") }
            detail("Printer", uiState.printer.displayName); detail("CUPS server", uiState.printer.serverAddress)
            choice("Paper size", uiState.paperSize, !active) { dialog = "paper" }; copies(uiState.copies, !active, onCopiesChanged)
            choice("Orientation", uiState.printOptions.orientation.label, !active) { dialog = "orientation" }
            choice("Paper type", uiState.printOptions.paperType.label, !active) { dialog = "media" }
            if (uiState.selectedPdf?.isImage != true) pageControls(uiState, active, onAllPages, onCustomPagesChanged)
            choice("Pages per sheet", uiState.printOptions.pagesPerSheet.toString(), !active) { dialog = "nup" }
            choice("Two-sided", uiState.printOptions.duplex.label, !active) { dialog = "duplex" }
            status(uiState.printSubmission)
            Button(onClick = onPrint, enabled = uiState.selectedPdf != null && !active && uiState.pageInputError == null, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(if (active) "SUBMITTING…" else "PRINT") }
        }
    }
    when (dialog) {
        "paper" -> selectionDialog("Paper size", SupportedPaperSizes, uiState.paperSize) { onPaperSizeChanged(it); dialog = null }
        "nup" -> selectionDialog("Pages per sheet", listOf("1", "2", "4"), uiState.printOptions.pagesPerSheet.toString()) { onPagesPerSheetChanged(it.toInt()); dialog = null }
        "duplex" -> selectionDialog("Two-sided", DuplexMode.entries.map { it.label }, uiState.printOptions.duplex.label) { value -> onDuplexChanged(DuplexMode.entries.first { it.label == value }); dialog = null }
        "orientation" -> selectionDialog("Orientation", PrintOrientation.entries.map { it.label }, uiState.printOptions.orientation.label) { value -> onOrientationChanged(PrintOrientation.entries.first { it.label == value }); dialog = null }
        "media" -> selectionDialog("Paper type", PaperType.entries.map { it.label }, uiState.printOptions.paperType.label) { value -> onPaperTypeChanged(PaperType.entries.first { it.label == value }); dialog = null }
        else -> Unit
    }
}

@Composable private fun pageControls(s: MainUiState, active: Boolean, all: () -> Unit, custom: (String) -> Unit) {
    val isCustom = s.printOptions.pageSelection is PageSelection.Custom
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pages", style = MaterialTheme.typography.labelLarge)
        Row { TextButton(onClick = all, enabled = !active) { Text(if (!isCustom) "✓ ALL" else "ALL") }; TextButton(onClick = { if (!isCustom) custom(s.pageInput) }, enabled = !active) { Text(if (isCustom) "✓ CUSTOM" else "CUSTOM") } }
        if (isCustom) OutlinedTextField(value = s.pageInput, onValueChange = custom, enabled = !active, isError = s.pageInputError != null, label = { Text("Pages, for example 1-3,7") }, supportingText = { Text(s.pageInputError ?: s.selectedPdf?.pageCount?.let { "$it pages" } ?: "Checking page count…") }, modifier = Modifier.fillMaxWidth())
    } }
}
@Composable private fun selectionDialog(title: String, values: List<String>, selected: String, pick: (String) -> Unit) = AlertDialog(onDismissRequest = {}, title = { Text(title) }, text = { Column { values.forEach { value -> TextButton(onClick = { pick(value) }, modifier = Modifier.fillMaxWidth()) { Text(if (value == selected) "✓ $value" else value) } } } }, confirmButton = {})
@Composable private fun detail(title: String, value: String) { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) } } }
@Composable private fun choice(title: String, value: String, enabled: Boolean, click: () -> Unit) { ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(title, style = MaterialTheme.typography.labelLarge); Text(value, style = MaterialTheme.typography.titleMedium) }; TextButton(onClick = click, enabled = enabled) { Text("CHANGE") } } } }
@Composable private fun copies(value: Int, enabled: Boolean, change: (Int) -> Unit) { ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Copies", style = MaterialTheme.typography.titleMedium); Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { change(value - 1) }, enabled = enabled && value > 1) { Text("−") }; Text(value.toString(), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge); TextButton(onClick = { change(value + 1) }, enabled = enabled && value < 99) { Text("+") } } } } }
@Composable private fun status(s: PrintSubmissionState) { val message = when (s) { PrintSubmissionState.Ready -> "Ready to submit this print job."; PrintSubmissionState.PreparingDocument -> "Preparing document…"; PrintSubmissionState.Connecting -> "Connecting to CUPS…"; PrintSubmissionState.Sending -> "Sending print job…"; is PrintSubmissionState.Submitted -> "Print job submitted.${s.jobId?.let { " Job ID: $it." }.orEmpty()} CUPS acceptance does not confirm physical completion."; is PrintSubmissionState.Failed -> s.message }; Row(verticalAlignment = Alignment.CenterVertically) { if (s.active) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Text(message, Modifier.padding(start = if (s.active) 10.dp else 0.dp), color = if (s is PrintSubmissionState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } }
private val PrintSubmissionState.active get() = this is PrintSubmissionState.PreparingDocument || this is PrintSubmissionState.Connecting || this is PrintSubmissionState.Sending
