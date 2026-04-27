package com.netscope.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.netscope.app.data.local.entity.ConnectionEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionEntryDao {

    @Query("SELECT * FROM connections ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<ConnectionEntryEntity>>

    @Query("SELECT * FROM connections WHERE isActive = 1 ORDER BY timestampMs DESC")
    fun observeActive(): Flow<List<ConnectionEntryEntity>>

    @Query("SELECT * FROM connections WHERE isFlagged = 1 ORDER BY timestampMs DESC")
    fun observeFlagged(): Flow<List<ConnectionEntryEntity>>

    @Query("SELECT * FROM connections WHERE uid = :uid ORDER BY timestampMs DESC")
    fun observeByUid(uid: Int): Flow<List<ConnectionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConnectionEntryEntity)

    @Update
    suspend fun update(entity: ConnectionEntryEntity)

    @Query("UPDATE connections SET isActive = 0 WHERE id = :id")
    suspend fun markInactive(id: String)

    @Query("UPDATE connections SET isFlagged = 1, flagReason = :reason WHERE id = :id")
    suspend fun flag(id: String, reason: String)

    @Query("DELETE FROM connections")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM connections WHERE isActive = 1")
    fun observeActiveCount(): Flow<Int>
}