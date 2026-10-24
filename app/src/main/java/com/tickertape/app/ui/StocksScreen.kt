package com.tickertape.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickertape.app.LocalRepository
import com.tickertape.app.data.BandRun
import com.tickertape.app.data.Position
import com.tickertape.app.data.Repository
import com.tickertape.app.data.Stock
import com.tickertape.app.data.matchPositions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StocksState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val banner: String? = null,
    val busy: String? = null,
    val stocks: List<Stock> = emptyList(),
    val positions: Map<String, Position> = emptyMap(),
    val bandRuns: List<BandRun> = emptyList(),
    val query: String = "",
    val sid: String = "HYU",
    val hideSmall: Boolean = false,
    val hideMid: Boolean = false,
    val heldOnly: Boolean = false,
    val sortKey: String = "mrktCapRank",
    val sortDir: Int = 1,
)

class StocksViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(StocksState())
    val state: StateFlow<StocksState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            repo.url.collect { load() }
        }
    }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            val hasRows = _state.value.stocks.isNotEmpty()
            _state.update { it.copy(loading = !hasRows, refreshing = hasRows, error = null) }
            try {
                val stocks = repo.stocks()
                repo.cacheStocks(stocks)
                val holdings = runCatching { repo.holdings() }.getOrDefault(emptyList())
                val runs = runCatching { repo.bandRuns() }.getOrDefault(emptyList())
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        stocks = stocks,
                        positions = matchPositions(holdings, stocks),
                        bandRuns = runs,
                        error = null,
                    )
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(loading = false, refreshing = false, error = error.message) }
            }
        }
    }

    fun query(value: String) = _state.update { it.copy(query = value) }
    fun sid(value: String) = _state.update { it.copy(sid = value.filter { ch -> ch.isLetterOrDigit() }.take(12)) }
    fun toggleSmall() = _state.update { it.copy(hideSmall = !it.hideSmall) }
    fun toggleMid() = _state.update { it.copy(hideMid = !it.hideMid) }
    fun toggleHeld() = _state.update { it.copy(heldOnly = !it.heldOnly) }
    fun sortBy(key: String, numeric: Boolean) = _state.update {
        if (it.sortKey == key) it.copy(sortDir = -it.sortDir)
        else it.copy(sortKey = key, sortDir = if (numeric) -1 else 1)
    }
    fun consumeBanner() = _state.update { it.copy(banner = null) }

    fun refreshOne(sid: String) {
        val clean = sid.trim().uppercase()
        if (clean.isEmpty()) return
        act("Syncing $clean") {
            repo.refresh(clean)
            "Updated $clean"
        }
    }

    fun syncSid() {
        val sid = _state.value.sid.trim().uppercase()
        if (sid.isEmpty()) return
        act("Syncing $sid") {
            repo.sync(sid)
            "Synced $sid"
        }
    }

    fun syncBand(band: String) = act("Syncing $band") {
        val result = repo.syncAll(band)
        result.message.ifBlank { "Synced ${result.updated} stocks" }
    }

    fun range(band: String) = act("Updating $band") {
        val result = repo.rangeUpdate(band)
        result.message.ifBlank { "Updated ${result.updated} stocks" }
    }

    fun quick() = act("Quick update") {
        val result = repo.syncDue()
        result.message.ifBlank { if (result.started) "Quick update started" else "Nothing due" }
    }

    private fun act(label: String, block: suspend () -> String) {
        if (_state.value.busy != null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = label, banner = null) }
            try {
                val message = block()
                val stocks = repo.stocks()
                repo.cacheStocks(stocks)
                val holdings = runCatching { repo.holdings() }.getOrDefault(emptyList())
                val runs = runCatching { repo.bandRuns() }.getOrDefault(_state.value.bandRuns)
                _state.update {
                    it.copy(
                        busy = null,
                        stocks = stocks,
                        positions = matchPositions(holdings, stocks),
                        bandRuns = runs,
                        banner = message,
                    )
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(busy = null, error = error.message) }
            }
        }
    }

    companion object {
        fun factory(repo: Repository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StocksViewModel(repo) as T
        }
    }
}

@Composable
fun StocksScreen(
    currentRoute: String?,
    onOpen: (String) -> Unit,
    onTab: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val repo = LocalRepository.current
    val vm: StocksViewModel = viewModel(factory = StocksViewModel.factory(repo))
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state.banner) {
        val text = state.banner ?: return@LaunchedEffect
        snack.showSnackbar(text)
        vm.consumeBanner()
    }

    val shown = remember(state.stocks, state.positions, state.query, state.hideSmall, state.hideMid, state.heldOnly, state.sortKey, state.sortDir) {
        visibleStocks(state)
    }

    AppFrame(
        title = "Stock Data",
        tabs = AppTabs,
        currentRoute = currentRoute,
        snackbarHostState = snack,
        onTab = onTab,
        onSettings = onSettings,
        showTopBar = false,
        containerColor = Forest,
        navigationColor = Sheet,
    ) { padding ->
        StockHome(padding, state, shown, vm, onOpen, onTab, onSettings)
    }
}
