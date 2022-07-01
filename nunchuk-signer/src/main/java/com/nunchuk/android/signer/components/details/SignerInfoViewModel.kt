package com.nunchuk.android.signer.components.details

import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.components.details.SignerInfoEvent.*
import com.nunchuk.android.type.HealthStatus
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.*
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SignerInfoViewModel @Inject constructor(
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase,
    private val deleteRemoteSignerUseCase: DeleteRemoteSignerUseCase,
    private val updateMasterSignerUseCase: UpdateMasterSignerUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val healthCheckMasterSignerUseCase: HealthCheckMasterSignerUseCase,
    private val sendSignerPassphrase: SendSignerPassphrase,
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase
) : NunchukViewModel<SignerInfoState, SignerInfoEvent>() {

    override val initialState = SignerInfoState()

    lateinit var id: String
    private var signerType: SignerType = SignerType.SOFTWARE

    fun init(id: String, type: SignerType) {
        this.id = id
        this.signerType = type
        viewModelScope.launch {
            if (shouldLoadMasterSigner(signerType)) {
                when (val result = getMasterSignerUseCase.execute(id)) {
                    is Success -> updateState { copy(masterSigner = result.data) }
                    is Error -> Log.e(TAG, "get software signer error", result.exception)
                }
            } else {
                when (val result = getRemoteSignerUseCase.execute(id)) {
                    is Success -> updateState { copy(remoteSigner = result.data) }
                    is Error -> Log.e(TAG, "get remote signer error", result.exception)
                }
            }
        }

    }

    fun handleEditCompletedEvent(updateSignerName: String) {
        viewModelScope.launch {
            val state = getState()
            if (shouldLoadMasterSigner(signerType)) {
                state.masterSigner?.let {
                    when (val result = updateMasterSignerUseCase.execute(masterSigner = it.copy(name = updateSignerName))) {
                        is Success -> event(UpdateNameSuccessEvent(updateSignerName))
                        is Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            } else {
                state.remoteSigner?.let {
                    when (val result = updateRemoteSignerUseCase.execute(signer = it.copy(name = updateSignerName))) {
                        is Success -> event(UpdateNameSuccessEvent(updateSignerName))
                        is Error -> event(UpdateNameErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    fun handleRemoveSigner() {
        viewModelScope.launch {
            val state = getState()
            if (shouldLoadMasterSigner(signerType)) {
                state.masterSigner?.let {
                    when (val result = deleteMasterSignerUseCase.execute(
                        masterSignerId = it.id
                    )) {
                        is Success -> event(RemoveSignerCompletedEvent)
                        is Error -> event(RemoveSignerErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            } else {
                state.remoteSigner?.let {
                    when (val result = deleteRemoteSignerUseCase.execute(
                        masterFingerprint = it.masterFingerprint,
                        derivationPath = it.derivationPath
                    )) {
                        is Success -> event(RemoveSignerCompletedEvent)
                        is Error -> event(RemoveSignerErrorEvent(result.exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    fun handleHealthCheck(masterSigner: MasterSigner, passPhrase: String? = null) {
        if (passPhrase != null) {
            viewModelScope.launch {
                sendSignerPassphrase.execute(masterSigner.id, passPhrase)
                    .flowOn(Dispatchers.IO)
                    .onException { event(HealthCheckErrorEvent(it.message.orEmpty())) }
                    .flowOn(Dispatchers.Main)
                    .collect { healthCheck(masterSigner) }
            }
        } else {
            healthCheck(masterSigner)
        }
    }

    private fun healthCheck(masterSigner: MasterSigner) {
        viewModelScope.launch {
            healthCheckMasterSignerUseCase.execute(
                fingerprint = masterSigner.device.masterFingerprint,
                message = "",
                signature = "",
                path = masterSigner.device.path,
                masterSignerId = if (masterSigner.device.needPassPhraseSent) masterSigner.id else null
                )
                .flowOn(Dispatchers.IO)
                .onException { event(HealthCheckErrorEvent(it.message)) }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (it == HealthStatus.SUCCESS) {
                        event(HealthCheckSuccessEvent)
                    } else {
                        event(HealthCheckErrorEvent())
                    }
                }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            val result = getTapSignerBackupUseCase(GetTapSignerBackupUseCase.Data(isoDep, cvc))
            if (result.isSuccess) {
                event(GetTapSignerBackupKeyEvent(result.getOrThrow()))
            }
        }
    }

    private fun shouldLoadMasterSigner(type: SignerType) = type == SignerType.SOFTWARE || type == SignerType.NFC

    companion object {
        private const val TAG = "SignerInfoViewModel"
    }

}