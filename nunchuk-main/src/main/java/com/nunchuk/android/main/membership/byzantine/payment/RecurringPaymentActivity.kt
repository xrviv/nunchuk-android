package com.nunchuk.android.main.membership.byzantine.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.main.membership.byzantine.payment.address.whitelist.addWhitelistAddress
import com.nunchuk.android.main.membership.byzantine.payment.address.whitelist.navigateToWhitelistAddress
import com.nunchuk.android.main.membership.byzantine.payment.amount.addPaymentAmount
import com.nunchuk.android.main.membership.byzantine.payment.amount.navigateToPaymentAmount
import com.nunchuk.android.main.membership.byzantine.payment.cosign.addPaymentCosignScreen
import com.nunchuk.android.main.membership.byzantine.payment.cosign.navigatePaymentCosign
import com.nunchuk.android.main.membership.byzantine.payment.detail.addRecurringPaymentDetail
import com.nunchuk.android.main.membership.byzantine.payment.detail.navigateToRecurringPaymentDetail
import com.nunchuk.android.main.membership.byzantine.payment.feerate.addPaymentFeeRateScreen
import com.nunchuk.android.main.membership.byzantine.payment.feerate.navigateToPaymentFeeRate
import com.nunchuk.android.main.membership.byzantine.payment.frequent.addPaymentFrequency
import com.nunchuk.android.main.membership.byzantine.payment.frequent.navigateToPaymentFrequency
import com.nunchuk.android.main.membership.byzantine.payment.list.recurringPaymentsList
import com.nunchuk.android.main.membership.byzantine.payment.name.addPaymentName
import com.nunchuk.android.main.membership.byzantine.payment.name.navigateToPaymentName
import com.nunchuk.android.main.membership.byzantine.payment.note.addPaymentNoteScreen
import com.nunchuk.android.main.membership.byzantine.payment.note.navigateToPaymentNote
import com.nunchuk.android.main.membership.byzantine.payment.paymentpercentage.addPaymentPercentageCalculation
import com.nunchuk.android.main.membership.byzantine.payment.paymentpercentage.navigateToPaymentPercentageCalculation
import com.nunchuk.android.main.membership.byzantine.payment.selectmethod.addPaymentSelectAddressType
import com.nunchuk.android.main.membership.byzantine.payment.selectmethod.navigateToPaymentSelectAddressType
import com.nunchuk.android.main.membership.byzantine.payment.summary.addPaymentSummary
import com.nunchuk.android.main.membership.byzantine.payment.summary.navigateToPaymentSummary
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecurringPaymentActivity : AppCompatActivity() {

    private val viewModel: RecurringPaymentViewModel by viewModels()

    private val groupId: String by lazy {
        intent.getStringExtra(GROUP_ID).orEmpty()
    }

    private val walletId: String by lazy {
        intent.getStringExtra(WALLET_ID).orEmpty()
    }

    @Inject
    lateinit var navigator: NunchukNavigator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "recurring_payment/${groupId}/${walletId}"
                    ) {
                        recurringPaymentsList(
                            onOpenAddRecurringPayment = {
                                navController.navigateToPaymentName()
                            },
                            groupId = groupId,
                            walletId = walletId,
                            onOpenRecurringPaymentDetail = { recurringPaymentId ->
                                navController.navigateToRecurringPaymentDetail(
                                    groupId = groupId,
                                    walletId = walletId,
                                    recurringPaymentId = recurringPaymentId,
                                )
                            },
                        )
                        addPaymentName(
                            recurringPaymentViewModel = viewModel,
                            openPaymentAmountScreen = {
                                navController.navigateToPaymentAmount()
                            },
                        )
                        addPaymentAmount(
                            recurringPaymentViewModel = viewModel,
                            openCalculateScreen = {
                                navController.navigateToPaymentPercentageCalculation()
                            },
                            openSelectAddressTypeScreen = {
                                navController.navigateToPaymentSelectAddressType()
                            },
                        )
                        addPaymentSelectAddressType(
                            recurringPaymentViewModel = viewModel,
                            openWhiteListAddressScreen = {
                                navController.navigateToWhitelistAddress()
                            },
                            openScanQRCodeScreen = {
                                navigator.openRecoverWalletQRCodeScreen(
                                    this@RecurringPaymentActivity,
                                    false
                                )
                            },
                        )
                        addPaymentPercentageCalculation(
                            recurringPaymentViewModel = viewModel,
                            openSelectAddressTypeScreen = {
                                navController.navigateToPaymentSelectAddressType()
                            },
                        )
                        addWhitelistAddress(
                            paymentViewModel = viewModel,
                            openPaymentFrequencyScreen = {
                                navController.navigateToPaymentFrequency()
                            },
                        )
                        addPaymentFrequency(
                            recurringPaymentViewModel = viewModel,
                            openPaymentFeeRateScreen = {
                                navController.navigateToPaymentFeeRate()
                            },
                        )
                        addPaymentCosignScreen(
                            recurringPaymentViewModel = viewModel,
                            openPaymentNote = {
                                navController.navigateToPaymentNote()
                            },
                        )
                        addPaymentNoteScreen(
                            recurringPaymentViewModel = viewModel,
                            openSummaryScreen = {
                                navController.navigateToPaymentSummary()
                            },
                        )
                        addPaymentFeeRateScreen(
                            recurringPaymentViewModel = viewModel,
                            openPaymentCosignScreen = {
                                navController.navigatePaymentCosign()
                            },
                        )
                        addPaymentSummary(
                            recurringPaymentViewModel = viewModel,
                            openDummyTransactionScreen = ::openWalletAuthentication,
                        )
                        addRecurringPaymentDetail(
                            onOpenDummyTransaction = ::openWalletAuthentication,
                        )
                    }
                }
            }
        )
    }

    private fun openWalletAuthentication(payload: DummyTransactionPayload) {
        navigator.openWalletAuthentication(
            walletId = payload.walletId,
            userData = "",
            requiredSignatures = payload.requiredSignatures,
            type = VerificationType.SIGN_DUMMY_TX,
            null,
            activityContext = this@RecurringPaymentActivity,
            groupId = groupId,
            dummyTransactionId = payload.dummyTransactionId,
        )
    }

    companion object {
        internal const val GROUP_ID = "group_id"
        internal const val WALLET_ID = "wallet_id"

        fun navigate(
            activity: Context,
            groupId: String,
            walletId: String?,
        ) {
            val intent = Intent(activity, RecurringPaymentActivity::class.java).apply {
                putExtra(GROUP_ID, groupId)
                putExtra(WALLET_ID, walletId)
            }
            activity.startActivity(intent)
        }
    }
}