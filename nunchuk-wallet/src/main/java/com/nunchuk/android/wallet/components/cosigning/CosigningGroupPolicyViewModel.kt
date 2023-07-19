/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.wallet.components.cosigning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CalculateRequiredSignaturesUpdateGroupKeyPolicyUseCase
import com.nunchuk.android.core.domain.membership.GetGroupKeyPolicyUserDataUseCase
import com.nunchuk.android.core.domain.membership.UpdateGroupServerKeysUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.isKeyHolder
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.usecase.byzantine.GetGroupServerKeysUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CosigningGroupPolicyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGroupServerKeysUseCase: GetGroupServerKeysUseCase,
    private val updateGroupServerKeysUseCase: UpdateGroupServerKeysUseCase,
    private val calculateRequiredSignaturesUpdateGroupKeyPolicyUseCase: CalculateRequiredSignaturesUpdateGroupKeyPolicyUseCase,
    private val getGroupKeyPolicyUserDataUseCase: GetGroupKeyPolicyUserDataUseCase,
    private val getGroupUseCase: GetGroupUseCase,
) : ViewModel() {
    private val args: CosigningGroupPolicyFragmentArgs =
        CosigningGroupPolicyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CosigningGroupPolicyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CosigningGroupPolicyState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val keyPolicy = getGroupServerKeysUseCase(
                GetGroupServerKeysUseCase.Param(
                    args.groupId,
                    args.xfp
                )
            ).getOrNull()
            val group = getGroupUseCase(args.groupId).getOrNull()
            if (keyPolicy != null && group != null) {
                val members = group.members.mapNotNull { member ->
                    if (member.role.toRole.isKeyHolder) {
                        AssistedMember(
                            role = member.role,
                            email = member.emailOrUsername,
                            name = member.user?.name,
                            membershipId = member.membershipId
                        )
                    } else null
                }
                _state.update { state ->
                    state.copy(
                        keyPolicy = keyPolicy,
                        members = members,
                    )
                }
            }
        }
    }

    fun updateState(keyPolicy: GroupKeyPolicy?, isEditMode: Boolean = false) {
        keyPolicy ?: return
        _state.update {
            it.copy(
                keyPolicy = keyPolicy,
                dummyTransactionId = "",
                isUpdateFlow = isEditMode
            )
        }
    }

    fun updateServerConfig(
        signatures: Map<String, String> = emptyMap(),
        securityQuestionToken: String = "",
    ) {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.Loading(true))
            val result = updateGroupServerKeysUseCase(
                UpdateGroupServerKeysUseCase.Param(
                    body = state.value.userData,
                    keyIdOrXfp = args.xfp,
                    signatures = signatures,
                    securityQuestionToken = securityQuestionToken,
                    token = args.token,
                    groupId = args.groupId,
                )
            )
            _event.emit(CosigningGroupPolicyEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(CosigningGroupPolicyEvent.UpdateKeyPolicySuccess)
                _state.update { it.copy(isUpdateFlow = false) }
            } else {
                _event.emit(CosigningGroupPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onDiscardChangeClicked() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.OnDiscardChange)
        }
    }

    fun onSaveChangeClicked() {
        viewModelScope.launch {
            if (state.value.dummyTransactionId.isNotEmpty()) {
                _event.emit(
                    CosigningGroupPolicyEvent.OnSaveChange(
                        _state.value.requiredSignature,
                        _state.value.userData,
                        _state.value.dummyTransactionId
                    )
                )
                return@launch
            }
            _event.emit(CosigningGroupPolicyEvent.Loading(true))
            val result = calculateRequiredSignaturesUpdateGroupKeyPolicyUseCase(
                CalculateRequiredSignaturesUpdateGroupKeyPolicyUseCase.Param(
                    walletId = args.walletId,
                    keyPolicy = state.value.keyPolicy,
                    xfp = args.xfp,
                    groupId = args.groupId
                )
            )
            _event.emit(CosigningGroupPolicyEvent.Loading(false))
            if (result.isSuccess) {
                val requiredSignature = result.getOrThrow()
                getGroupKeyPolicyUserDataUseCase(
                    GetGroupKeyPolicyUserDataUseCase.Param(
                        args.walletId,
                        state.value.keyPolicy
                    )
                ).onSuccess { data ->
                    _state.update { it.copy(userData = data) }
                    if (requiredSignature.type == VerificationType.SIGN_DUMMY_TX) {
                        updateGroupServerKeysUseCase(
                            UpdateGroupServerKeysUseCase.Param(
                                body = data,
                                keyIdOrXfp = args.xfp,
                                signatures = emptyMap(),
                                securityQuestionToken = "",
                                token = args.token,
                                groupId = args.groupId,
                            )
                        ).onSuccess { transactionId ->
                            _state.update { it.copy(dummyTransactionId = transactionId, requiredSignature = requiredSignature) }
                            _event.emit(
                                CosigningGroupPolicyEvent.OnSaveChange(
                                    requiredSignature,
                                    data,
                                    transactionId
                                )
                            )
                        }.onFailure { exception ->
                            _event.emit(CosigningGroupPolicyEvent.ShowError(exception.message.orUnknownError()))
                        }
                    } else {
                        _event.emit(
                            CosigningGroupPolicyEvent.OnSaveChange(
                                requiredSignature,
                                data,
                                ""
                            )
                        )
                    }
                }
            } else {
                _event.emit(CosigningGroupPolicyEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onEditSpendingLimitClicked() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.OnEditSpendingLimitClicked)
        }
    }

    fun onEditSigningDelayClicked() {
        viewModelScope.launch {
            _event.emit(CosigningGroupPolicyEvent.OnEditSingingDelayClicked)
        }
    }
}

data class CosigningGroupPolicyState(
    val keyPolicy: GroupKeyPolicy = GroupKeyPolicy(),
    val members: List<AssistedMember> = emptyList(),
    val isUpdateFlow: Boolean = false,
    val userData: String = "",
    val signingDelayText: String = "",
    val dummyTransactionId: String = "",
    val requiredSignature: CalculateRequiredSignatures = CalculateRequiredSignatures(),
)

sealed class CosigningGroupPolicyEvent {
    class Loading(val isLoading: Boolean) : CosigningGroupPolicyEvent()
    class ShowError(val error: String) : CosigningGroupPolicyEvent()
    class OnSaveChange(
        val required: CalculateRequiredSignatures,
        val data: String,
        val dummyTransactionId: String
    ) : CosigningGroupPolicyEvent()

    object OnEditSpendingLimitClicked : CosigningGroupPolicyEvent()
    object OnEditSingingDelayClicked : CosigningGroupPolicyEvent()
    object OnDiscardChange : CosigningGroupPolicyEvent()
    object UpdateKeyPolicySuccess : CosigningGroupPolicyEvent()
}