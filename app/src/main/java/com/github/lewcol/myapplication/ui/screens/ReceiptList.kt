package com.github.lewcol.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.lewcol.myapplication.R
import com.github.lewcol.myapplication.model.ReceiptWithItems
import com.github.lewcol.myapplication.utils.millisToLocalDate
import com.github.lewcol.myapplication.viewmodel.ReceiptViewModel

@Composable
fun ReceiptButton(navController: NavHostController, fullReceipt: ReceiptWithItems) {
    Card (
        Modifier
            .fillMaxWidth()
            .padding(horizontal=8.dp, vertical=4.dp)
            .clickable{navController.navigate("receiptdetails/${fullReceipt.receipt.receiptId}")},
        elevation= CardDefaults.elevatedCardElevation(2.dp),
        shape=MaterialTheme.shapes.medium,
        colors=CardDefaults.cardColors(
            containerColor=MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row (
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment=Alignment.CenterVertically
        ) {
            Image(
                painter=painterResource(id=R.drawable.outline_description_24),
                contentDescription="Receipt Icon",
                modifier=Modifier.padding(8.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text("%s - %s".format(fullReceipt.receipt.storeName, millisToLocalDate(fullReceipt.receipt.date)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptList(navController: NavHostController, viewModel: ReceiptViewModel) {
    val receipts by viewModel.receipts.collectAsState()

    Scaffold (
        topBar={
            TopAppBar(
                colors= TopAppBarDefaults.topAppBarColors(
                    containerColor=MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor=MaterialTheme.colorScheme.primary
                ),
                title={Text("Receipt List")}
            )
        }
    ){ innerPadding ->
        LazyColumn (
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){ items(receipts) { receipt ->
            ReceiptButton(navController, receipt)
        } }
    }
}