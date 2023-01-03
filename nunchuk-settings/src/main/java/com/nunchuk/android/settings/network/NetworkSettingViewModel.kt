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

package com.nunchuk.android.settings.network

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.InitAppSettingsUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NetworkSettingViewModel @Inject constructor(
    private val initAppSettingsUseCase: InitAppSettingsUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val accountManager: AccountManager,
    private val repository: UserProfileRepository,
    private val sessionHolder: SessionHolder,
    private val appScope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : NunchukViewModel<NetworkSettingState, NetworkSettingEvent>() {

    override val initialState = NetworkSettingState()

    val currentAppSettings: AppSettings?
        get() = state.value?.appSetting

    var initAppSettings: AppSettings? = null

    fun updateCurrentState(appSettings: AppSettings) {
        updateState {
            copy(appSetting = appSettings)
        }
    }

    fun fireResetTextHostServerEvent(appSettings: AppSettings) {
        event(
            NetworkSettingEvent.ResetTextHostServerEvent(appSettings)
        )
    }

    fun getAppSettings() {
        viewModelScope.launch {
            val result = getAppSettingUseCase(Unit)
            if (result.isSuccess) {
                initAppSettings = result.getOrThrow()
                updateState {
                    copy(appSetting = result.getOrThrow())
                }
                event(
                    NetworkSettingEvent.ResetTextHostServerEvent(result.getOrThrow())
                )
            }
        }
    }

    fun updateAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            val result = updateAppSettingUseCase(appSettings)
            if (result.isSuccess) {
                initAppSettings = result.getOrThrow()
                updateState {
                    copy(appSetting = result.getOrThrow())
                }
                event(NetworkSettingEvent.UpdateSettingSuccessEvent(result.getOrThrow()))
            }
        }
    }

    fun resetToDefaultAppSetting() {
        viewModelScope.launch {
            val result = initAppSettingsUseCase(Unit)
            result.getOrNull()?.let {
                initAppSettings = it
                updateState {
                    copy(appSetting = it)
                }
                event(NetworkSettingEvent.UpdateSettingSuccessEvent(it))
            }
        }
    }

    fun signOut() {
        appScope.launch {
            repository.signOut().flowOn(dispatcher).onException { }.firstOrNull()
            event(NetworkSettingEvent.LoadingEvent(true))
            sessionHolder.clearActiveSession()
            accountManager.signOut()
            event(NetworkSettingEvent.SignOutSuccessEvent)
        }
    }
}