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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderType
import com.cafebunchai.pos.data.model.Payment
import com.cafebunchai.pos.notify.StockAlerts
import com.cafebunchai.pos.ui.util.formatTime
import com.cafebunchai.pos.ui.util.paiseToRupeeLabel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OrderScreen(vm: OrderViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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

    LaunchedEffect(state.refillAlerts) {
        if (state.refillAlerts.isEmpty()) return@LaunchedEffect
        StockAlerts.notifyRefill(context, state.refillAlerts)
        vm.clearRefillAlerts()
    }

    LaunchedEffect(cartQty) {
        if (cartQty == 0) return@LaunchedEffect
        pulse = 1.07f
        delay(180)
        pulse = 1f
    }

    Column(Modifier.fillMaxSize()) {
        if (state.unpaidOrders.isNotEmpty() || !state.composing) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.unpaidOrders.isNotEmpty()) {
                    FilterChip(
                        selected = !state.composing,
                        onClick = vm::showUnpaidList,
                        label = { Text("Unpaid (${state.unpaidOrders.size})") },
                    )
                }
                if (!state.composing) {
                    Button(onClick = vm::newOrder) { Text("New order") }
                }
            }
        }

        if (!state.composing) {
            UnpaidTicketList(
                orders = state.unpaidOrders,
                onOpen = vm::openUnpaid,
                onNewOrder = vm::newOrder,
            )
        } else {
            ComposeOrder(
                vm = vm,
                state = state,
                cartQty = cartQty,
                cartScale = cartScale,
            )
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

@Composable
private fun UnpaidTicketList(
    orders: List<Order>,
    onOpen: (Order) -> Unit,
    onNewOrder: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Open tickets", style = MaterialTheme.typography.titleMedium)
        }
        if (orders.isEmpty()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Koi unpaid ticket nahi hai.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onNewOrder, modifier = Modifier.fillMaxWidth()) {
                    Text("New order")
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(orders, key = { it.id }) { order ->
                    Card(
                        onClick = { onOpen(order) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    order.tableNote.ifBlank {
                                        if (order.type == OrderType.DINE_IN) "Dine in" else "Takeaway"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(formatTime(order.createdAt), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                order.lines.joinToString { "${it.qty}× ${it.nameSnapshot}" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                paiseToRupeeLabel(order.totalPaise),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ComposeOrder(
    vm: OrderViewModel,
    state: OrderUiState,
    cartQty: Int,
    cartScale: Float,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.unpaidOrders.isNotEmpty()) {
                TextButton(onClick = vm::showUnpaidList) { Text("Unpaid tickets") }
            }
            Text(
                if (state.editingUnpaid) "Edit unpaid" else "New order",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
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
            items(state.visibleCategories, key = { it.id }) { cat ->
                FilterChip(
                    selected = state.selectedCategoryId == cat.id,
                    onClick = { vm.selectCategory(cat.id) },
                    label = { Text(cat.name) },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.visibleItems.isEmpty()) {
                item {
                    Text(
                        "Is category me abhi sellable item nahi hai — stock khatam.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
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
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .scale(cartScale),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (cartQty == 0) "Cart" else "Cart · $cartQty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        paiseToRupeeLabel(state.cartTotalPaise),
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (state.cart.isEmpty()) {
                    Text(
                        "Tap an item to add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(state.cart, key = { "cart-${it.menuItem.id}-${it.note}" }) { line ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${line.menuItem.name} × ${line.qty}",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                TextButton(onClick = { vm.changeQty(line.menuItem.id, line.note, -1) }) {
                                    Text("−")
                                }
                                TextButton(
                                    onClick = { vm.changeQty(line.menuItem.id, line.note, 1) },
                                    enabled = state.canSell(line.menuItem),
                                ) {
                                    Text("+")
                                }
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
                    label = { Text("Table / name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    FilterChip(
                        selected = state.payment == Payment.UNPAID,
                        onClick = { vm.setPayment(Payment.UNPAID) },
                        label = { Text("Unpaid") },
                    )
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
                }
                val actionLabel = when {
                    state.editingUnpaid && state.cart.isEmpty() -> "Withdraw ticket"
                    state.payment == Payment.UNPAID && state.editingUnpaid -> "Update unpaid"
                    state.payment == Payment.UNPAID -> "Save unpaid"
                    state.editingUnpaid -> "Collect ${paiseToRupeeLabel(state.cartTotalPaise)}"
                    else -> "Complete · ${paiseToRupeeLabel(state.cartTotalPaise)}"
                }
                Button(
                    onClick = vm::tryComplete,
                    enabled = !state.busy && (state.cart.isNotEmpty() || state.editingUnpaid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
