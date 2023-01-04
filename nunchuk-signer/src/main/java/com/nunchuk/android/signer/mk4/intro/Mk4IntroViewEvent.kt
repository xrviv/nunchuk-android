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

package com.nunchuk.android.signer.mk4.intro

import com.nunchuk.android.model.SingleSigner

sealed class Mk4IntroViewEvent {
    data class Loading(val isLoading: Boolean) : Mk4IntroViewEvent()
    data class LoadMk4SignersSuccess(val signers: List<SingleSigner>) : Mk4IntroViewEvent()
    data class ShowError(val message: String) : Mk4IntroViewEvent()
    object OnContinueClicked : Mk4IntroViewEvent()
    object ErrorMk4TestNet : Mk4IntroViewEvent()
    object OnCreateSignerSuccess : Mk4IntroViewEvent()
    object OnSignerExistInAssistedWallet : Mk4IntroViewEvent()
}