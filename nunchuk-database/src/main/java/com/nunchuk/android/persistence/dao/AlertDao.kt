package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_ALERT
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao : BaseDao<AlertEntity> {

    companion object {
        const val GET_ALERTS_STATEMENT =
            "SELECT * FROM $TABLE_ALERT WHERE ((group_id = :groupId AND :groupId IS NOT NULL) OR (wallet_id = :walletId AND :walletId IS NOT NULL)) AND chain = :chain"
    }

    @Query(GET_ALERTS_STATEMENT)
    fun getAlertsFlow(
        groupId: String? = null,
        walletId: String? = null,
        chain: Chain,
    ): Flow<List<AlertEntity>>

    @Query(GET_ALERTS_STATEMENT)
    fun getAlerts(
        groupId: String? = null,
        walletId: String? = null,
        chain: Chain
    ): List<AlertEntity>

    @Transaction
    suspend fun updateData(
        updateOrInsertList: List<AlertEntity>,
        deleteList: List<AlertEntity>,
    ) {
        if (updateOrInsertList.isNotEmpty()) {
            updateOrInsert(updateOrInsertList)
        }

        if (deleteList.isNotEmpty()) {
            deletes(deleteList)
        }
    }
}