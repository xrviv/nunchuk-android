package com.nunchuk.android.main.membership.key.desktop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerTag

class RequestAddKeySuccessFragment : MembershipFragment() {
    private val args: RequestAddKeySuccessFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
                RequestAddKeySuccessContent(
                    remainingTime = remainingTime,
                    tag = args.signerTag,
                    onContinueClick = {
                        findNavController().popBackStack()
                    },
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }
}

@Composable
private fun RequestAddKeySuccessContent(
    remainingTime: Int = 0,
    tag: SignerTag = SignerTag.LEDGER,
    onContinueClick: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClick
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_text_continue)
                    )
                }
            },
            topBar = {
                NcTopAppBar(
                    elevation = 0.dp,
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NcCircleImage(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterHorizontally),
                    iconSize = 60.dp,
                    iconTintColor = Color(0xFF1C652D),
                    color = colorResource(id = com.nunchuk.android.signer.R.color.nc_green_color),
                    resId = com.nunchuk.android.signer.R.drawable.ic_check,
                )
                Text(
                    modifier = Modifier.padding(24.dp),
                    text = stringResource(
                        R.string.nc_added_successfully,
                        if (tag == SignerTag.LEDGER)
                            stringResource(id = R.string.nc_ledger)
                        else stringResource(id = R.string.nc_trezor)
                    ),
                    style = NunchukTheme.typography.heading
                )
            }
        }
    }
}

@Preview
@Composable
fun RequestAddKeySuccessContentPreview(
) {
    RequestAddKeySuccessContent()
}