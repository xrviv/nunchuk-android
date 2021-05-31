package com.nunchuk.android.transaction.details

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.Device
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.details.TransactionDetailsEvent.*
import com.nunchuk.android.usecase.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class TransactionDetailsViewModel @Inject constructor(
    private val getBlockchainExplorerUrlUseCase: GetBlockchainExplorerUrlUseCase,
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val signTransactionUseCase: SignTransactionUseCase,
    private val broadcastTransactionUseCase: BroadcastTransactionUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val getChainTipUseCase: GetChainTipUseCase
) : NunchukViewModel<TransactionDetailsState, TransactionDetailsEvent>() {

    private lateinit var walletId: String
    private lateinit var txId: String
    private var remoteSigners: List<SingleSigner> = emptyList()
    private var masterSigners: List<MasterSigner> = emptyList()
    private var chainTip: Int = -1

    override val initialState = TransactionDetailsState()

    fun init(walletId: String, txId: String) {
        this.walletId = walletId
        this.txId = txId
        getChainTip()
    }

    private fun getChainTip() {
        viewModelScope.launch {
            chainTip = when (val result = getChainTipUseCase.execute()) {
                is Error -> -1
                is Success -> result.data
            }
        }
    }

    fun getTransactionInfo() {
        viewModelScope.launch {
            masterSigners = when (val result = getMasterSignersUseCase.execute()) {
                is Success -> result.data
                is Error -> emptyList()
            }

            remoteSigners = when (val result = getRemoteSignersUseCase.execute()) {
                is Success -> result.data
                is Error -> emptyList()
            }

            when (val result = getTransactionUseCase.execute(walletId, txId)) {
                is Success -> onRetrieveTransactionSuccess(result.data)
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

    private fun onRetrieveTransactionSuccess(transaction: Transaction) {
        updateTransaction(transaction)
    }

    private fun updateTransaction(transaction: Transaction) {
        val updatedTransaction = transaction.copy(height = transaction.getConfirmations())
        updateState { copy(transaction = updatedTransaction) }
        val signers = updatedTransaction.signers
        if (signers.isNotEmpty()) {
            val signedMasterSigners = masterSigners.filter { it.device.masterFingerprint in signers }.map(MasterSigner::toModel)
            val signedRemoteSigners = remoteSigners.filter { it.masterFingerprint in signers }.map(SingleSigner::toModel)
            updateState { copy(signers = signedMasterSigners + signedRemoteSigners) }
        }
    }

    fun handleViewMoreEvent() {
        updateState { copy(viewMore = !viewMore) }
    }

    fun handleBroadcastEvent() {
        viewModelScope.launch {
            when (val result = broadcastTransactionUseCase.execute(walletId, txId)) {
                is Success -> {
                    updateTransaction(result.data)
                    event(BroadcastTransactionSuccess)
                }
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

    fun handleViewBlockchainEvent() {
        viewModelScope.launch {
            when (val result = getBlockchainExplorerUrlUseCase.execute(txId)) {
                is Success -> event(ViewBlockchainExplorer(result.data))
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

    fun handleDeleteTransactionEvent() {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase.execute(walletId, txId)) {
                is Success -> event(DeleteTransactionSuccess)
                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
            }
        }
    }

    fun handleSignEvent(signer: SignerModel) {
        if (signer.software) {
            viewModelScope.launch {
                val fingerPrint = signer.fingerPrint
                val device = masterSigners.first { it.device.masterFingerprint == fingerPrint }.device
                if (device.needPassPhraseSent) {
                    event(PromptInputPassphrase {
                        viewModelScope.launch {
                            when (val result = sendSignerPassphrase.execute(signer.id, it)) {
                                is Success -> signTransaction(device)
                                is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
                            }
                        }
                    })
                } else {
                    signTransaction(device)
                }
            }
        } else {
            // FIXME
        }
    }

    private suspend fun signTransaction(device: Device) {
        when (val result = signTransactionUseCase.execute(walletId, txId, device)) {
            is Success -> {
                updateTransaction(result.data)
                event(SignTransactionSuccess)
            }
            is Error -> event(TransactionDetailsError(result.exception.message.orEmpty()))
        }
    }

    private fun Transaction.getConfirmations(): Int = if (height > 0) (chainTip - height + 1) else height

}