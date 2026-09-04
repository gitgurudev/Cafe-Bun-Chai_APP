package com.cafebunchai.pos.ui.menu

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.ui.util.paiseToRupeeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(vm: MenuViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val grouped = remember(state.items, state.categories) {
        val byId = state.categories.associateBy { it.id }
        val sections = state.categories
            .sortedBy { it.sortOrder }
            .map { cat ->
                cat to state.items.filter { it.categoryId == cat.id }.sortedBy { it.name.lowercase() }
            }
            .filter { it.second.isNotEmpty() }
        val leftover = state.items.filter { it.categoryId !in byId }
        if (leftover.isEmpty()) sections
        else sections + (Category("other", "Other", Int.MAX_VALUE) to leftover.sortedBy { it.name.lowercase() })
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Menu & prices", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = vm::startNew) { Text("Add item") }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            grouped.forEach { (category, items) ->
                item(key = "cat-${category.id}") {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
                    )
                }
                items(items, key = { it.id }) { item ->
                    MenuListRow(item = item, onClick = { vm.startEdit(item) })
                }
            }
        }
    }

    val editor = state.editor
    if (editor != null) {
        MenuEditorDialog(
            editor = editor,
            categories = state.categories,
            onChange = vm::updateEditor,
            onSave = vm::save,
            isExisting = state.items.any { it.id == editor.id },
            onDelete = {
                vm.delete(editor.id)
                vm.closeEditor()
            },
            onDismiss = vm::closeEditor,
        )
    }
}

@Composable
private fun MenuListRow(item: MenuItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    item.noteHint?.ifBlank { null } ?: " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.noteHint.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                paiseToRupeeLabel(item.pricePaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuEditorDialog(
    editor: MenuEditor,
    categories: List<Category>,
    isExisting: Boolean,
    onChange: ((MenuEditor) -> MenuEditor) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var catOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.name.isBlank()) "New item" else editor.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(expanded = catOpen, onExpandedChange = { catOpen = it }) {
                    val catName = categories.find { it.id == editor.categoryId }?.name.orEmpty()
                    OutlinedTextField(
                        value = catName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catOpen) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    onChange { it.copy(categoryId = c.id) }
                                    catOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = editor.priceRupees,
                    onValueChange = { v -> onChange { it.copy(priceRupees = v) } },
                    label = { Text("Price (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.noteHint,
                    onValueChange = { v -> onChange { it.copy(noteHint = v) } },
                    label = { Text("Note (optional, e.g. flavour)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = editor.name.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (isExisting) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this item?") },
            text = {
                Text(
                    "“${editor.name.ifBlank { "This item" }}” will be removed from the menu on every phone. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep it") }
            },
        )
    }
}
