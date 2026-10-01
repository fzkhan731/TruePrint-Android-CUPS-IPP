package com.trueprint.app.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trueprint.app.data.PrintHistoryEntry
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun JobHistoryScreen(entries: List<PrintHistoryEntry>, onBack: () -> Unit, onClear: () -> Unit) {
    BackHandler(onBack = onBack); var confirm by remember { mutableStateOf(false) }
    Scaffold(topBar={TopAppBar(title={Text("Job history")},navigationIcon={TextButton(onClick=onBack){Text("BACK")}},actions={if(entries.isNotEmpty())TextButton(onClick={confirm=true}){Text("CLEAR")}})}) { padding ->
        if(entries.isEmpty()) Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),verticalArrangement=Arrangement.Center){Text("No local print history yet.",style=MaterialTheme.typography.bodyLarge)}
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(entries,key={it.timestamp}){entry->ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(entry.fileName,fontWeight=FontWeight.SemiBold);Text("${entry.status} • ${DateFormat.getDateTimeInstance().format(Date(entry.timestamp))}");Text("${entry.copies} copy/copies • ${entry.paperSize} • ${entry.orientation} • ${entry.paperType}");Text("${entry.pageSpecification} • ${entry.pagesPerSheet}-up • ${entry.duplex}"); entry.jobId?.let{Text("CUPS job ID: $it")};entry.failureReason?.let{Text(it,color=MaterialTheme.colorScheme.error)}}}} }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Clear print history?")},text={Text("This removes your local print history. Your documents will not be deleted.")},dismissButton={TextButton(onClick={confirm=false}){Text("CANCEL")}},confirmButton={TextButton(onClick={onClear();confirm=false}){Text("CLEAR")}})
}
