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

package com.nunchuk.android.main.components.tabs.wallet

import android.nfc.tech.IsoDep
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetNfcCardStatusUseCase
import com.nunchuk.android.core.domain.GetRemotePriceConvertBTCUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.domain.IsShowNfcUniversalUseCase
import com.nunchuk.android.core.domain.membership.GetServerWalletUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.AddWalletEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.CheckWalletPin
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GetTapSignerStatusSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GoToSatsCardScreen
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.Loading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NeedSetupSatsCard
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NfcLoading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.None
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.SatsCardUsedUp
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowErrorEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowSignerIntroEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.VerifyPassphraseSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.VerifyPasswordSuccess
import com.nunchuk.android.model.ByzantineGroupBrief
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.Chain
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetGroupBriefsFlowUseCase
import com.nunchuk.android.usecase.GetLocalCurrencyUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.banner.GetBannerUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberAcceptRequestUseCase
import com.nunchuk.android.usecase.byzantine.GroupMemberDenyRequestUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletsUseCase
import com.nunchuk.android.usecase.membership.GetAssistedWalletConfigUseCase
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import com.nunchuk.android.usecase.membership.GetPendingWalletNotifyCountUseCase
import com.nunchuk.android.usecase.membership.GetUserSubscriptionUseCase
import com.nunchuk.android.usecase.user.IsHideUpsellBannerUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    private val getNfcCardStatusUseCase: GetNfcCardStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
    private val masterSignerMapper: MasterSignerMapper,
    private val accountManager: AccountManager,
    private val getUserSubscriptionUseCase: GetUserSubscriptionUseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val getBannerUseCase: GetBannerUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val getAssistedWalletConfigUseCase: GetAssistedWalletConfigUseCase,
    private val getLocalCurrencyUseCase: GetLocalCurrencyUseCase,
    private val getRemotePriceConvertBTCUseCase: GetRemotePriceConvertBTCUseCase,
    private val pushEventManager: PushEventManager,
    isShowNfcUniversalUseCase: IsShowNfcUniversalUseCase,
    isHideUpsellBannerUseCase: IsHideUpsellBannerUseCase,
    private val syncGroupWalletsUseCase: SyncGroupWalletsUseCase,
    private val getGroupBriefsFlowUseCase: GetGroupBriefsFlowUseCase,
    private val groupMemberAcceptRequestUseCase: GroupMemberAcceptRequestUseCase,
    private val groupMemberDenyRequestUseCase: GroupMemberDenyRequestUseCase,
    private val getPendingWalletNotifyCountUseCase: GetPendingWalletNotifyCountUseCase,
) : NunchukViewModel<WalletsState, WalletsEvent>() {
    private val keyPolicyMap = hashMapOf<String, KeyPolicy>()

    val isShownNfcUniversal = isShowNfcUniversalUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val isHideUpsellBanner = isHideUpsellBannerUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var isRetrievingData = AtomicBoolean(false)

    override val initialState = WalletsState()
    private var badgeJob: Job? = null

    init {
        viewModelScope.launch {
            getAssistedWalletConfigUseCase(Unit)
        }
        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect {
                    updateState { copy(assistedWallets = it) }
                    checkMemberMembership()
                    checkInheritance(it)
                }
        }
        viewModelScope.launch {
            membershipStepManager.remainingTime.collect {
                updateState { copy(remainingTime = it) }
            }
        }
        viewModelScope.launch {
            isHideUpsellBanner.collect {
                updateState { copy(isHideUpsellBanner = it) }
            }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    updateState {
                        copy(
                            walletSecuritySetting = it.getOrNull() ?: WalletSecuritySetting()
                        )
                    }
                }
        }
        viewModelScope.launch {
            delay(1000)
            getWalletPinUseCase(Unit).collect {
                updateState { copy(currentWalletPin = it.getOrDefault("")) }
            }
        }
        viewModelScope.launch {
            getLocalCurrencyUseCase(Unit).collect {
                LOCAL_CURRENCY = it.getOrDefault(USD_CURRENCY)
                getRemotePriceConvertBTCUseCase(Unit)
            }
        }
        viewModelScope.launch {
            pushEventManager.event.collect { event ->
                if (event is PushEvent.WalletCreate) {
                    if (!getState().wallets.any { it.wallet.id == event.walletId }) {
                        getServerWalletUseCase(Unit).onSuccess {
                            if (it.isNeedReload) {
                                retrieveData()
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            syncGroupWalletsUseCase(Unit).onSuccess { shouldReload ->
                if (shouldReload) retrieveData()
            }
        }
        viewModelScope.launch {
            getGroupBriefsFlowUseCase(Unit).collect {
                updateState { copy(allGroups = it.getOrDefault(emptyList())) }
                mapGroupWalletUi()
            }
        }
        getAppSettings()
    }

    fun reloadMembership() {
        viewModelScope.launch {
            delay(1000L)
            checkMemberMembership()
        }
    }

    private suspend fun checkInheritance(wallets: List<AssistedWalletBrief>) {
        val honeyWalletsUnSetupInheritance =
            wallets.filter { it.plan == MembershipPlan.HONEY_BADGER }
        supervisorScope {
            honeyWalletsUnSetupInheritance.map {
                async {
                    getInheritanceUseCase(
                        GetInheritanceUseCase.Param(
                            it.localId,
                            groupId = it.groupId
                        )
                    )
                }
            }.awaitAll()
        }
    }

    private fun checkMemberMembership() {
        viewModelScope.launch {
            val result = getUserSubscriptionUseCase(Unit)
            if (result.isSuccess) {
                val subscription = result.getOrThrow()
                val getServerWalletResult = getServerWalletUseCase(Unit)
                if (getServerWalletResult.isFailure) return@launch
                if (getServerWalletResult.isSuccess && getServerWalletResult.getOrThrow().isNeedReload) {
                    retrieveData()
                }
                keyPolicyMap.clear()
                keyPolicyMap.putAll(getServerWalletResult.getOrNull()?.keyPolicyMap.orEmpty())
                updateState { copy(plan = subscription.plan) }
                mapGroupWalletUi()
            } else {
                updateState { copy(plan = MembershipPlan.NONE) }
            }
            if (result.getOrNull()?.subscriptionId.isNullOrEmpty() && isHideUpsellBanner.value.not()) {
                val bannerResult = getBannerUseCase(Unit)
                updateState {
                    copy(banner = bannerResult.getOrNull())
                }
            }
        }
    }

    private fun getAppSettings() {
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit).map { it.getOrElse { Chain.MAIN } }.collect {
                updateState {
                    copy(chain = it)
                }
            }
        }
    }

    fun retrieveData() {
        if (isRetrievingData.get()) return
        isRetrievingData.set(true)
        viewModelScope.launch {
            getCompoundSignersUseCase.execute().zip(getWalletsUseCase.execute()) { p, wallets ->
                Triple(p.first, p.second, wallets)
            }.map {
                mapSigners(it.second, it.first).sortedByDescending { signer ->
                    isPrimaryKey(
                        signer.id
                    )
                } to it.third
            }.flowOn(Dispatchers.IO).onException {
                updateState { copy(signers = emptyList()) }
            }.flowOn(Dispatchers.Main).onStart {
                if (getState().wallets.isEmpty()) {
                    event(Loading(true))
                }
            }.onCompletion {
                event(Loading(false))
                isRetrievingData.set(false)
            }.collect {
                val (signers, wallets) = it
                updateState {
                    copy(
                        signers = signers, wallets = wallets
                    )
                }
                mapGroupWalletUi()
            }
        }
    }

    private fun mapGroupWalletUi() {
        viewModelScope.launch {
            val results = arrayListOf<GroupWalletUi>()
            val wallets = getState().wallets
            val groups = getState().allGroups
            val groupWalletUis = getState().groupWalletUis
            val assistedWallets = getState().assistedWallets
            val assistedWalletIds = assistedWallets.map { it.localId }.toHashSet()
            val assistedWalletGroupIds =
                assistedWallets.filter { it.groupId.isNotEmpty() }.map { it.groupId }.toHashSet()
            val pendingGroup = groups.filter { it.isPendingWallet() }
            wallets.forEach { wallet ->
                val group = groups.firstOrNull { it.groupId in assistedWalletGroupIds }
                var groupWalletUi =
                    groupWalletUis.find { it.wallet?.wallet?.id == wallet.wallet.id }
                        ?: GroupWalletUi(
                            wallet = wallet,
                            isAssistedWallet = wallet.wallet.id in assistedWalletIds
                        )
                if (group != null) {
                    val role = getCurrentUserRole(group)
                    var inviterName = ""
                    if ((role == AssistedWalletRole.MASTER.name).not()) {
                        inviterName = getInviterName(group)
                    }
                    groupWalletUi = groupWalletUi.copy(
                        group = group,
                        role = role,
                        inviterName = inviterName
                    )
                }
                results.add(groupWalletUi)
            }
            pendingGroup.forEach { group ->
                var groupWalletUi =
                    groupWalletUis.find { it.group?.groupId == group.groupId }
                        ?: GroupWalletUi(group = group)
                val role = getCurrentUserRole(group)
                var inviterName = ""
                if ((role == AssistedWalletRole.MASTER.name).not()) {
                    inviterName = getInviterName(group)
                }
                groupWalletUi = groupWalletUi.copy(
                    group = group,
                    role = role,
                    inviterName = inviterName
                )
                results.add(groupWalletUi)
            }
            val sortedList = results.sortedWith { o1, o2 ->
                val walletNullOrder = if (o1.wallet == null) -1 else 1
                val groupTimeCreatedOrder =
                    o1.group?.createdTimeMillis?.compareTo(o2.group?.createdTimeMillis!!) ?: 0
                walletNullOrder + groupTimeCreatedOrder
            }
            updateState { copy(groupWalletUis = sortedList) }
            updateBadge()
        }
    }

    private fun updateBadge() {
        badgeJob?.cancel()
        badgeJob = viewModelScope.launch {
            val groupIds = getState().allGroups.map { it.groupId }
            val groupWalletUis = getState().groupWalletUis
            val result = getPendingWalletNotifyCountUseCase(groupIds)
            if (result.isSuccess) {
                val alerts = result.getOrDefault(hashMapOf())
                updateState {
                    copy(groupWalletUis = groupWalletUis.map {
                        it.copy(badgeCount = alerts[it.group?.groupId] ?: 0)
                    })
                }
            }
        }
    }

    private fun isPrimaryKey(id: String) =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id

    fun handleAddSigner() {
        event(ShowSignerIntroEvent)
    }

    fun handleAddWallet() {
        event(AddWalletEvent)
    }

    fun isInWallet(signer: SignerModel): Boolean {
        return getState().wallets.any {
            it.wallet.signers.any anyLast@{ singleSigner ->
                if (singleSigner.hasMasterSigner) {
                    return@anyLast singleSigner.masterFingerprint == signer.fingerPrint
                }
                return@anyLast singleSigner.masterFingerprint == signer.fingerPrint && singleSigner.derivationPath == signer.derivationPath
            }
        }
    }

    fun hasSigner() = getState().signers.isNotEmpty()

    fun hasWallet() = getState().wallets.isNotEmpty()

    fun getSatsCardStatus(isoDep: IsoDep?) {
        isoDep ?: return
        viewModelScope.launch {
            setEvent(NfcLoading(true))
            val result = getNfcCardStatusUseCase(BaseNfcUseCase.Data(isoDep))
            setEvent(NfcLoading(false))
            if (result.isSuccess) {
                val status = result.getOrThrow()
                if (status is TapSignerStatus) {
                    setEvent(GetTapSignerStatusSuccess(status))
                } else if (status is SatsCardStatus) {
                    if (status.isUsedUp) {
                        setEvent(SatsCardUsedUp(status.numberOfSlot))
                    } else if (status.isNeedSetup) {
                        setEvent(NeedSetupSatsCard(status))
                    } else {
                        setEvent(GoToSatsCardScreen(status))
                    }
                }
            } else {
                setEvent(ShowErrorEvent(result.exceptionOrNull()))
            }
        }
    }

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>, masterSigners: List<MasterSigner>
    ): List<SignerModel> {
        return masterSigners.map {
            masterSignerMapper(it)
        } + singleSigners.map(SingleSigner::toModel)
    }

    // Don't change, logic is very complicated :D
    fun getGroupStage(): MembershipStage {
        val plan = getState().plan
        val assistedWallets = getState().assistedWallets
        when (plan) {
            MembershipPlan.IRON_HAND -> {
                return when {
                    assistedWallets.isNotEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.DONE
                    membershipStepManager.isNotConfig() -> MembershipStage.NONE
                    else -> MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
                }
            }

            MembershipPlan.HONEY_BADGER -> {
                return when {
                    assistedWallets.all { it.isSetupInheritance } && assistedWallets.isNotEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.DONE
                    // Only show setup Inheritance banner with first assisted wallet
                    assistedWallets.isNotEmpty() && assistedWallets.first().isSetupInheritance.not() && membershipStepManager.isNotConfig() -> MembershipStage.SETUP_INHERITANCE
                    assistedWallets.isEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.NONE
                    assistedWallets.isNotEmpty() && membershipStepManager.isNotConfig() -> MembershipStage.DONE
                    else -> MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS
                }
            }

            MembershipPlan.BYZANTINE -> {
                val allGroups = getState().allGroups
                return if (allGroups.isEmpty()) MembershipStage.NONE else MembershipStage.DONE
            }

            else -> return MembershipStage.DONE // no subscription plan it means done
        }
    }

    fun getAssistedWalletId() = getState().assistedWallets.firstOrNull()?.localId

    fun getKeyPolicy(walletId: String) = keyPolicyMap[walletId]

    fun isPremiumUser() = getState().plan != null && getState().plan != MembershipPlan.NONE

    fun clearEvent() = event(None)

    fun isWalletPinEnable() =
        getState().walletSecuritySetting.protectWalletPin

    fun isWalletPasswordEnable() =
        accountManager.loginType() == SignInMode.EMAIL.value && getState().walletSecuritySetting.protectWalletPassword

    fun isWalletPassphraseEnable() =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && getState().walletSecuritySetting.protectWalletPassphrase

    fun checkWalletPin(input: String, walletId: String) = viewModelScope.launch {
        val match = checkWalletPinUseCase(input).getOrDefault(false)
        event(CheckWalletPin(match, walletId))
    }

    fun confirmPassword(password: String, walletId: String) = viewModelScope.launch {
        if (password.isBlank()) {
            return@launch
        }
        val result = verifiedPasswordTokenUseCase(
            VerifiedPasswordTokenUseCase.Param(
                password = password, targetAction = VerifiedPasswordTargetAction.PROTECT_WALLET.name
            )
        )
        if (result.isSuccess) {
            event(VerifyPasswordSuccess(walletId))
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    fun confirmPassphrase(passphrase: String, walletId: String) = viewModelScope.launch {
        if (passphrase.isBlank()) {
            return@launch
        }
        val result = verifiedPKeyTokenUseCase(passphrase)
        if (result.isSuccess) {
            event(VerifyPassphraseSuccess(walletId))
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    private fun isMatchingEmailOrUserName(emailOrUsername: String) =
        emailOrUsername == accountManager.getAccount().email
                || emailOrUsername == accountManager.getAccount().username

    private fun getCurrentUserRole(group: ByzantineGroupBrief): String {
       return group.members.firstOrNull {
            isMatchingEmailOrUserName(it.emailOrUsername)
        }?.role ?: AssistedWalletRole.NONE.name
    }

    private fun getInviterName(group: ByzantineGroupBrief): String {
        val invitee =
            group.members.firstOrNull {
                it.isPendingRequest() && isMatchingEmailOrUserName(it.emailOrUsername)
            } ?: return ""
        val inviter = group.members.firstOrNull { it.userId == invitee.inviterUserId }
        return inviter?.name.orEmpty()
    }

    fun acceptInviteMember(groupId: String) = viewModelScope.launch {
        val result = groupMemberAcceptRequestUseCase(groupId)
        if (result.isSuccess) {
            removePendingInviteMember(groupId)
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    fun denyInviteMember(groupId: String) = viewModelScope.launch {
        val result = groupMemberDenyRequestUseCase(groupId)
        if (result.isSuccess) {
            event(WalletsEvent.DenyWalletInvitationSuccess)
            removePendingInviteMember(groupId)
        } else {
            event(ShowErrorEvent(result.exceptionOrNull()))
        }
    }

    private fun removePendingInviteMember(groupId: String) {
        val results = getState().groupWalletUis.map {
            if (it.group != null && it.group.groupId == groupId) {
                it.copy(inviterName = "")
            } else {
                it
            }
        }
        updateState { copy(groupWalletUis = results) }
    }
}