package com.example.cashlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cashlog.ui.EditTransactionScreen
import com.example.cashlog.ui.HistoryScreen
import com.example.cashlog.ui.MainScreen
import com.example.cashlog.ui.AdjustmentScreen
import com.example.cashlog.ui.SettingsScreen
import com.example.cashlog.ui.theme.CashLogTheme
import com.example.cashlog.util.ExportUtils
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class MainActivity : ComponentActivity() {

    private val viewModel: CashLogViewModel by viewModels()
    private var selectedCharset: Charset = Charsets.UTF_8

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val transactions = viewModel.getAllTransactionsSnapshot()
                val csv = ExportUtils.generateCsv(transactions)
                ExportUtils.writeToUri(this@MainActivity, it, csv, selectedCharset)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            CashLogTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var showExportDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is CashLogViewModel.UiEvent.ShowUndoSnackbar -> {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Transaction saved",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoTransaction(event.transactionId)
                                }
                            }
                        }
                    }
                }

                if (showExportDialog) {
                    AlertDialog(
                        onDismissRequest = { showExportDialog = false },
                        title = { Text("CSV Export Encoding") },
                        text = { Text("Choose encoding for compatibility with your software.") },
                        confirmButton = {
                            TextButton(onClick = { 
                                selectedCharset = Charsets.UTF_8
                                showExportDialog = false
                                createDocumentLauncher.launch("CashLog_${System.currentTimeMillis()}.csv")
                            }) { Text("UTF-8 (Modern)") }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                selectedCharset = Charset.forName("x-windows-949")
                                showExportDialog = false
                                createDocumentLauncher.launch("CashLog_${System.currentTimeMillis()}.csv")
                            }) { Text("CP949 (Excel 2003 Compatible)") }
                        }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        val bottomBarRoutes = listOf("main", "history", "reconcile", "settings")
                        if (currentRoute in bottomBarRoutes) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    selected = currentRoute == "main",
                                    onClick = { navController.navigate("main") }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                    label = { Text("History") },
                                    selected = currentRoute == "history",
                                    onClick = { navController.navigate("history") }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Tune, contentDescription = "Reconcile") },
                                    label = { Text("Reconcile") },
                                    selected = currentRoute == "reconcile",
                                    onClick = { navController.navigate("reconcile") }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    selected = currentRoute == "settings",
                                    onClick = { navController.navigate("settings") }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("main") {
                            LaunchedEffect(Unit) {
                                if (!viewModel.hasInitialTransaction()) {
                                    navController.navigate("reconcile")
                                }
                            }
                            MainScreen(
                                viewModel = viewModel,
                                onSave = { type ->
                                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                                    if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                            viewModel.saveTransaction(type, location?.latitude, location?.longitude)
                                        }.addOnFailureListener {
                                            viewModel.saveTransaction(type, null, null)
                                        }
                                    } else {
                                        viewModel.saveTransaction(type, null, null)
                                    }
                                }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                viewModel = viewModel,
                                onEditTransaction = { id -> 
                                    navController.navigate("edit/$id")
                                },
                                onExport = { showExportDialog = true }
                            )
                        }
                        composable(
                            route = "edit/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: return@composable
                            EditTransactionScreen(
                                transactionId = id,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("reconcile") {
                            AdjustmentScreen(
                                viewModel = viewModel,
                                onAdjustmentComplete = { 
                                    navController.navigate("main") {
                                        popUpTo("reconcile") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
