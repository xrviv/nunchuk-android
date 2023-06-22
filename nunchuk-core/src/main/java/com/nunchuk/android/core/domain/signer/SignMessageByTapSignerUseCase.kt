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

package com.nunchuk.android.core.domain.signer

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.WaitAutoCardUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SignMessageByTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<SignMessageByTapSignerUseCase.Data, SignedMessage?>(dispatcher, waitAutoCardUseCase) {

    override suspend fun executeNfc(parameters: Data): SignedMessage? {
        return nunchukNativeSdk.signMessageByTapSigner(
            isoDep = parameters.isoDep,
            cvc = parameters.cvc,
            masterSignerId = parameters.masterSignerId,
            path = parameters.path,
            message = parameters.message
        )
    }

    class Data(
        isoDep: IsoDep,
        val cvc: String,
        val path: String,
        val message: String,
        val masterSignerId: String
    ) : BaseNfcUseCase.Data(isoDep)
}