package com.nunchuk.android.main.membership.byzantine.payment.address.whitelist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textColorMid
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel
import de.palm.composestateevents.EventEffect


@Composable
fun WhitelistAddressRoute(
    recurringPaymentViewModel: RecurringPaymentViewModel,
    whitelistAddressViewModel: WhitelistAddressViewModel = hiltViewModel(),
    openPaymentFrequencyScreen: () -> Unit,
) {
    val state by whitelistAddressViewModel.state.collectAsStateWithLifecycle()
    EventEffect(state.openNextScreenEvent, onConsumed = whitelistAddressViewModel::onOpenNextScreenEventConsumed) {
        openPaymentFrequencyScreen()
    }
    EventEffect(state.invalidAddressEvent, onConsumed = whitelistAddressViewModel::onInvalidAddressEventConsumed) {

    }
    WhitelistAddressScreen(
        openPaymentFrequencyScreen = openPaymentFrequencyScreen,
        checkAddress = whitelistAddressViewModel::checkAddressValid
    )
}

@Composable
fun WhitelistAddressScreen(
    openPaymentFrequencyScreen: () -> Unit = {},
    checkAddress: (List<String>) -> Unit = {},
) {
    var addresses by rememberSaveable {
        mutableStateOf(listOf(""))
    }
    var batchAddress by rememberSaveable {
        mutableStateOf("")
    }
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(0)
    }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_use_whitelisted_addresses),
                textStyle = NunchukTheme.typography.titleLarge,
                isBack = false
            )
        }, bottomBar = {
            Column {
                if (selectedTabIndex == 0) {
                    NcOutlineButton(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                            .fillMaxWidth(),
                        onClick = { addresses = addresses + "" },
                    ) {
                        Image(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(24.dp),
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = "Icon Add",
                        )
                        Text(text = stringResource(R.string.nc_add_address))
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = {
                        if (selectedTabIndex == 0) {
                            checkAddress(addresses)
                        } else {
                            checkAddress(batchAddress.split(",").map { it.trim() })
                        }
                    },
                    enabled = (addresses.isNotEmpty() && addresses.all { it.isNotEmpty() }) || batchAddress.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 12.dp),
                            text = stringResource(R.string.nc_enter_addresses),
                        )
                    }
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 12.dp),
                            text = stringResource(R.string.nc_batch_import),
                        )
                    }
                }
                if (selectedTabIndex == 0) {
                    EnterAddressView(
                        addresses = addresses,
                        onAddressChange = { index, s ->
                            addresses = addresses.toMutableList().apply {
                                set(index, s)
                            }
                        },
                        onRemoveAddress = { index ->
                            addresses = addresses.toMutableList().apply {
                                removeAt(index)
                            }
                        },
                    )
                } else {
                    BatchImportView(batchAddress) {
                        batchAddress = it
                    }
                }
            }
        }
    }
}

@Composable
private fun EnterAddressView(
    addresses: List<String>,
    onAddressChange: (Int, String) -> Unit = { _, _ -> },
    onRemoveAddress: (Int) -> Unit = {},
) {
    LazyColumn {
        itemsIndexed(addresses) { index, address ->
            EnterAddressItem(
                index = index.inc(),
                value = address,
                onValueChange = { onAddressChange(index, it) },
                onRemoveAddress = { onRemoveAddress(index) }
            )
        }
    }
}

@Composable
private fun BatchImportView(
    addresses: String,
    onAddressesChange: (String) -> Unit = {},
) {
    NcTextField(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxSize(),
        title = stringResource(R.string.nc_addresses),
        value = addresses,
        onValueChange = onAddressesChange,
        minLines = 7,
        maxLines = 7,
        placeholder = {
            Text(
                text = stringResource(R.string.nc_batch_import_addresses_place_holder),
                style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textColorMid)
            )
        }
    )
}

@Preview
@Composable
fun WhitelistAddressScreenPreview() {
    WhitelistAddressScreen()
}