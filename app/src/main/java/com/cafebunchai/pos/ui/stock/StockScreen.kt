package com.cafebunchai.pos.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.ui.theme.LowStock
import com.cafebunchai.pos.ui.util.qtyLabel

private val units = listOf("pcs", "packs", "kg", "litre")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(vm: StockViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Kitchen stock", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = vm::startNew) { Text("Add item") }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                StockListRow(item = item, onClick = { vm.openEdit(item) })
            }
        }
    }

    val editor = state.editor
    if (editor != null) {
        AlertDialog(
            onDismissRequest = vm::close,
            title = { Text(if (editor.name.isBlank()) "New stock" else editor.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = { v -> vm.update { it.copy(name = v) } },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        units.take(2).forEach { unit ->
                            FilterChip(
                                selected = editor.unit == unit,
                                onClick = { vm.update { it.copy(unit = unit) } },
                                label = { Text(unit) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        units.drop(2).forEach { unit ->
                            FilterChip(
                                selected = editor.unit == unit,
                                onClick = { vm.update { it.copy(unit = unit) } },
                                label = { Text(unit) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editor.qty,
                        onValueChange = { v -> vm.update { it.copy(qty = v) } },
                        label = { Text("Qty on hand") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editor.low,
                        onValueChange = { v -> vm.update { it.copy(low = v) } },
                        label = { Text("Low-stock alert") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = vm::save, enabled = editor.name.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = vm::close) { Text("Close") } },
        )
    }
}

@Composable
private fun StockListRow(item: InventoryItem, onClick: () -> Unit) {
    val low = item.isLow
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (low) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (low) "Refill — alert at ${qtyLabel(item.lowStockThreshold, item.unit)}" else " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (low) LowStock else MaterialTheme.colorScheme.onSurface.copy(alpha = 0f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                qtyLabel(item.qtyOnHand, item.unit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                color = if (low) LowStock else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}
