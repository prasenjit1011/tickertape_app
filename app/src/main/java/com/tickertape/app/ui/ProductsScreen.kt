package com.tickertape.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tickertape.app.LocalRepository
import com.tickertape.app.data.Product
import com.tickertape.app.data.ProductBody
import com.tickertape.app.data.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val banner: String? = null,
    val products: List<Product> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val total: Long = 0,
)

class ProductsViewModel(private val repo: Repository) : ViewModel() {
    private val _state = MutableStateFlow(ProductsState())
    val state: StateFlow<ProductsState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repo.url.collect { load(reset = true) } }
    }

    fun load(reset: Boolean) {
        viewModelScope.launch {
            val page = if (reset) 0 else _state.value.page
            val hasRows = _state.value.products.isNotEmpty()
            _state.update { it.copy(loading = !hasRows && reset, refreshing = hasRows && reset, error = null) }
            try {
                val result = repo.products(page)
                _state.update {
                    val merged = if (reset || page == 0) result.content else it.products + result.content
                    it.copy(
                        loading = false,
                        refreshing = false,
                        products = merged,
                        page = result.number,
                        totalPages = result.totalPages,
                        total = result.totalElements,
                    )
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(loading = false, refreshing = false, error = error.message) }
            }
        }
    }

    fun loadMore() {
        val state = _state.value
        if (state.page + 1 >= state.totalPages) return
        viewModelScope.launch {
            try {
                val result = repo.products(state.page + 1)
                _state.update {
                    it.copy(
                        products = it.products + result.content,
                        page = result.number,
                        totalPages = result.totalPages,
                        total = result.totalElements,
                    )
                }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun lookup(sku: String, onFound: (Product) -> Unit) {
        val clean = sku.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            try {
                onFound(repo.productBySku(clean))
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun save(existing: Product?, body: ProductBody) {
        viewModelScope.launch {
            try {
                if (existing == null) repo.createProduct(body) else repo.updateProduct(existing.id, body)
                _state.update { it.copy(banner = if (existing == null) "Product created" else "Product updated") }
                load(reset = true)
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun delete(product: Product) {
        viewModelScope.launch {
            try {
                repo.deleteProduct(product.id)
                _state.update { it.copy(banner = "Deleted ${product.sku}", products = it.products.filterNot { row -> row.id == product.id }) }
            } catch (error: Exception) {
                if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun consumeBanner() = _state.update { it.copy(banner = null) }

    companion object {
        fun factory(repo: Repository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProductsViewModel(repo) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(currentRoute: String?, onTab: (String) -> Unit, onSettings: () -> Unit) {
    val repo = LocalRepository.current
    val vm: ProductsViewModel = viewModel(factory = ProductsViewModel.factory(repo))
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(state.banner) {
        val text = state.banner ?: return@LaunchedEffect
        snack.showSnackbar(text)
        vm.consumeBanner()
    }

    val shown = state.products.filter { product ->
        val q = query.trim().lowercase()
        q.isEmpty() || product.name.lowercase().contains(q) || product.sku.lowercase().contains(q)
    }

    AppFrame(
        title = "Products",
        tabs = AppTabs,
        currentRoute = currentRoute,
        snackbarHostState = snack,
        onTab = onTab,
        onSettings = onSettings,
        floatingActionButton = { AddButton { creating = true } },
    ) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { vm.load(true) }, modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingPane("Loading products…")
                state.error != null && state.products.isEmpty() -> MessagePane("Products unavailable", state.error.orEmpty(), "Try again") { vm.load(true) }
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Name or SKU") },
                        )
                        Row {
                            Text("${state.total} products", color = Mist, modifier = Modifier.weight(1f).padding(top = 8.dp))
                            TextButton(onClick = { vm.lookup(query) { editor = it } }, enabled = query.isNotBlank()) {
                                Text("Find SKU")
                            }
                        }
                        if (state.error != null) Text(state.error.orEmpty(), color = Down)
                    }
                    if (shown.isEmpty()) item { Text("No products.", color = Mist) }
                    items(shown, key = { it.id.ifBlank { it.sku } }) { product ->
                        ProductRow(product, onEdit = { editor = product }, onDelete = { pendingDelete = product })
                    }
                    if (state.page + 1 < state.totalPages) {
                        item {
                            TextButton(onClick = vm::loadMore, modifier = Modifier.fillMaxWidth()) { Text("Load more") }
                        }
                    }
                }
            }
        }
    }

    if (creating || editor != null) {
        ProductDialog(
            product = if (creating) null else editor,
            onDismiss = {
                creating = false
                editor = null
            },
            onSave = { body ->
                vm.save(if (creating) null else editor, body)
                creating = false
                editor = null
            },
        )
    }
    pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${product.sku}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(product)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProductRow(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = PanelRaised, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name.ifBlank { product.sku }, fontWeight = FontWeight.SemiBold)
                    Text(product.sku, color = Mist, style = MaterialTheme.typography.bodySmall)
                }
                Text(money(product.price), fontWeight = FontWeight.SemiBold)
            }
            if (product.description.isNotBlank()) {
                Text(product.description, color = Mist, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
            }
            Row {
                Text("Stock ${product.stock}", color = Mist, modifier = Modifier.weight(1f).padding(top = 8.dp))
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = Down) }
            }
        }
    }
}

@Composable
private fun ProductDialog(product: Product?, onDismiss: () -> Unit, onSave: (ProductBody) -> Unit) {
    var sku by remember(product) { mutableStateOf(product?.sku.orEmpty()) }
    var name by remember(product) { mutableStateOf(product?.name.orEmpty()) }
    var description by remember(product) { mutableStateOf(product?.description.orEmpty()) }
    var price by remember(product) { mutableStateOf(if (product == null) "" else product.price.toString()) }
    var stock by remember(product) { mutableStateOf(product?.stock?.toString().orEmpty()) }
    var problem by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "New product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                OutlinedTextField(
                    price,
                    { price = it },
                    label = { Text("Price") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    stock,
                    { stock = it.filter(Char::isDigit) },
                    label = { Text("Stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (problem != null) Text(problem.orEmpty(), color = Down)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = price.toDoubleOrNull()
                val units = stock.toIntOrNull()
                problem = when {
                    sku.isBlank() -> "SKU is required"
                    name.isBlank() -> "Name is required"
                    name.length > 200 -> "Name must be at most 200 characters"
                    description.length > 2000 -> "Description must be at most 2000 characters"
                    amount == null || amount <= 0.0 -> "Price must be greater than 0"
                    units == null || units < 0 -> "Stock must be 0 or greater"
                    else -> null
                }
                if (problem == null && amount != null && units != null) {
                    onSave(ProductBody(sku.trim(), name.trim(), description.trim(), amount, units))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
