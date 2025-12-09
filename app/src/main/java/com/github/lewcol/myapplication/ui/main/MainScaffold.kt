package com.github.lewcol.myapplication.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.lewcol.myapplication.R
import com.github.lewcol.myapplication.ui.screens.ReceiptList
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel

sealed class Screen(val route: String) {
    object ReceiptList : Screen("receiptlist")
    object ReceiptDetails : Screen("receiptdetails/{id}")
    object Reports : Screen("reports")
    object Scan : Screen("scan")
}

data class NavItem(
    var label: String,
    val icon: ImageVector,
    val screen: Screen
)

@Composable
fun MainScaffold(viewModel: ReceiptViewModel) {
    val navController = rememberNavController()
    val navItemList = listOf(
        NavItem(label="Task List", icon= Icons.AutoMirrored.Rounded.List, screen=Screen.ReceiptList),
        NavItem(label="Scan", icon= ImageVector.vectorResource(R.drawable.rounded_add_a_photo_24), screen=Screen.Scan),
        NavItem(label="Reports", icon=ImageVector.vectorResource(R.drawable.outline_assignment_24), screen=Screen.Reports)
    )
    var selectedIndex by rememberSaveable{mutableIntStateOf(0)}

    Scaffold(
        modifier=Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar {
                NavigationBar {
                    navItemList.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected=selectedIndex==index,
                            onClick = {
                                selectedIndex=index
                                if (navController.currentDestination?.route != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon={Icon(imageVector=item.icon, contentDescription=item.label)},
                            label={Text(text=item.label)}
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController=navController,
            startDestination=Screen.ReceiptList.route,
            modifier=Modifier.padding(innerPadding)
        ) {
            composable(Screen.ReceiptList.route) { ReceiptList(navController, viewModel) }
            //composable(Screen.ReceiptDetails.route) { backStackEntry ->
            //    val id = backStackEntry.arguments?.getString("id")?.toInt()
            //    id?.let { ReceiptDetails(navController, viewModel, it) }
            //}
            //composable(Screen.Reports.route) { Reports(viewModel) }
            //composable(Screen.Scan.route) { Scan(viewModel) }
        }
    }
}