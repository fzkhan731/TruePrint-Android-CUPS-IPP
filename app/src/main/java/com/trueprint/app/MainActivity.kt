package com.trueprint.app

import android.os.Bundle
import android.content.ContentResolver
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.trueprint.app.data.DataStorePrinterProfileRepository
import com.trueprint.app.data.DataStoreJobHistoryRepository
import com.trueprint.app.network.IppClient
import com.trueprint.app.ui.home.TruePrintHomeScreen
import com.trueprint.app.ui.settings.PrinterSettingsScreen
import com.trueprint.app.ui.print.PrintWorkflowScreen
import com.trueprint.app.ui.theme.TruePrintTheme
import com.trueprint.app.viewmodel.AppDestination
import com.trueprint.app.viewmodel.MainViewModel
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.trueprint.app.viewmodel.HomeAction
import com.trueprint.app.model.DocumentTypeResolver

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    printerProfileRepository = DataStorePrinterProfileRepository(applicationContext),
                    ippClient = IppClient(),
                    context = applicationContext,
                    historyRepository = DataStoreJobHistoryRepository(applicationContext)
                ) as T
            }
        }
    }
    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                runCatching {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val fileName = getFileName(it)
                val type = DocumentTypeResolver.resolve(contentResolver.getType(it), null, fileName)
                if (type == null) viewModel.onUnsupportedSharedDocument()
                else viewModel.onDocumentSelected(it.toString(), fileName, type.canonicalMimeType)
            }
        }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    return it.getString(index)
                }
            }
        }

        return "Selected PDF"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        setContent {
            val uiState by viewModel.uiState

            TruePrintTheme {
                when (uiState.destination) {
                    AppDestination.Home -> TruePrintHomeScreen(
                        uiState = uiState,
                        onActionSelected = { action ->
                            if (action == HomeAction.PrintPdf || action == HomeAction.PrintImage) {
                                documentPicker.launch(if (action == HomeAction.PrintImage) arrayOf("image/jpeg", "image/png") else arrayOf("application/pdf"))
                            } else {
                                viewModel.onActionSelected(action)
                            }
                        },
                        onNotificationShown = viewModel::onNotificationShown
                    )

                    AppDestination.Print -> PrintWorkflowScreen(
                        uiState = uiState,
                        onBack = viewModel::navigateHome,
                        onChooseDocument = {
                            documentPicker.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                        },
                        onCopiesChanged = viewModel::updateCopies,
                        onPaperSizeChanged = viewModel::updatePaperSize,
                        onAllPages = viewModel::selectAllPages,
                        onCustomPagesChanged = viewModel::updateCustomPages,
                        onPagesPerSheetChanged = viewModel::updatePagesPerSheet,
                        onDuplexChanged = viewModel::updateDuplex,
                        onOrientationChanged = viewModel::updateOrientation,
                        onPaperTypeChanged = viewModel::updatePaperType,
                        onPrint = viewModel::printSelectedPdf
                    )

                    AppDestination.Settings -> PrinterSettingsScreen(
                        settings = uiState.settings,
                        notification = uiState.notification,
                        onBack = viewModel::navigateHome,
                        onFieldChanged = viewModel::onSettingsFieldChanged,
                        onSave = viewModel::saveSettings,
                        onResetRequested = viewModel::requestReset,
                        onResetDismissed = viewModel::dismissReset,
                        onResetConfirmed = viewModel::resetToDefaults,
                        onNotificationShown = viewModel::onNotificationShown
                    )

                    AppDestination.History -> com.trueprint.app.ui.history.JobHistoryScreen(
                        entries = uiState.history,
                        onBack = viewModel::navigateHome,
                        onClear = viewModel::clearHistory
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) {
            if (intent?.action == Intent.ACTION_SEND) viewModel.onUnsupportedSharedDocument()
            return
        }

        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri?.scheme != ContentResolver.SCHEME_CONTENT) {
            viewModel.onUnsupportedSharedDocument()
            return
        }

        val fileName = getFileName(uri)
        val type = DocumentTypeResolver.resolve(contentResolver.getType(uri), intent.type, fileName)
        if (type == null) {
            viewModel.onUnsupportedSharedDocument()
            return
        }
        viewModel.onDocumentSelected(uri.toString(), fileName, type.canonicalMimeType)
    }
}
