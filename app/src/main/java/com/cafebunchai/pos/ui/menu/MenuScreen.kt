package com.cafebunchai.pos.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.ui.util.paiseToRupeeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(vm: MenuViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Menu & prices", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = vm::startNew) { Text("Add item") }
        }
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.id }) { item ->
                val cat = state.categories.find { it.id == item.categoryId }?.name.orEmpty()
                Card(onClick = { vm.startEdit(item) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("$cat · ${paiseToRupeeLabel(item.pricePaise)}")
                        if (!item.noteHint.isNullOrBlank()) {
                            Text(item.noteHint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
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
            onDelete = {
                vm.delete(editor.id)
                vm.closeEditor()
            },
            onDismiss = vm::closeEditor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuEditorDialog(
    editor: MenuEditor,
    categories: List<Category>,
    onChange: ((MenuEditor) -> MenuEditor) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var catOpen by remember { mutableStateOf(false) }
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
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}
