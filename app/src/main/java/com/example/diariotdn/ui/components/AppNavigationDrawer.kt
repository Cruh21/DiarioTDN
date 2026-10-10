package com.example.diariotdn.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diariotdn.R
import com.example.diariotdn.ui.screens.BackupScreen
import com.example.diariotdn.ui.screens.ConfigCategoriesScreen
import com.example.diariotdn.ui.screens.DashboardScreen
import com.example.diariotdn.ui.screens.HistoryScreen
import com.example.diariotdn.ui.screens.LotesScreen
import com.example.diariotdn.ui.screens.MainScreen
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import kotlinx.coroutines.launch

enum class ScreenOption(val title: String) {
    DASHBOARD("Panel Principal"),
    TODAY("Registro del Día"),
    HISTORY("Historial de Registros"),
    LOTES("Gestión de Lotes"),
    BACKUP("Copia de Seguridad"),
    CONFIG("Configurar Categorías")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    viewModel: DailyRecordViewModel
) {
    var currentScreen by remember { mutableStateOf(ScreenOption.DASHBOARD) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Manejo de la tecla/gesto "Atrás": si el menú está abierto, se cierra. Si estamos en otra pantalla, vuelve a Dashboard.
    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    } else if (currentScreen != ScreenOption.DASHBOARD) {
        BackHandler {
            currentScreen = ScreenOption.DASHBOARD
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Header del Menú Lateral con Logo Empresa Centrado
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_nazareth),
                            contentDescription = "Logo Nazareth",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Diario TDN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Gestión de Producción",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // BLOQUE 1: PRODUCCIÓN
                    Text(
                        text = "PRODUCCIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null
                            )
                        },
                        label = { Text("Dashboard") },
                        selected = currentScreen == ScreenOption.DASHBOARD,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreen = ScreenOption.DASHBOARD
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null
                            )
                        },
                        label = { Text("Dia") },
                        selected = currentScreen == ScreenOption.TODAY,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreen = ScreenOption.TODAY
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null
                            )
                        },
                        label = { Text("Historial") },
                        selected = currentScreen == ScreenOption.HISTORY,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreen = ScreenOption.HISTORY
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null
                            )
                        },
                        label = { Text("Lotes") },
                        selected = currentScreen == ScreenOption.LOTES,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreen = ScreenOption.LOTES
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // BLOQUE 2: SISTEMA
                    Text(
                        text = "SISTEMA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null
                            )
                        },
                        label = { Text("Copia de Seguridad") },
                        selected = currentScreen == ScreenOption.BACKUP,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreen = ScreenOption.BACKUP
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
                            )
                        },
                        label = { Text("Categorías") },
                        selected = currentScreen == ScreenOption.CONFIG,
                        onClick = {
                            scope.launch { drawerState.close() }
                            currentScreen = ScreenOption.CONFIG
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú lateral"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    ScreenOption.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToToday = { currentScreen = ScreenOption.TODAY },
                        onNavigateToLotes = { currentScreen = ScreenOption.LOTES }
                    )
                    ScreenOption.TODAY -> MainScreen(viewModel = viewModel)
                    ScreenOption.HISTORY -> HistoryScreen(viewModel = viewModel)
                    ScreenOption.LOTES -> LotesScreen(viewModel = viewModel)
                    ScreenOption.BACKUP -> BackupScreen(viewModel = viewModel)
                    ScreenOption.CONFIG -> ConfigCategoriesScreen(viewModel = viewModel)
                }
            }
        }
    }
}
