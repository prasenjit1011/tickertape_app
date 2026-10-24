package com.tickertape.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickertape.app.data.BandRun
import com.tickertape.app.data.Position
import com.tickertape.app.data.Stock
import com.tickertape.app.data.livePnl
import com.tickertape.app.data.livePnlPct

internal val Forest = Color(0xFF0C241C)
internal val Sheet = Color(0xFF12382D)
private val SheetLine = Color(0xFF2D6A56)
private val Leaf = Color(0xFF0F6E56)
private val HeaderGreen = Color(0xFF1A4D3E)
private val Stripe = Color(0xFF164536)
private val Moss = Color(0xFFB7D4C6)
private val Sage = Color(0xFF8FB5A4)
private val Foam = Color(0xFFE7F6EE)
private val PnlUp = Color(0xFF8FD4B4)
private val PnlDown = Color(0xFFFFB4B4)
private val FieldLine = Color(0xFF3D7A66)
private val NameWidth = 168.dp
private val RowHeight = 72.dp

private data class StockColumn(
    val key: String,
    val label: String,
    val numeric: Boolean,
    val width: Dp,
    val text: (Stock, Position?) -> String,
    val signed: Boolean = false,
    val money: Boolean = false,
)

private val stockColumns = listOf(
    StockColumn("ticker", "Ticker", false, 84.dp, { stock, _ -> stock.ticker.ifBlank { "—" } }),
    StockColumn("sid", "SID", false, 72.dp, { stock, _ -> stock.sid.ifBlank { "—" } }),
    StockColumn("sector", "Sector", false, 120.dp, { stock, _ -> stock.sector.ifBlank { "—" } }),
    StockColumn("marketCapLabel", "Cap label", false, 88.dp, { stock, _ -> stock.marketCapLabel.ifBlank { "—" } }),
    StockColumn("mrktCapRank", "Rank", true, 64.dp, { stock, _ -> plain(stock.mrktCapRank, 0) }),
    StockColumn("marketCap", "MarketCap", true, 96.dp, { stock, _ -> plain(stock.marketCap, 0) }),
    StockColumn("breco", "Forecast", true, 80.dp, { stock, _ -> plain(stock.breco) }),
    StockColumn("risk", "Risk", true, 68.dp, { stock, _ -> plain(stock.risk) }),
    StockColumn("pe", "PE", true, 64.dp, { stock, _ -> plain(stock.pe) }),
    StockColumn("lastPrice", "LTP", true, 80.dp, { stock, _ -> plain(stock.quote) }),
    StockColumn("change", "Change", true, 80.dp, { stock, _ -> signedNumber(stock.change) }, signed = true),
    StockColumn("dyChange", "Day %", true, 72.dp, { stock, _ -> signedPct(stock.dyChange) }, signed = true),
    StockColumn("4wpct", "4w %", true, 72.dp, { stock, _ -> signedPct(stock.week4Pct) }, signed = true),
    StockColumn("52wpct", "52w %", true, 72.dp, { stock, _ -> signedPct(stock.week52Pct) }, signed = true),
    StockColumn("away52wH", "H-52W %", true, 80.dp, { stock, _ -> signedPct(stock.away52wH) }, signed = true),
    StockColumn("away52wL", "L-52W %", true, 80.dp, { stock, _ -> signedPct(stock.away52wL) }, signed = true),
    StockColumn("52wHigh", "52w high", true, 84.dp, { stock, _ -> plain(stock.week52High) }),
    StockColumn("52wLow", "52w low", true, 84.dp, { stock, _ -> plain(stock.week52Low) }),
    StockColumn("holdQty", "Qty", true, 64.dp, { _, position -> plain(position?.qty, 0) }),
    StockColumn("holdInvested", "Invested", true, 96.dp, { _, position -> money(position?.invested, 0) }, money = true),
    StockColumn("holdLivePnl", "Live P/L", true, 96.dp, { stock, position -> money(livePnl(stock, position), 0) }, signed = true),
    StockColumn("holdPct", "Live P/L %", true, 88.dp, { stock, position ->
        signedPct(livePnlPct(livePnl(stock, position), position?.invested))
    }, signed = true),
    StockColumn("holdAvgCost", "Avg buy", true, 84.dp, { _, position -> money(position?.avgCost, 0) }, money = true),
    StockColumn("wkChange", "Week %", true, 76.dp, { stock, _ -> signedPct(stock.wkChange) }, signed = true),
    StockColumn("mnChange", "Month %", true, 80.dp, { stock, _ -> signedPct(stock.mnChange) }, signed = true),
    StockColumn("ttmPe", "TTM PE", true, 76.dp, { stock, _ -> plain(stock.ttmPe) }),
    StockColumn("pb", "PB", true, 64.dp, { stock, _ -> plain(stock.pb) }),
    StockColumn("eps", "EPS", true, 68.dp, { stock, _ -> plain(stock.eps) }),
    StockColumn("roe", "ROE", true, 68.dp, { stock, _ -> plain(stock.roe) }),
    StockColumn("divYield", "Div yield", true, 80.dp, { stock, _ -> plain(stock.divYield) }),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StockHome(
    padding: PaddingValues,
    state: StocksState,
    shown: List<Stock>,
    vm: StocksViewModel,
    onOpen: (String) -> Unit,
    onTab: (String) -> Unit,
    onSettings: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = vm::load,
        modifier = Modifier.fillMaxSize().padding(padding).padding(6.dp),
    ) {
        when {
            state.loading -> LoadingPane("Loading stocks…")
            state.error != null && state.stocks.isEmpty() -> MessagePane(
                title = "Stocks are unavailable",
                detail = state.error.orEmpty(),
                actionLabel = "Try again",
                onAction = vm::load,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Sheet)
                    .border(1.dp, SheetLine, RoundedCornerShape(13.dp)),
            ) {
                StockToolbar(state, shown.size, vm, onTab, onSettings)
                if (state.busy != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PnlUp, trackColor = SheetLine)
                }
                if (state.error != null) {
                    Text(
                        state.error.orEmpty(),
                        color = PnlDown,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                StockTable(shown, state, vm, onOpen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StockToolbar(
    state: StocksState,
    shown: Int,
    vm: StocksViewModel,
    onTab: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var searchOpen by remember { mutableStateOf(state.query.isNotBlank()) }
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Leaf),
                contentAlignment = Alignment.Center,
            ) {
                Text("SD", color = Color(0xFFF4FFF9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (!searchOpen) {
                Text(
                    "Stock Data",
                    color = Foam,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            if (searchOpen) {
                SearchField(
                    value = state.query,
                    onValueChange = vm::query,
                    onClear = {
                        vm.query("")
                        searchOpen = false
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = { searchOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = Moss, modifier = Modifier.size(16.dp))
                }
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = state.busy == null, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Foam, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    RunItem("Sync 1–600", state.bandRuns, "sync:1-600") { menu = false; vm.syncBand("1-600") }
                    RunItem("Sync 601–All", state.bandRuns, "sync:601-all") { menu = false; vm.syncBand("601-all") }
                    RunItem("Rank 1–600", state.bandRuns, "rank:1-600") { menu = false; vm.range("1-600") }
                    RunItem("Rank 601–All", state.bandRuns, "rank:601-all") { menu = false; vm.range("601-all") }
                    DropdownMenuItem(text = { Text("Quick update") }, onClick = { menu = false; vm.quick() })
                    DropdownMenuItem(text = { Text("Days Profit History") }, onClick = { menu = false; onTab(ROUTE_PROFIT) })
                    DropdownMenuItem(text = { Text("Daily forecast") }, onClick = { menu = false; onTab(ROUTE_FORECAST) })
                    DropdownMenuItem(text = { Text("Portfolio") }, onClick = { menu = false; onTab(ROUTE_PORTFOLIO) })
                    DropdownMenuItem(text = { Text("Products") }, onClick = { menu = false; onTab(ROUTE_PRODUCTS) })
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { menu = false; onSettings() })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusBoard(shown, state.stocks.size, bookLive(state))
            BasicTextField(
                value = state.sid,
                onValueChange = vm::sid,
                textStyle = TextStyle(color = Foam, fontSize = 14.sp),
                singleLine = true,
                cursorBrush = SolidColor(PnlUp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { vm.syncSid() }),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Forest)
                    .border(1.dp, FieldLine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
            GhostButton("Sync", enabled = state.busy == null && state.sid.isNotBlank(), onClick = vm::syncSid)
            CapToggle(state.hideSmall, state.hideMid, vm::toggleSmall, vm::toggleMid)
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Forest)
            .border(1.dp, FieldLine, RoundedCornerShape(999.dp))
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Foam, fontSize = 14.sp),
            singleLine = true,
            cursorBrush = SolidColor(PnlUp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Filter name, ticker, sector", color = Sage, fontSize = 13.sp)
                inner()
            },
        )
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Moss, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun StatusBoard(shown: Int, stored: Int, live: Double?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Forest)
            .border(1.dp, SheetLine, RoundedCornerShape(8.dp)),
    ) {
        StatusStat(shown.toString(), "shown")
        if (live != null) {
            StatusStat(money(live, 0), "live", tone = if (live > 0) PnlUp else if (live < 0) PnlDown else Foam)
        } else if (stored == 0) {
            StatusStat("0", "stored")
        }
    }
}

@Composable
private fun StatusStat(value: String, label: String, tone: Color = Foam) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(value, color = tone, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label.uppercase(), color = Sage, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun CapToggle(hideSmall: Boolean, hideMid: Boolean, onSmall: () -> Unit, onMid: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Forest)
            .border(1.dp, FieldLine, RoundedCornerShape(999.dp))
            .padding(2.dp),
    ) {
        CapButton("Small", hideSmall, onSmall)
        CapButton("Mid", hideMid, onMid)
    }
}

@Composable
private fun CapButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color(0xFFF4FFF9) else Foam,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Leaf else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun GhostButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (enabled) Foam else Sage,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, FieldLine, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun RunItem(label: String, runs: List<BandRun>, id: String, onClick: () -> Unit) {
    val whenRun = runs.firstOrNull { it.id == id }?.updatedAt
    DropdownMenuItem(
        text = {
            Column {
                Text(label)
                Text(prettyInstant(whenRun), color = Sage, fontSize = 11.sp)
            }
        },
        onClick = onClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StockTable(
    shown: List<Stock>,
    state: StocksState,
    vm: StocksViewModel,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val list = rememberLazyListState()
    LazyColumn(state = list, modifier = modifier.fillMaxWidth()) {
        stickyHeader {
            Row(modifier = Modifier.fillMaxWidth().height(36.dp).background(HeaderGreen)) {
                HeaderCell("Name", NameWidth, sortMark(state, "name")) { vm.sortBy("name", false) }
                Row(modifier = Modifier.horizontalScroll(scroll)) {
                    stockColumns.forEach { column ->
                        HeaderCell(column.label, column.width, sortMark(state, column.key)) {
                            vm.sortBy(column.key, column.numeric)
                        }
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    if (state.stocks.isEmpty()) "No stocks stored yet. Sync a symbol." else "No matching stocks.",
                    color = Moss,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        itemsIndexed(shown, key = { _, stock -> "${stock.sid}|${stock.id}|${stock.ticker}" }) { index, stock ->
            val position = state.positions[stock.sid.uppercase()]
            val background = if (index % 2 == 1) Stripe else Sheet
            Row(modifier = Modifier.fillMaxWidth().height(RowHeight).background(background)) {
                NameCell(stock, onOpen, vm)
                Row(modifier = Modifier.horizontalScroll(scroll).height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
                    stockColumns.forEach { column ->
                        val value = column.text(stock, position)
                        val color = when {
                            column.key == "sector" && stock.sector.isNotBlank() -> PnlUp
                            column.signed || column.money && column.key == "holdLivePnl" -> signedColor(value)
                            else -> Foam
                        }
                        Text(
                            value,
                            color = color,
                            fontSize = 13.sp,
                            textAlign = if (column.numeric) TextAlign.End else TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(column.width)
                                .clickable(enabled = column.key == "sector" && stock.sector.isNotBlank()) {
                                    vm.query(stock.sector)
                                }
                                .padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(label: String, width: Dp, mark: String, onClick: () -> Unit) {
    Text(
        if (mark.isEmpty()) label else "$label $mark",
        color = Moss,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
    )
}

@Composable
private fun NameCell(stock: Stock, onOpen: (String) -> Unit, vm: StocksViewModel) {
    val context = LocalContext.current
    val title = clipName(stock.name.ifBlank { stock.sid })
    Column(
        modifier = Modifier
            .width(NameWidth)
            .height(RowHeight)
            .clickable(enabled = stock.sid.isNotBlank()) { onOpen(stock.sid) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = Foam, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniAction(plain(stock.quote, 0)) { vm.refreshOne(stock.sid) }
            MiniAction("Chart") {
                val url = "https://www.tickertape.in/stocks/${Uri.encode(stock.sid)}?chartScope=1mo"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            MiniAction("Yearly") { if (stock.sid.isNotBlank()) onOpen(stock.sid) }
        }
    }
}

@Composable
private fun MiniAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Foam,
        fontSize = 10.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, FieldLine, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private fun sortMark(state: StocksState, key: String): String {
    if (state.sortKey != key) return ""
    return if (state.sortDir > 0) "▲" else "▼"
}

private fun clipName(value: String): String {
    if (value.length <= 20) return value
    return value.take(19) + "…"
}

private fun bookLive(state: StocksState): Double? {
    var total = 0.0
    var count = 0
    for (stock in state.stocks) {
        val pnl = livePnl(stock, state.positions[stock.sid.uppercase()]) ?: continue
        total += pnl
        count += 1
    }
    return if (count == 0) null else total
}

private fun signedColor(value: String): Color = when {
    value.startsWith("+") || value.startsWith("₹") && !value.contains("-") && value != "₹0" -> PnlUp
    value.startsWith("-") || value.startsWith("-₹") || value.startsWith("₹-") -> PnlDown
    else -> Foam
}

internal fun visibleStocks(state: StocksState): List<Stock> {
    val query = state.query.trim().lowercase()
    return state.stocks.filter { stock ->
        val label = stock.marketCapLabel.lowercase()
        if (state.hideSmall && label == "smallcap") return@filter false
        if (state.hideMid && label == "midcap") return@filter false
        if (state.heldOnly && state.positions[stock.sid.uppercase()] == null) return@filter false
        if (query.isEmpty()) return@filter true
        listOf(stock.name, stock.ticker, stock.sid, stock.sector).any { it.lowercase().contains(query) }
    }.sortedWith(stockOrder(state))
}

private fun stockOrder(state: StocksState): Comparator<Stock> {
    return Comparator { left, right ->
        val a = sortValue(left, state, state.sortKey)
        val b = sortValue(right, state, state.sortKey)
        when {
            a == null && b == null -> 0
            a == null -> 1
            b == null -> -1
            else -> {
                @Suppress("UNCHECKED_CAST")
                val compared = (a as Comparable<Any>).compareTo(b)
                if (state.sortDir > 0) compared else -compared
            }
        }
    }
}

private fun sortValue(stock: Stock, state: StocksState, key: String): Comparable<*>? {
    val position = state.positions[stock.sid.uppercase()]
    val number: Double? = when (key) {
        "mrktCapRank" -> stock.mrktCapRank
        "marketCap" -> stock.marketCap
        "breco" -> stock.breco
        "risk" -> stock.risk
        "pe" -> stock.pe
        "lastPrice" -> stock.quote
        "change" -> stock.change
        "dyChange" -> stock.dyChange
        "4wpct" -> stock.week4Pct
        "52wpct" -> stock.week52Pct
        "away52wH" -> stock.away52wH
        "away52wL" -> stock.away52wL
        "52wHigh" -> stock.week52High
        "52wLow" -> stock.week52Low
        "holdQty" -> position?.qty
        "holdInvested" -> position?.invested
        "holdLivePnl" -> livePnl(stock, position)
        "holdPct" -> livePnlPct(livePnl(stock, position), position?.invested)
        "holdAvgCost" -> position?.avgCost
        "wkChange" -> stock.wkChange
        "mnChange" -> stock.mnChange
        "ttmPe" -> stock.ttmPe
        "pb" -> stock.pb
        "eps" -> stock.eps
        "roe" -> stock.roe
        "divYield" -> stock.divYield
        else -> null
    }
    if (stockColumns.any { it.key == key && it.numeric } || key == "mrktCapRank") return number
    val text = when (key) {
        "name" -> stock.name
        "ticker" -> stock.ticker
        "sid" -> stock.sid
        "sector" -> stock.sector
        "marketCapLabel" -> stock.marketCapLabel
        else -> ""
    }
    return text.lowercase().ifBlank { null }
}
