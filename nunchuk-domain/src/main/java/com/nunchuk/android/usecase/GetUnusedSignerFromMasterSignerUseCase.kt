package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetUnusedSignerFromMasterSignerUseCase {
    fun execute(
        masterSigners: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ): Flow<List<SingleSigner>>
}

internal class GetUnusedSignerFromMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetUnusedSignerFromMasterSignerUseCase {

    override fun execute(
        masterSigners: List<MasterSigner>,
        walletType: WalletType,
        addressType: AddressType
    ) = flow {
        emit(
            masterSigners.map { masterSigner ->
                if (masterSigner.type == SignerType.NFC) {
                    nativeSdk.getDefaultSignerFromMasterSigner(
                        masterSignerId = masterSigner.id,
                        walletType = walletType.ordinal,
                        addressType = addressType.ordinal
                    )
                } else {
                    nativeSdk.getUnusedSignerFromMasterSigner(
                        masterSignerId = masterSigner.id,
                        walletType = walletType,
                        addressType = addressType
                    ).also {
                        if (masterSigner.device.needPassPhraseSent) {
                            nativeSdk.clearSignerPassphrase(masterSigner.id)
                        }
                    }
                }
            }
        )
    }

}