package com.netscope.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.netscope.app.data.local.entity.HttpTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HttpTransactionDao {

    @Query("SELECT * FROM http_transactions ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<HttpTransactionEntity>>

    @Query("SELECT * FROM http_transactions WHERE id = :id")
    suspend fun getById(id: String): HttpTransactionEntity?

    @Query("""
        SELECT * FROM http_transactions 
        WHERE (:query = '' OR url LIKE '%' || :query || '%' OR host LIKE '%' || :query || '%')
        ORDER BY timestampMs DESC
        LIMIT :limit
    """)
    fun observeFiltered(query: String, limit: Int = 500): Flow<List<HttpTransactionEntity>>

    @Query("SELECT * FROM http_transactions WHERE uid = :uid ORDER BY timestampMs DESC")
    fun observeByUid(uid: Int): Flow<List<HttpTransactionEntity>>

    @Query("SELECT * FROM http_transactions WHERE responseCode BETWEEN 400 AND 599 ORDER BY timestampMs DESC")
    fun observeErrors(): Flow<List<HttpTransactionEntity>>

    @Query("SELECT * FROM http_transactions WHERE durationMs > :thresholdMs ORDER BY timestampMs DESC")
    fun observeSlow(thresholdMs: Long = 2000): Flow<List<HttpTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HttpTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HttpTransactionEntity>)

    @Update
    suspend fun update(entity: HttpTransactionEntity)

    @Query("DELETE FROM http_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM http_transactions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM http_transactions")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM http_transactions WHERE timestampMs < :beforeMs")
    suspend fun deleteBefore(beforeMs: Long)
}