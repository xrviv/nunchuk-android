package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetUnusedSignerFromMasterSignerUseCase {
    fun execute(
        masterSignerIds: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ): Flow<List<SingleSigner>>
}

internal class GetUnusedSignerFromMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetUnusedSignerFromMasterSignerUseCase {

    override fun execute(
        masterSignerIds: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ) = flow {
        emit(
            masterSignerIds.map {
                nativeSdk.getUnusedSignerFromMasterSigner(
                    it.id,
                    walletType,
                    addressType
                )
            }
        )
    }

}