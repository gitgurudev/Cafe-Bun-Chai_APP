package com.cafebunchai.pos.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafebunchai.pos.R
import com.cafebunchai.pos.ui.util.formatTime
import com.cafebunchai.pos.ui.util.paiseToRupeeLabel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    onOpenRegister: () -> Unit,
    onOpenOrder: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_cafe_bun_chai),
            contentDescription = "Cafe Bun Chai",
            modifier = Modifier.size(132.dp),
            contentScale = ContentScale.Fit,
        )
        Text("Today at the counter", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Sales today", style = MaterialTheme.typography.labelLarge)
                Text(paiseToRupeeLabel(state.todayTotalPaise), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text("${state.orderCount} orders  ·  avg ${paiseToRupeeLabel(state.avgOrderPaise)}")
                Text(
                    "Cash ${paiseToRupeeLabel(state.cashPaise)}   UPI ${paiseToRupeeLabel(state.upiPaise)}   Unpaid ${paiseToRupeeLabel(state.unpaidPaise)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HighlightCard(
                modifier = Modifier.weight(1f),
                label = "Top seller",
                title = state.topBySales?.name ?: "—",
                detail = state.topBySales?.let {
                    "${pct(it.share)} · ${paiseToRupeeLabel(it.paise)}"
                } ?: "No sales yet",
            )
            HighlightCard(
                modifier = Modifier.weight(1f),
                label = "Most ordered",
                title = state.topByQty?.name ?: "—",
                detail = state.topByQty?.let { "${it.qty} cups / pcs" } ?: "No sales yet",
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Payment mix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ShareBar("Cash", state.cashPaise, state.todayTotalPaise)
                ShareBar("UPI", state.upiPaise, state.todayTotalPaise)
                ShareBar("Unpaid", state.unpaidPaise, state.todayTotalPaise)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What's selling", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (state.items.isEmpty()) {
                    Text("Complete an order to see item share.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.items.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(pct(item.share), color = MaterialTheme.colorScheme.secondary)
                            }
                            LinearProgressIndicator(
                                progress = { item.share.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${item.qty} sold  ·  ${paiseToRupeeLabel(item.paise)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onOpenOrder, modifier = Modifier.weight(1f)) { Text("New order") }
            OutlinedButton(onClick = onOpenRegister, modifier = Modifier.weight(1f)) { Text("Register") }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Latest orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (state.recent.isEmpty()) {
                    Text("No orders yet today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.recent.forEach { order ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${formatTime(order.createdAt)} · ${order.payment}", fontWeight = FontWeight.Medium)
                                Text(
                                    order.lines.joinToString { "${it.qty}× ${it.nameSnapshot}" },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(paiseToRupeeLabel(order.totalPaise), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HighlightCard(
    modifier: Modifier,
    label: String,
    title: String,
    detail: String,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ShareBar(label: String, paise: Int, total: Int) {
    val share = if (total <= 0) 0f else paise.toFloat() / total.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${pct(share)}  ${paiseToRupeeLabel(paise)}")
        }
        LinearProgressIndicator(
            progress = { share.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun pct(share: Float): String = "${(share * 100).toInt()}%"
