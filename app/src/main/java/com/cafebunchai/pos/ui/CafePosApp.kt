package com.cafebunchai.pos.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cafebunchai.pos.data.AppContainer
import com.cafebunchai.pos.data.auth.AuthState
import com.cafebunchai.pos.data.auth.StaffSession
import com.cafebunchai.pos.notify.StockAlerts
import com.cafebunchai.pos.ui.auth.AuthViewModel
import com.cafebunchai.pos.ui.auth.LoginScreen
import com.cafebunchai.pos.ui.dashboard.DashboardScreen
import com.cafebunchai.pos.ui.dashboard.DashboardViewModel
import com.cafebunchai.pos.ui.menu.MenuScreen
import com.cafebunchai.pos.ui.menu.MenuViewModel
import com.cafebunchai.pos.ui.order.OrderScreen
import com.cafebunchai.pos.ui.order.OrderViewModel
import com.cafebunchai.pos.ui.register.RegisterScreen
import com.cafebunchai.pos.ui.register.RegisterViewModel
import com.cafebunchai.pos.ui.stock.StockScreen
import com.cafebunchai.pos.ui.stock.StockViewModel

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafePosApp(container: AppContainer) {
    val authVm: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.auth))
    val authState by authVm.authState.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthState.SignedOut -> LoginScreen(authVm)
        is AuthState.SignedIn -> PosShell(container, state.session, authVm::signOut)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosShell(
    container: AppContainer,
    session: StaffSession,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val tabs = buildList {
        if (session.isAdmin) add(Tab("home", "Home", Icons.Outlined.Home))
        add(Tab("order", "Order", Icons.Outlined.LocalCafe))
        add(Tab("register", "Register", Icons.Outlined.ReceiptLong))
        if (session.isAdmin) {
            add(Tab("stock", "Stock", Icons.Outlined.Inventory2))
            add(Tab("menu", "Menu", Icons.Outlined.MenuBook))
        }
    }
    val roleLabel = if (session.isAdmin) "Admin" else "Staff"
    val start = if (session.isAdmin) "home" else "order"
    var confirmSignOut by remember { mutableStateOf(false) }
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val repo = container.repository
    val orderVm: OrderViewModel = viewModel(
        key = "order-${session.uid}",
        factory = OrderViewModel.factory(repo),
    )
    val registerVm: RegisterViewModel = viewModel(
        key = "register-${session.uid}",
        factory = RegisterViewModel.factory(repo),
    )
    val dashboardVm: DashboardViewModel = viewModel(
        key = "home-${session.uid}",
        factory = DashboardViewModel.factory(repo),
    )
    val stockVm: StockViewModel = viewModel(
        key = "stock-${session.uid}",
        factory = StockViewModel.factory(repo),
    )
    val menuVm: MenuViewModel = viewModel(
        key = "menu-${session.uid}",
        factory = MenuViewModel.factory(repo),
    )

    fun goTab(route: String) {
        if (current == route) return
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(Unit) {
        StockAlerts.createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching { container.repository.hydrateFromCloud() }
        launch {
            container.repository.observeCloudTickets().collect { tickets ->
                runCatching { container.repository.applyLiveTickets(tickets) }
            }
        }
        launch {
            container.repository.observeCloudInventory().collect { items ->
                runCatching { container.repository.applyLiveInventory(items) }
            }
        }
        launch {
            container.repository.observeCloudCatalog().collect { (cats, menu, recipes) ->
                runCatching { container.repository.applyLiveCatalog(cats, menu, recipes) }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cafe Bun Chai")
                        Text(
                            "$roleLabel · ${session.email}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            maxLines = 1,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { confirmSignOut = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { goTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            if (session.isAdmin) {
                composable("home") {
                    DashboardScreen(
                        vm = dashboardVm,
                        onOpenStock = { goTab("stock") },
                        onOpenRegister = { goTab("register") },
                        onOpenOrder = { goTab("order") },
                    )
                }
            }
            composable("order") {
                OrderScreen(orderVm)
            }
            composable("register") {
                RegisterScreen(registerVm, isAdmin = session.isAdmin)
            }
            if (session.isAdmin) {
                composable("stock") {
                    StockScreen(stockVm)
                }
                composable("menu") {
                    MenuScreen(menuVm)
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to log in again to use the till.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        onSignOut()
                    },
                ) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Stay") }
            },
        )
    }
}
