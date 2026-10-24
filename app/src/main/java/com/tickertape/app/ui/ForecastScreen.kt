package com.tickertape.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.tickertape.app.data.Repository
import com.tickertape.app.data.SidQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForecastState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val quotes: List<SidQuote> = emptyList(),
)

class ForecastViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repo.url.collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            val hasRows = _state.value.quotes.isNotEmpty()
            _state.update { it.copy(loading = !hasRows, refreshing = hasRows, error = null) }
            try {
                val quotes = repo.sidQuotes()
                _state.update { it.copy(loading = false, refreshing = false, quotes = quotes) }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(loading = false, refreshing = false, error = error.message) }
            }
        }
    }

    companion object {
        fun factory(repo: Repository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ForecastViewModel(repo) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(currentRoute: String?, onTab: (String) -> Unit, onSettings: () -> Unit) {
    val repo = LocalRepository.current
    val vm: ForecastViewModel = viewModel(factory = ForecastViewModel.factory(repo))
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val shown = state.quotes.filter { quote ->
        val q = query.trim().lowercase()
        q.isEmpty() || quote.name.lowercase().contains(q) || quote.sid.lowercase().contains(q)
    }
    val points = shown.sumOf { it.quotes.size }

    AppFrame("Forecast", AppTabs, currentRoute, snack, onTab, onSettings) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::load, modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingPane("Loading forecast…")
                state.error != null && state.quotes.isEmpty() -> MessagePane("Forecast unavailable", state.error.orEmpty(), "Try again", vm::load)
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Name or SID") },
                        )
                        Text(
                            if (state.quotes.isEmpty()) "No snapshot yet" else "${shown.size} stocks · $points days",
                            color = Mist,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (shown.isEmpty()) {
                        item {
                            Text(
                                if (state.quotes.isEmpty()) "Quotes are stored at 9:15am and 4:00pm on trading days." else "No matching stocks.",
                                color = Mist,
                            )
                        }
                    }
                    items(shown, key = { it.sid }) { quote ->
                        ForecastRow(quote, open == quote.sid) {
                            open = if (open == quote.sid) null else quote.sid
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastRow(quote: SidQuote, expanded: Boolean, onClick: () -> Unit) {
    val latest = quote.quotes.firstOrNull()
    Surface(
        color = PanelRaised,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(quote.name.ifBlank { quote.sid }, fontWeight = FontWeight.SemiBold)
            Text(quote.sid, color = Mist, style = MaterialTheme.typography.bodySmall)
            if (latest == null) {
                Text("No quotes yet", color = Mist, modifier = Modifier.padding(top = 6.dp))
            } else {
                KeyValueRow("Forecast", plain(latest.breco))
                KeyValueRow("Price", money(latest.price))
                KeyValueRow("Date", prettyDate(latest.date))
            }
            if (expanded && quote.quotes.size > 1) {
                quote.quotes.drop(1).forEach { point ->
                    Text(
                        "${prettyDate(point.date)}   ${plain(point.breco)}   ${money(point.price)}",
                        color = Mist,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
