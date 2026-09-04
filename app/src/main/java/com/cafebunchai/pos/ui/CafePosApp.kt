package com.cafebunchai.pos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Home
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cafebunchai.pos.data.AppContainer
import com.cafebunchai.pos.data.auth.AuthState
import com.cafebunchai.pos.data.auth.StaffSession
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
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val tabs = buildList {
        if (session.isAdmin) add(Tab("home", "Home", Icons.Outlined.Home))
        add(Tab("order", "Order", Icons.Outlined.LocalCafe))
        add(Tab("register", "Register", Icons.Outlined.ReceiptLong))
        if (session.isAdmin) {
            add(Tab("menu", "Menu", Icons.Outlined.MenuBook))
        }
    }
    val roleLabel = if (session.isAdmin) "Admin" else "Staff"
    val start = if (session.isAdmin) "home" else "order"
    var confirmSignOut by remember { mutableStateOf(false) }

    fun goTab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching { container.repository.hydrateFromCloud() }
        container.repository.observeCloud().collect { snapshot ->
            container.repository.applyCloud(snapshot)
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
        ) {
            if (session.isAdmin) {
                composable("home") {
                    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container.repository))
                    DashboardScreen(
                        vm = vm,
                        onOpenRegister = { goTab("register") },
                        onOpenOrder = { goTab("order") },
                    )
                }
            }
            composable("order") {
                val vm: OrderViewModel = viewModel(factory = OrderViewModel.factory(container.repository))
                OrderScreen(vm)
            }
            composable("register") {
                val vm: RegisterViewModel = viewModel(factory = RegisterViewModel.factory(container.repository))
                RegisterScreen(vm, isAdmin = session.isAdmin)
            }
            if (session.isAdmin) {
                composable("menu") {
                    val vm: MenuViewModel = viewModel(factory = MenuViewModel.factory(container.repository))
                    MenuScreen(vm)
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
