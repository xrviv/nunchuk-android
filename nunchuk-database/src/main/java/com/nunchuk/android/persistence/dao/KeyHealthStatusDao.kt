package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_KEY_HEALTH_STATUS
import com.nunchuk.android.persistence.entity.KeyHealthStatusEntity
import com.nunchuk.android.type.Chain
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyHealthStatusDao : BaseDao<KeyHealthStatusEntity> {
    @Query("SELECT * FROM $TABLE_KEY_HEALTH_STATUS WHERE group_id = :groupId AND wallet_id = :walletId AND chat_id = :chatId AND chain = :chain")
    fun getKeys(
        groupId: String,
        walletId: String,
        chatId: String,
        chain: Chain
    ): Flow<List<KeyHealthStatusEntity>>

    @Transaction
    suspend fun updateData(
        updateOrInsertList: List<KeyHealthStatusEntity>,
        deleteList: List<KeyHealthStatusEntity>,
    ) {
        if (updateOrInsertList.isNotEmpty()) {
            updateOrInsert(updateOrInsertList)
        }
        if (deleteList.isNotEmpty()) {
            deletes(deleteList)
        }
    }
}