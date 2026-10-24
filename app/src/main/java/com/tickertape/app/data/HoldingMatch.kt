package com.tickertape.app.data

private val HOLDING_STOP = setOf(
    "LTD", "LIMITED", "INDIA", "THE", "AND", "OF", "CO", "COMPANY",
    "CORP", "CORPORATION", "PLC", "PVT", "PRIVATE", "INC", "IND",
)

data class Position(
    val qty: Double?,
    val avgCost: Double?,
    val invested: Double?,
)

fun matchPositions(holdings: List<Holding>, stocks: List<Stock>): Map<String, Position> {
    val byTicker = stocks.mapNotNull { stock ->
        val ticker = stock.ticker.trim().uppercase()
        if (ticker.isEmpty() || stock.sid.isBlank()) null else ticker to stock
    }.toMap()
    val matched = linkedMapOf<String, Position>()
    holdings.forEach { row ->
        val symbol = row.symbol.trim().uppercase()
        val stock = byTicker[symbol] ?: matchHolding(row.company, stocks)
        val sid = stock?.sid?.trim()?.uppercase().orEmpty()
        if (sid.isEmpty()) return@forEach
        matched[sid] = Position(row.qty, row.avgCost, row.costValue)
    }
    return matched
}

fun livePnl(stock: Stock, position: Position?): Double? {
    val price = stock.quote ?: return null
    val qty = position?.qty ?: return null
    val invested = position.invested ?: return null
    return price * qty - invested
}

fun livePnlPct(pnl: Double?, invested: Double?): Double? {
    if (pnl == null || invested == null || invested == 0.0) return null
    return pnl / invested * 100.0
}

private fun matchHolding(company: String, stocks: List<Stock>): Stock? {
    val name = tokens(company)
    var best: Stock? = null
    var bestScore = 0.0
    var second = 0.0
    for (stock in stocks) {
        val score = score(name, tokens(stock.name))
        if (score > bestScore) {
            second = bestScore
            bestScore = score
            best = stock
        } else if (score > second) {
            second = score
        }
    }
    val winner = best ?: return null
    val first = name.firstOrNull()
    val stockTokens = tokens(winner.name)
    if (first != null && first.length >= 4 && stockTokens.none { softEqual(first, it) }) return null
    if (bestScore >= 0.75 && bestScore - second >= 0.08) return winner
    if (bestScore >= 0.67 && bestScore - second >= 0.15) return winner
    return null
}

private fun tokens(name: String): List<String> = name.uppercase()
    .replace("&", " AND ")
    .replace(Regex("[^A-Z0-9 ]"), " ")
    .split(Regex("\\s+"))
    .filter { it.length > 1 && it !in HOLDING_STOP }

private fun softEqual(left: String, right: String): Boolean {
    if (left == right) return true
    if (left.length < 4 || right.length < 4) return false
    return left.startsWith(right) || right.startsWith(left)
}

private fun overlap(left: List<String>, right: List<String>): Int {
    val used = mutableSetOf<Int>()
    var hit = 0
    for (token in left) {
        val at = right.indices.firstOrNull { position ->
            position !in used && softEqual(token, right[position])
        }
        if (at != null) {
            used += at
            hit += 1
        }
    }
    return hit
}

private fun score(left: List<String>, right: List<String>): Double {
    if (left.isEmpty() || right.isEmpty()) return 0.0
    val hit = overlap(left, right)
    val dice = (2.0 * hit) / (left.size + right.size)
    val leading = dice >= 0.62 &&
        left[0].length >= 5 &&
        softEqual(left[0], right[0]) &&
        left.size > 1 &&
        right.size > 1 &&
        softEqual(left[1], right[1])
    return if (leading) maxOf(dice, 0.86) else dice
}
