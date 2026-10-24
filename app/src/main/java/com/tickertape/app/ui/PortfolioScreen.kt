package com.tickertape.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickertape.app.LocalRepository
import com.tickertape.app.data.Holding
import com.tickertape.app.data.Order
import com.tickertape.app.data.Repository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PortfolioState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val holdings: List<Holding> = emptyList(),
    val orders: List<Order> = emptyList(),
)

class PortfolioViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repo.url.collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            val hasRows = _state.value.holdings.isNotEmpty()
            _state.update { it.copy(loading = !hasRows, refreshing = hasRows, error = null) }
            try {
                val loaded = coroutineScope {
                    val holdings = async { repo.holdings() }
                    val orders = async { repo.orders() }
                    holdings.await() to orders.await()
                }
                _state.update {
                    it.copy(loading = false, refreshing = false, holdings = loaded.first, orders = loaded.second)
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(loading = false, refreshing = false, error = error.message) }
            }
        }
    }

    companion object {
        fun factory(repo: Repository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PortfolioViewModel(repo) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(currentRoute: String?, onTab: (String) -> Unit, onSettings: () -> Unit) {
    val repo = LocalRepository.current
    val vm: PortfolioViewModel = viewModel(factory = PortfolioViewModel.factory(repo))
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val shown = state.holdings.filter { holding ->
        val q = query.trim().lowercase()
        q.isEmpty() || holding.company.lowercase().contains(q) || holding.symbol.lowercase().contains(q)
    }
    val invested = state.holdings.sumOf { it.costValue ?: 0.0 }
    val market = state.holdings.sumOf { it.marketValue ?: 0.0 }
    val unrealized = state.holdings.sumOf { it.unrealized ?: 0.0 }
    val realized = state.holdings.sumOf { it.realized ?: 0.0 }

    AppFrame("Portfolio", AppTabs, currentRoute, snack, onTab, onSettings) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::load, modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingPane("Loading portfolio…")
                state.error != null && state.holdings.isEmpty() -> MessagePane("Portfolio unavailable", state.error.orEmpty(), "Try again", vm::load)
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryCard("Invested", money(invested, 0), modifier = Modifier.weight(1f))
                            SummaryCard("Market", money(market, 0), modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryCard("Unrealized", money(unrealized, 0), tone(unrealized), Modifier.weight(1f))
                            SummaryCard("Realized", money(realized, 0), tone(realized), Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true,
                            label = { Text("Company or symbol") },
                        )
                        if (state.error != null) Text(state.error.orEmpty(), color = Down, modifier = Modifier.padding(top = 8.dp))
                    }
                    val open = selected
                    if (open != null) {
                        val trades = state.orders.filter { it.symbol.equals(open, ignoreCase = true) }
                        item {
                            Surface(color = Panel, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row {
                                        Text(open, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        Text("Close", color = Teal, modifier = Modifier.clickable { selected = null })
                                    }
                                    if (trades.isEmpty()) {
                                        Text("No orders for this symbol.", color = Mist, modifier = Modifier.padding(top = 8.dp))
                                    }
                                    trades.forEach { order ->
                                        Text(
                                            "${prettyDate(order.date)}  ${order.action}  ${plain(order.qty, 0)} @ ${money(order.price)}",
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                        Text(
                                            "${money(order.value, 0)} · charges ${money(order.charges)} · ${order.exchange}",
                                            color = Mist,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (shown.isEmpty()) {
                        item { Text(if (state.holdings.isEmpty()) "No holdings." else "No matching holdings.", color = Mist) }
                    }
                    items(shown, key = { it.symbol + it.isin }) { holding ->
                        HoldingRow(holding) { selected = holding.symbol }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingRow(holding: Holding, onClick: () -> Unit) {
    Surface(
        color = PanelRaised,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(holding.company.ifBlank { holding.symbol }, fontWeight = FontWeight.SemiBold)
                    Text("${holding.symbol} · qty ${plain(holding.qty, 2)}", color = Mist, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(money(holding.marketValue, 0), fontWeight = FontWeight.SemiBold)
                    Text(signedPct(holding.dayPct), color = tone(holding.dayPct), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "P/L ${money(holding.unrealized, 0)} · ${signedPct(holding.unrealizedPct)}",
                color = tone(holding.unrealized),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
