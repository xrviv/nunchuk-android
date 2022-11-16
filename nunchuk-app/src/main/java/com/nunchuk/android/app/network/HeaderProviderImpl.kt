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

package com.nunchuk.android.app.network

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.utils.DeviceManager
import javax.inject.Inject

class HeaderProviderImpl @Inject constructor(
    private val accountManager: AccountManager,
    private val deviceManager: DeviceManager
) : HeaderProvider {

    override fun getOsVersion(): String = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        VERSION.BASE_OS
    } else {
        VERSION.RELEASE
    }

    override fun getDeviceId() = deviceManager.getDeviceId()

    override fun getDeviceName(): String = android.os.Build.MODEL

    override fun getAppVersion() = BuildConfig.VERSION_NAME

    override fun getAccessToken() = accountManager.getAccount().token

}