/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.auth.components.authentication

import android.app.Activity
import android.content.Intent
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.databinding.FragmentSignInDummyTransactionDetailsBinding
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.BottomSheetTooltip
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.bindTransactionStatus
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.core.util.truncatedAddress
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.crypto.model.UnknownInfo.deviceId
import javax.inject.Inject

@AndroidEntryPoint
class SignInDummyTransactionDetailsFragment :
    BaseFragment<FragmentSignInDummyTransactionDetailsBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var pushEventManager: PushEventManager

    private val viewModel: DummyTransactionDetailsViewModel by viewModels()
    private val signInAuthenticationViewModel: SignInAuthenticationViewModel by activityViewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()
    private val controller: IntentSharingController by lazy {
        IntentSharingController.from(
            requireActivity()
        )
    }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.importTransactionViaFile(it)
            }
        }

    private val importTxLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null && it.resultCode == Activity.RESULT_OK) {
                val transaction = data.parcelable<Transaction>(GlobalResultKey.TRANSACTION_EXTRA)
                    ?: return@registerForActivityResult
                signInAuthenticationViewModel.handleImportAirgapTransaction(transaction)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            TransactionOption.EXPORT_TRANSACTION.ordinal -> showExportTransactionOptions()
            TransactionOption.IMPORT_TRANSACTION.ordinal -> showImportTransactionOptions()
            SheetOptionType.TYPE_EXPORT_QR -> openExportTransactionScreen(false)
            SheetOptionType.TYPE_EXPORT_BBQR -> openExportTransactionScreen(true)
            SheetOptionType.TYPE_EXPORT_FILE -> viewModel.exportTransactionToFile(
                signInAuthenticationViewModel.getDataToSign()
            )

            SheetOptionType.TYPE_IMPORT_QR -> openImportTransactionScreen()
            SheetOptionType.TYPE_IMPORT_FILE -> importFileLauncher.launch("*/*")
        }
    }

    private fun observeEvent() {
        flowObserver(signInAuthenticationViewModel.state, ::handleState)
        flowObserver(viewModel.state) {
            handleViewMore(it.viewMore)
        }
        flowObserver(viewModel.event) {
            when (it) {
                is DummyTransactionDetailEvent.ExportToFileSuccess -> shareTransactionFile(it.filePath)
                is DummyTransactionDetailEvent.ImportTransactionSuccess -> signInAuthenticationViewModel.handleImportAirgapTransaction(
                    it.transaction ?: return@flowObserver
                )

                is DummyTransactionDetailEvent.LoadingEvent -> showOrHideLoading(it.isLoading)
                is DummyTransactionDetailEvent.TransactionError -> showError(it.error)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            signInAuthenticationViewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is SignInAuthenticationEvent.ScanTapSigner -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )

                        SignInAuthenticationEvent.ScanColdCard -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION
                        )

                        is SignInAuthenticationEvent.ProcessFailure -> showError(event.message)

                        is SignInAuthenticationEvent.NfcLoading -> showOrHideNfcLoading(
                            event.isLoading,
                            event.isColdCard
                        )

                        SignInAuthenticationEvent.ShowAirgapOption -> handleMenuMore()
                        SignInAuthenticationEvent.ExportTransactionToColdcardSuccess -> handleExportToColdcardSuccess()
                        SignInAuthenticationEvent.CanNotSignDummyTx -> showError(getString(R.string.nc_can_not_sign_please_try_again))
                        SignInAuthenticationEvent.CanNotSignHardwareKey -> showError(getString(R.string.nc_use_desktop_app_to_sign))

                        is SignInAuthenticationEvent.SignFailed -> {}
                        is SignInAuthenticationEvent.Loading,
                        is SignInAuthenticationEvent.ShowError,
                        -> Unit

                        is SignInAuthenticationEvent.SignInSuccess -> {
                                hideLoading()
                                if (NotificationUtils.areNotificationsEnabled(requireContext()).not()) {
                                    navigator.openTurnNotificationScreen(requireActivity())
                                } else {
                                    navigator.openMainScreen(requireActivity(), isClearTask = true)
                                }
                                requireActivity().finish()
                        }
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }) { info ->
            signInAuthenticationViewModel.getInteractSingleSigner()?.let {
                signInAuthenticationViewModel.handleTapSignerSignCheckMessage(
                    it,
                    info,
                    nfcViewModel.inputCvc.orEmpty()
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION }) { scanInfo ->
            signInAuthenticationViewModel.handleExportTransactionToMk4(
                Ndef.get(scanInfo.tag) ?: return@flowObserver
            )
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }) {
            signInAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                signInAuthenticationViewModel.generateSignatureFromColdCardPsbt(signer, it.records)
            }
            nfcViewModel.clearScanInfo()
        }
    }

    private fun shareTransactionFile(filePath: String) {
        controller.shareFile(filePath)
    }

    private fun handleExportToColdcardSuccess() {
        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE)
        showSuccess(getString(R.string.nc_transaction_exported))
    }

    private fun setupViews() {
        binding.viewMore.setOnClickListener {
            viewModel.handleViewMoreEvent()
        }
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.toolbar.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.menu_more -> {
                    handleMenuMore()
                    true
                }

                else -> false
            }
        }
        binding.estimatedFeeLabel.setOnClickListener {
            BottomSheetTooltip.newInstance(
                title = getString(R.string.nc_text_info),
                message = getString(R.string.nc_estimated_fee_tooltip),
            ).show(childFragmentManager, "BottomSheetTooltip")
        }
    }

    private fun handleMenuMore() {
        val options = mutableListOf(
            SheetOption(
                type = TransactionOption.IMPORT_TRANSACTION.ordinal,
                resId = R.drawable.ic_import,
                label = getString(R.string.nc_transaction_import_signature),
            ),
            SheetOption(
                type = TransactionOption.EXPORT_TRANSACTION.ordinal,
                resId = R.drawable.ic_export,
                label = getString(R.string.nc_transaction_export_transaction),
            ),
        )
        BottomSheetOption.newInstance(options).show(childFragmentManager, "BottomSheetOption")
    }

    private fun handleState(state: SignInAuthenticationState) {
        val transaction = state.transaction ?: return
        bindTransaction(transaction, state.pendingSignature)
        bindSigners(
            signerMap = state.signatures.mapValues { true },
            signers = state.walletSigner.sortedByDescending(SignerModel::localKey),
            status = transaction.status,
            enabledSigners = state.enabledSigners,
        )
        hideLoading()
    }

    private fun handleViewMore(viewMore: Boolean) {
        binding.viewMore.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            if (viewMore) ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_collapse
            ) else ContextCompat.getDrawable(requireContext(), R.drawable.ic_expand),
            null
        )
        binding.viewMore.text = if (viewMore) {
            getString(R.string.nc_transaction_less_details)
        } else {
            getString(R.string.nc_transaction_more_details)
        }

        binding.transactionDetailsContainer.isVisible = viewMore
    }

    private fun bindSigners(
        signerMap: Map<String, Boolean>,
        signers: List<SignerModel>,
        status: TransactionStatus,
        enabledSigners: Set<String>,
    ) {
        SignInTransactionSignersViewBinder(
            container = binding.signerListView,
            signerMap = signerMap,
            signers = signers,
            txStatus = status,
            listener = { signer ->
                signInAuthenticationViewModel.onSignerSelect(signer)
            },
            enabledSigners = enabledSigners
        ).bindItems()
    }

    private fun bindTransaction(transaction: Transaction, pendingSigners: Int) {
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.sendingTo.text = output?.first.orEmpty().truncatedAddress()
        binding.signatureStatus.isVisible = !transaction.status.hadBroadcast()
        binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_pending_signatures,
            0,
            0,
            0
        )
        binding.signatureStatus.text = resources.getQuantityString(
            R.plurals.nc_transaction_pending_signature,
            pendingSigners,
            pendingSigners
        )
        binding.status.bindTransactionStatus(transaction)
        binding.sendingBTC.text = transaction.totalAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive

        bindAddress(transaction)
        bindTransactionFee(transaction)
        bindingTotalAmount(transaction)
        bindViewSendOrReceive(transaction)
    }

    private fun bindViewSendOrReceive(transaction: Transaction) {
        binding.divider.isVisible = !transaction.isReceive
        binding.estimatedFeeBTC.isVisible = !transaction.isReceive
        binding.estimatedFeeUSD.isVisible = !transaction.isReceive
        binding.estimatedFeeLabel.isVisible = !transaction.isReceive
        binding.totalAmountLabel.isVisible = !transaction.isReceive
        binding.totalAmountBTC.isVisible = !transaction.isReceive
        binding.totalAmountUSD.isVisible = !transaction.isReceive
    }

    private fun bindAddress(transaction: Transaction) {
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.sendAddressLabel.text = output?.first.orEmpty()
        binding.sendAddressBTC.text = output?.second?.getBTCAmount().orEmpty()
        binding.sendAddressUSD.text = output?.second?.getCurrencyAmount().orEmpty()

        binding.sendingToLabel.text = getString(R.string.nc_transaction_sending_to)
        binding.sendToAddress.text = getString(R.string.nc_transaction_send_to_address)
    }

    private fun bindingTotalAmount(transaction: Transaction) {
        binding.totalAmountBTC.text = transaction.totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = transaction.totalAmount.getCurrencyAmount()
    }

    private fun bindTransactionFee(transaction: Transaction) {
        binding.estimatedFeeBTC.text = transaction.fee.getBTCAmount()
        binding.estimatedFeeUSD.text = transaction.fee.getCurrencyAmount()
    }

    private fun openExportTransactionScreen(isBBQR: Boolean) {
        navigator.openExportTransactionScreen(
            launcher = importTxLauncher,
            activityContext = requireActivity(),
            txToSign = signInAuthenticationViewModel.getDataToSign(),
            isDummyTx = true,
            isBBQR = isBBQR,
            isSignInFlow = true
        )
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(
            launcher = importTxLauncher,
            activityContext = requireActivity(),
            walletId = "",
            isSignInFlow = true,
            isDummyTx = true
        )
    }

    private fun showExportTransactionOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_QR,
                    resId = R.drawable.ic_qr,
                    label = getString(R.string.nc_export_via_qr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_BBQR,
                    resId = R.drawable.ic_qr,
                    label = getString(R.string.nc_export_via_bbqr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_FILE,
                    resId = R.drawable.ic_export,
                    label = getString(R.string.nc_export_via_file),
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showImportTransactionOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_QR,
                    resId = R.drawable.ic_qr,
                    label = getString(R.string.nc_import_via_qr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_FILE,
                    resId = R.drawable.ic_import,
                    label = getString(R.string.nc_import_via_file),
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(message)
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentSignInDummyTransactionDetailsBinding {
        return FragmentSignInDummyTransactionDetailsBinding.inflate(inflater, container, false)
    }
}