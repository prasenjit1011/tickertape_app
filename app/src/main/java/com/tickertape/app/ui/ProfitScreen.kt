package com.tickertape.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickertape.app.LocalRepository
import com.tickertape.app.data.DayProfitHistory
import com.tickertape.app.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfitState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val history: DayProfitHistory? = null,
)

class ProfitViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ProfitState())
    val state: StateFlow<ProfitState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repo.url.collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            val hasRows = _state.value.history != null
            _state.update { it.copy(loading = !hasRows, refreshing = hasRows, error = null) }
            try {
                val history = repo.dayProfits()
                _state.update { it.copy(loading = false, refreshing = false, history = history) }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(loading = false, refreshing = false, error = error.message) }
            }
        }
    }

    companion object {
        fun factory(repo: Repository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfitViewModel(repo) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreen(currentRoute: String?, onTab: (String) -> Unit, onSettings: () -> Unit) {
    val repo = LocalRepository.current
    val vm: ProfitViewModel = viewModel(factory = ProfitViewModel.factory(repo))
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    val history = state.history
    val days = history?.days.orEmpty().sortedByDescending { it.date }

    AppFrame("Days profit", AppTabs, currentRoute, snack, onTab, onSettings) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::load, modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingPane("Loading profit history…")
                history == null -> MessagePane("Profit history unavailable", state.error ?: "No response", "Try again", vm::load)
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryCard("Total invested", money(history.totalInvested, 0), modifier = Modifier.weight(1f))
                            SummaryCard("Total profit", money(history.totalProfit, 0), tone(history.totalProfit), Modifier.weight(1f))
                        }
                    }
                    if (days.isEmpty()) {
                        item { Text("No day profits yet.", color = Mist, modifier = Modifier.padding(top = 12.dp)) }
                    }
                    items(days, key = { it.date }) { day ->
                        Surface(color = PanelRaised, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(14.dp)) {
                                Text(prettyDate(day.date), fontWeight = FontWeight.SemiBold)
                                KeyValueRow("Invested", money(day.invested, 0))
                                KeyValueRow("Profit", money(day.profit, 0), tone(day.profit))
                                KeyValueRow("Total invested", money(day.totalInvested, 0))
                                KeyValueRow("Total profit", money(day.totalProfit, 0), tone(day.totalProfit))
                            }
                        }
                    }
                }
            }
        }
    }
}
