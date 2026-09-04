package com.cafebunchai.pos.ui.order

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.data.model.OrderType
import com.cafebunchai.pos.data.model.Payment
import com.cafebunchai.pos.ui.util.paiseToRupeeLabel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OrderScreen(vm: OrderViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val cartQty = state.cart.sumOf { it.qty }
    var pulse by remember { mutableFloatStateOf(1f) }
    val cartScale by animateFloatAsState(
        targetValue = pulse,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "cartPulse",
    )

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snack.showSnackbar(msg)
        vm.clearMessage()
    }

    LaunchedEffect(cartQty) {
        if (cartQty == 0) return@LaunchedEffect
        pulse = 1.07f
        delay(180)
        pulse = 1f
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "CHAI, BUN & GOOD VIBES",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { vm.selectCategory(null) },
                    label = { Text("All") },
                )
            }
            items(state.categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = state.selectedCategoryId == cat.id,
                    onClick = { vm.selectCategory(cat.id) },
                    label = { Text(cat.name) },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.visibleItems, key = { "menu-${it.id}" }) { item ->
                Card(
                    onClick = { vm.addItem(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            if (item.isCombo) {
                                Text(
                                    "Combo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            if (item.noteHint != null) {
                                Text(item.noteHint, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(paiseToRupeeLabel(item.pricePaise), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .scale(cartScale),
        ) {
            Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (cartQty == 0) "Cart" else "Cart · $cartQty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    paiseToRupeeLabel(state.cartTotalPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (state.cart.isEmpty()) {
                Text(
                    "Tap a drink or bun to add it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 132.dp)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(state.cart, key = { "cart-${it.menuItem.id}-${it.note}" }) { line ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${line.menuItem.name} × ${line.qty}")
                                if (line.note.isNotBlank()) {
                                    Text(line.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            TextButton(onClick = { vm.changeQty(line.menuItem.id, line.note, -1) }) {
                                Text("−")
                            }
                            TextButton(onClick = { vm.changeQty(line.menuItem.id, line.note, 1) }) {
                                Text("+")
                            }
                            Text(paiseToRupeeLabel(line.lineTotalPaise))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == OrderType.TAKEAWAY,
                    onClick = { vm.setType(OrderType.TAKEAWAY) },
                    label = { Text("Takeaway") },
                )
                FilterChip(
                    selected = state.type == OrderType.DINE_IN,
                    onClick = { vm.setType(OrderType.DINE_IN) },
                    label = { Text("Dine in") },
                )
            }
            OutlinedTextField(
                value = state.tableNote,
                onValueChange = vm::setTableNote,
                label = { Text("Table / name (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilterChip(
                    selected = state.payment == Payment.CASH,
                    onClick = { vm.setPayment(Payment.CASH) },
                    label = { Text("Cash") },
                )
                FilterChip(
                    selected = state.payment == Payment.UPI,
                    onClick = { vm.setPayment(Payment.UPI) },
                    label = { Text("UPI") },
                )
                FilterChip(
                    selected = state.payment == Payment.UNPAID,
                    onClick = { vm.setPayment(Payment.UNPAID) },
                    label = { Text("Unpaid") },
                )
            }
            Button(
                onClick = vm::tryComplete,
                enabled = !state.busy && state.cart.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text("Complete · ${paiseToRupeeLabel(state.cartTotalPaise)}")
            }
            }
        }
        SnackbarHost(snack)
    }

    if (state.noteItemId != null) {
        AlertDialog(
            onDismissRequest = vm::cancelNote,
            title = { Text("Icy Pops flavour") },
            text = {
                OutlinedTextField(
                    value = state.noteDraft,
                    onValueChange = vm::setNoteDraft,
                    label = { Text("e.g. Mango") },
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmNote) { Text("Add") } },
            dismissButton = { TextButton(onClick = vm::cancelNote) { Text("Cancel") } },
        )
    }
}
