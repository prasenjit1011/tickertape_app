package com.tickertape.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tickertape.app.data.MonthlyHigh
import com.tickertape.app.data.Position
import com.tickertape.app.data.Repository
import com.tickertape.app.data.Stock
import com.tickertape.app.data.YearlyRange
import com.tickertape.app.data.livePnl
import com.tickertape.app.data.livePnlPct
import com.tickertape.app.data.matchPositions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val banner: String? = null,
    val busy: String? = null,
    val stock: Stock? = null,
    val position: Position? = null,
    val yearly: List<YearlyRange> = emptyList(),
    val monthly: List<MonthlyHigh> = emptyList(),
)

class StockDetailViewModel(private val repo: Repository, private val sid: String) : ViewModel() {
    private val _state = MutableStateFlow(DetailState(stock = repo.cachedStock(sid)))
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.stock == null, error = null) }
            try {
                val loaded = coroutineScope {
                    val stock = async {
                        repo.cachedStock(sid) ?: repo.stocks().also(repo::cacheStocks)
                            .firstOrNull { it.sid.equals(sid, ignoreCase = true) }
                    }
                    val yearly = async { runCatching { repo.yearly(sid) }.getOrDefault(emptyList()) }
                    val monthly = async { runCatching { repo.monthly(sid) }.getOrDefault(emptyList()) }
                    val holdings = async { runCatching { repo.holdings() }.getOrDefault(emptyList()) }
                    Loaded(stock.await(), yearly.await(), monthly.await(), holdings.await())
                }
                val stock = loaded.stock ?: throw IllegalStateException("No stock stored for $sid")
                val positions = matchPositions(loaded.holdings, listOf(stock))
                _state.update {
                    it.copy(
                        loading = false,
                        stock = stock,
                        position = positions[stock.sid.uppercase()],
                        yearly = loaded.yearly,
                        monthly = loaded.monthly,
                    )
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(loading = false, error = error.message) }
            }
        }
    }

    fun refresh() = act("Refreshing") {
        val stock = repo.refresh(sid)
        repo.cacheStocks(listOf(stock))
        "Refreshed ${stock.ticker.ifBlank { sid }}"
    }

    fun sync() = act("Syncing") {
        repo.sync(sid)
        "Synced $sid"
    }

    private fun act(label: String, block: suspend () -> String) {
        if (_state.value.busy != null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = label) }
            try {
                val message = block()
                _state.update { it.copy(busy = null, banner = message) }
                load()
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(busy = null, error = error.message) }
            }
        }
    }

    fun consumeBanner() = _state.update { it.copy(banner = null) }

    private data class Loaded(
        val stock: Stock?,
        val yearly: List<YearlyRange>,
        val monthly: List<MonthlyHigh>,
        val holdings: List<com.tickertape.app.data.Holding>,
    )

    companion object {
        fun factory(repo: Repository, sid: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StockDetailViewModel(repo, sid) as T
        }
    }
}

@Composable
fun StockDetailScreen(sid: String, onBack: () -> Unit, onSettings: () -> Unit) {
    val repo = LocalRepository.current
    val vm: StockDetailViewModel = viewModel(
        key = sid,
        factory = StockDetailViewModel.factory(repo, sid),
    )
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.banner) {
        val text = state.banner ?: return@LaunchedEffect
        snack.showSnackbar(text)
        vm.consumeBanner()
    }
    AppFrame(
        title = state.stock?.ticker?.ifBlank { sid } ?: sid,
        tabs = AppTabs,
        currentRoute = null,
        snackbarHostState = snack,
        onTab = {},
        onSettings = onSettings,
        onBack = onBack,
    ) { padding ->
        when {
            state.loading && state.stock == null -> LoadingPane("Loading $sid…")
            state.stock == null -> MessagePane(
                title = "Stock unavailable",
                detail = state.error ?: "No stock stored for $sid",
                actionLabel = "Try again",
                onAction = vm::load,
            )
            else -> DetailBody(state, padding, vm)
        }
    }
}

@Composable
private fun DetailBody(state: DetailState, padding: PaddingValues, vm: StockDetailViewModel) {
    val stock = state.stock ?: return
    val pnl = livePnl(stock, state.position)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            if (state.busy != null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            Text(stock.name.ifBlank { stock.sid }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                listOf(stock.sector, stock.exchange, stock.marketCapLabel).filter { it.isNotBlank() }.joinToString(" · "),
                color = Mist,
            )
            Text(money(stock.quote), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
            Text(signedPct(stock.dyChange), color = tone(stock.dyChange))
            if (state.error != null) Text(state.error.orEmpty(), color = Down, modifier = Modifier.padding(top = 8.dp))
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                Button(onClick = vm::refresh, enabled = state.busy == null) { Text("Refresh") }
                OutlinedButton(onClick = vm::sync, enabled = state.busy == null) { Text("Sync") }
            }
            SectionTitle("Quote")
            KeyValueRow("Change", signedNumber(stock.change), tone(stock.change))
            KeyValueRow("Day", signedPct(stock.dyChange), tone(stock.dyChange))
            KeyValueRow("Week", signedPct(stock.wkChange), tone(stock.wkChange))
            KeyValueRow("Month", signedPct(stock.mnChange), tone(stock.mnChange))
            KeyValueRow("Forecast", plain(stock.breco))
            KeyValueRow("Risk", plain(stock.risk))
            SectionTitle("Valuation")
            KeyValueRow("PE", plain(stock.pe))
            KeyValueRow("TTM PE", plain(stock.ttmPe))
            KeyValueRow("PB", plain(stock.pb))
            KeyValueRow("ROE", plain(stock.roe))
            KeyValueRow("EPS", plain(stock.eps))
            KeyValueRow("Div yield", plain(stock.divYield))
            SectionTitle("Range")
            KeyValueRow("52w high", money(stock.week52High))
            KeyValueRow("52w low", money(stock.week52Low))
            KeyValueRow("From high", signedPct(stock.away52wH), tone(stock.away52wH))
            KeyValueRow("From low", signedPct(stock.away52wL), tone(stock.away52wL))
            KeyValueRow("4w", signedPct(stock.week4Pct), tone(stock.week4Pct))
            KeyValueRow("52w", signedPct(stock.week52Pct), tone(stock.week52Pct))
            SectionTitle("Size")
            KeyValueRow("Rank", rankLabel(stock.mrktCapRank))
            KeyValueRow("Market cap", plain(stock.marketCap, 0))
            KeyValueRow("Updated", prettyInstant(stock.updatedAt))
            if (state.position != null || pnl != null) {
                SectionTitle("Holding")
                KeyValueRow("Qty", plain(state.position?.qty, 0))
                KeyValueRow("Invested", money(state.position?.invested, 0))
                KeyValueRow("Avg buy", money(state.position?.avgCost, 0))
                KeyValueRow("Live P/L", money(pnl, 0), tone(pnl))
                KeyValueRow("Live P/L %", signedPct(livePnlPct(pnl, state.position?.invested)), tone(pnl))
            }
            SectionTitle("2020 – 2021")
            KeyValueRow("High 2020", money(stock.hPrice2020))
            KeyValueRow("Low 2020", money(stock.lPrice2020))
            KeyValueRow("High 2021", money(stock.hPrice2021))
            KeyValueRow("Low 2021", money(stock.lPrice2021))
            SectionTitle("Yearly")
        }
        if (state.yearly.isEmpty()) {
            item { Text("No yearly ranges.", color = Mist) }
        }
        items(state.yearly) { range ->
            Surface(color = PanelRaised, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(range.year.toString(), fontWeight = FontWeight.SemiBold)
                    KeyValueRow("High", "${money(range.high)} · ${range.highWeek.ifBlank { "—" }}")
                    KeyValueRow("Low", "${money(range.low)} · ${range.lowWeek.ifBlank { "—" }}")
                }
            }
        }
        item { SectionTitle("Monthly") }
        if (state.monthly.isEmpty()) {
            item { Text("No monthly highs.", color = Mist) }
        }
        items(state.monthly) { month ->
            Surface(color = PanelRaised, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(month.month.ifBlank { "Month" }, fontWeight = FontWeight.SemiBold)
                    KeyValueRow("High", "${money(month.high)} · ${prettyDate(month.highDate)}")
                    KeyValueRow("Low", "${money(month.low)} · ${prettyDate(month.lowDate)}")
                }
            }
        }
    }
}
