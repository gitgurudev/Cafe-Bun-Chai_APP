package com.cafebunchai.pos.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MenuEditor(
    val id: String,
    val name: String,
    val categoryId: String,
    val priceRupees: String,
    val noteHint: String,
)

data class MenuUiState(
    val categories: List<Category> = emptyList(),
    val items: List<MenuItem> = emptyList(),
    val editor: MenuEditor? = null,
)

class MenuViewModel(
    private val repo: OrderInventoryRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<MenuEditor?>(null)

    val state: StateFlow<MenuUiState> = combine(
        repo.observeCategories(),
        repo.observeMenuItems(),
        editor,
    ) { cats, items, ed ->
        MenuUiState(cats, items, ed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())

    fun startNew() {
        val cat = state.value.categories.firstOrNull()?.id.orEmpty()
        editor.value = MenuEditor(
            id = UUID.randomUUID().toString(),
            name = "",
            categoryId = cat,
            priceRupees = "",
            noteHint = "",
        )
    }

    fun startEdit(item: MenuItem) {
        editor.value = MenuEditor(
            id = item.id,
            name = item.name,
            categoryId = item.categoryId,
            priceRupees = (item.pricePaise / 100.0).toString(),
            noteHint = item.noteHint.orEmpty(),
        )
    }

    fun updateEditor(transform: (MenuEditor) -> MenuEditor) {
        val cur = editor.value ?: return
        editor.value = transform(cur)
    }

    fun closeEditor() {
        editor.value = null
    }

    fun save() {
        val ed = editor.value ?: return
        val rupees = ed.priceRupees.toDoubleOrNull() ?: return
        viewModelScope.launch {
            repo.upsertMenuItem(
                MenuItem(
                    id = ed.id,
                    name = ed.name.trim(),
                    categoryId = ed.categoryId,
                    pricePaise = (rupees * 100).toInt(),
                    isAvailable = true,
                    isCombo = false,
                    noteHint = ed.noteHint.ifBlank { null },
                ),
            )
            editor.value = null
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteMenuItem(id) }
    }

    companion object {
        fun factory(repo: OrderInventoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MenuViewModel(repo) as T
        }
    }
}
