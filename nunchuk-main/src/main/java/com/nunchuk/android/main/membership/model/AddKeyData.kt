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

package com.nunchuk.android.main.membership.model

import android.content.Context
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.TAPSIGNER_INHERITANCE_NAME
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType

data class AddKeyData(
    val type: MembershipStep,
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE
) {
    val isVerifyOrAddKey: Boolean
        get() = signer != null || verifyType != VerifyType.NONE
}

val MembershipStep.resId: Int
    get() {
        return when (this) {
            MembershipStep.ADD_SEVER_KEY -> R.drawable.ic_server_key_dark
            MembershipStep.HONEY_ADD_TAP_SIGNER,
            MembershipStep.BYZANTINE_ADD_TAP_SIGNER,
            MembershipStep.BYZANTINE_ADD_TAP_SIGNER_1 -> R.drawable.ic_nfc_card

            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4,
            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> R.drawable.ic_hardware_key

            else -> 0
        }
    }

fun MembershipStep.getLabel(context: Context): String {
    return when (this) {
        MembershipStep.IRON_ADD_HARDWARE_KEY_1 -> "Hardware key"
        MembershipStep.IRON_ADD_HARDWARE_KEY_2 -> "Hardware key #2"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0 -> "Hardware key #1"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        MembershipStep.HONEY_ADD_TAP_SIGNER, MembershipStep.BYZANTINE_ADD_TAP_SIGNER -> TAPSIGNER_INHERITANCE_NAME
        MembershipStep.BYZANTINE_ADD_TAP_SIGNER_1 -> "$TAPSIGNER_INHERITANCE_NAME #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1 -> "Hardware key #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2, MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> "Hardware key #3"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3 -> "Hardware key #4"
        MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> "Hardware key #5"
        else -> ""
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}