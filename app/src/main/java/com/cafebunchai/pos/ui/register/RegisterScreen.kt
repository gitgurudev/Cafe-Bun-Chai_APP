package com.cafebunchai.pos.ui.register

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.CafeBunChaiApp
import com.cafebunchai.pos.data.export.ExcelRegisterExport
import com.cafebunchai.pos.data.export.PdfRegisterExport
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.ui.util.formatDate
import com.cafebunchai.pos.ui.util.formatTime
import com.cafebunchai.pos.ui.util.paiseToRupeeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(vm: RegisterViewModel, isAdmin: Boolean = true) {
    val state by vm.state.collectAsStateWithLifecycle()
    var cancelId by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var importConfirm by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var pickField by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val backup = (context.applicationContext as CafeBunChaiApp).container.backup
    val scope = rememberCoroutineScope()

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = backup.exportJson()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            banner = "JSON backup saved"
        }
    }

    val exportExcelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val from = state.from
        val to = state.to
        val orders = state.orders
        scope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    ExcelRegisterExport.write(out, from, to, orders)
                }
            }
            banner = "Excel saved"
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val from = state.from
        val to = state.to
        val orders = state.orders
        scope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfRegisterExport.write(out, from, to, orders)
                }
            }
            banner = "PDF saved"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        pendingImport = uri
        if (uri != null) importConfirm = true
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.preset == RegisterPreset.TODAY,
                    onClick = vm::presetToday,
                    label = { Text("Today") },
                )
            }
            item {
                FilterChip(
                    selected = state.preset == RegisterPreset.WEEK,
                    onClick = vm::presetWeek,
                    label = { Text("This week") },
                )
            }
            item {
                FilterChip(
                    selected = state.preset == RegisterPreset.MONTH,
                    onClick = vm::presetMonth,
                    label = { Text("This month") },
                )
            }
            item {
                FilterChip(
                    selected = state.preset == RegisterPreset.YEAR,
                    onClick = vm::presetYear,
                    label = { Text("This year") },
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.payFilter == RegisterPayFilter.ALL,
                    onClick = { vm.setPayFilter(RegisterPayFilter.ALL) },
                    label = { Text("All") },
                )
            }
            item {
                FilterChip(
                    selected = state.payFilter == RegisterPayFilter.UNPAID,
                    onClick = { vm.setPayFilter(RegisterPayFilter.UNPAID) },
                    label = { Text("Unpaid") },
                )
            }
            item {
                FilterChip(
                    selected = state.payFilter == RegisterPayFilter.PAID,
                    onClick = { vm.setPayFilter(RegisterPayFilter.PAID) },
                    label = { Text("Paid") },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (state.from == state.to) {
                TextButton(onClick = vm::prevDay) { Text("◀") }
            } else {
                TextButton(onClick = {}, enabled = false) { Text(" ") }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { pickField = "from" }) { Text("From ${formatDate(state.from)}") }
                TextButton(onClick = { pickField = "to" }) { Text("To ${formatDate(state.to)}") }
            }
            if (state.from == state.to) {
                TextButton(onClick = vm::nextDay) { Text("▶") }
            } else {
                TextButton(onClick = {}, enabled = false) { Text(" ") }
            }
        }
        Card(
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Register total", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (state.ready) paiseToRupeeLabel(state.dayTotalPaise) else "—",
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (state.ready) {
                    Text("Cash ${paiseToRupeeLabel(state.cashPaise)}  ·  UPI ${paiseToRupeeLabel(state.upiPaise)}")
                    Text("Unpaid ${paiseToRupeeLabel(state.unpaidPaise)}  ·  ${state.completed.size} orders")
                }
            }
        }
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { exportExcelLauncher.launch("cafe-register-${state.from}_to_${state.to}.xlsx") },
            ) { Text("Excel") }
            OutlinedButton(
                onClick = { exportPdfLauncher.launch("cafe-register-${state.from}_to_${state.to}.pdf") },
            ) { Text("PDF") }
            if (isAdmin) {
                OutlinedButton(onClick = { exportJsonLauncher.launch("cafe-bun-chai-backup.json") }) {
                    Text("JSON")
                }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) {
                    Text("Import")
                }
            }
        }
        if (banner != null) {
            Text(banner!!, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.secondary)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.ready && state.orders.isEmpty()) {
                item { Text("No orders in this date range.") }
            }
            if (state.ready) {
                items(state.orders, key = { it.id }) { order ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatTime(order.createdAt), fontWeight = FontWeight.SemiBold)
                                Text(paiseToRupeeLabel(order.totalPaise), fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${order.type.replace('_', ' ')} · ${order.payment}" +
                                    if (order.tableNote.isNotBlank()) " · ${order.tableNote}" else "",
                            )
                            order.lines.forEach { line ->
                                val extra = if (line.lineNote.isNullOrBlank()) "" else " (${line.lineNote})"
                                Text("  ${line.qty} × ${line.nameSnapshot}$extra")
                            }
                            if (order.status == OrderStatus.CANCELLED) {
                                Text("Cancelled: ${order.cancelReason.orEmpty()}", color = MaterialTheme.colorScheme.error)
                            } else if (isAdmin) {
                                TextButton(onClick = { cancelId = order.id; cancelReason = "" }) {
                                    Text("Cancel order")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickField != null) {
        val initial = if (pickField == "from") state.from else state.to
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickField = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ms = pickerState.selectedDateMillis
                        if (ms != null) {
                            val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                            if (pickField == "from") vm.setFrom(date) else vm.setTo(date)
                        }
                        pickField = null
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickField = null }) { Text("Back") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (cancelId != null) {
        AlertDialog(
            onDismissRequest = { cancelId = null },
            title = { Text("Cancel this order?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This cannot be undone. The order stays in the register as cancelled.")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.cancel(cancelId!!, cancelReason.ifBlank { "Cancelled" })
                        cancelId = null
                    },
                ) { Text("Cancel order") }
            },
            dismissButton = { TextButton(onClick = { cancelId = null }) { Text("Back") } },
        )
    }

    if (importConfirm && pendingImport != null) {
        AlertDialog(
            onDismissRequest = { importConfirm = false; pendingImport = null },
            title = { Text("Replace all local data?") },
            text = { Text("This cannot be undone. Import overwrites the menu and register on this phone, then updates cloud.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImport!!
                        importConfirm = false
                        pendingImport = null
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                            }
                            if (text != null) {
                                backup.importJson(text)
                                runCatching {
                                    (context.applicationContext as CafeBunChaiApp).container.repository.pushLocalToCloud()
                                }
                                banner = "Import done · cloud updated"
                            }
                        }
                    },
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { importConfirm = false; pendingImport = null }) { Text("Back") }
            },
        )
    }
}
