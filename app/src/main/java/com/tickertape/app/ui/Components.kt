package com.tickertape.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class TabSpec(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFrame(
    title: String,
    tabs: List<TabSpec>,
    currentRoute: String?,
    snackbarHostState: SnackbarHostState,
    onTab: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = Ink,
    navigationColor: androidx.compose.ui.graphics.Color = Panel,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = containerColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Ink,
                    titleContentColor = Paper,
                    navigationIconContentColor = Paper,
                    actionIconContentColor = Paper,
                ),
            )
            }
        },
        bottomBar = {
            if (currentRoute != null) {
                NavigationBar(containerColor = navigationColor) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { onTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
fun LoadingPane(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Teal, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        Text(message, color = Mist, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun MessagePane(title: String, detail: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            detail,
            color = Mist,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            androidx.compose.material3.TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun Metric(label: String, value: String, tone: androidx.compose.ui.graphics.Color = Paper) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = Mist, style = MaterialTheme.typography.labelSmall)
        Text(value, color = tone, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SummaryCard(label: String, value: String, tone: androidx.compose.ui.graphics.Color = Paper, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .then(Modifier),
    ) {
        androidx.compose.material3.Surface(
            color = PanelRaised,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(label, color = Mist, style = MaterialTheme.typography.labelMedium)
                Text(value, color = tone, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        color = Gold,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
}

@Composable
fun AddButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, containerColor = Teal, contentColor = Ink) {
        Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun KeyValueRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = Paper) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Mist, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = FontWeight.Medium)
    }
}
