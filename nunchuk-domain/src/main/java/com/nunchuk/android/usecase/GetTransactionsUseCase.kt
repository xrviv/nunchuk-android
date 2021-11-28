package com.nunchuk.android.usecase

import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetTransactionsUseCase {
    fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>): Flow<List<TransactionExtended>>
}

internal class GetTransactionsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetTransactionsUseCase {

    override fun execute(walletId: String, eventIds: List<Pair<String, Boolean>>) = flow {
        val transactions = eventIds.mapNotNull {
            val initEventId = it.first
            val isReceive = it.second
            getTransaction(walletId = walletId, initEventId = initEventId, isReceive = isReceive)
        }
        emit(transactions)
    }.catch {
        CrashlyticsReporter.recordException(it)
        emit(emptyList())
    }

    private fun getTransaction(walletId: String, initEventId: String, isReceive: Boolean): TransactionExtended? {
        try {
            val txId = if (isReceive) {
                nativeSdk.getTransactionId(initEventId)
            } else {
                nativeSdk.getRoomTransaction(initEventId).txId
            }
            if (txId.isEmpty()) return null

            val tx = nativeSdk.getTransaction(walletId, txId = txId)
            return TransactionExtended(walletId = walletId, initEventId, tx)
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
            return null
        }
    }
}