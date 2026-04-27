package com.netscope.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netscope.app.data.local.entity.DnsEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsEntryDao {

    @Query("SELECT * FROM dns_entries ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<DnsEntryEntity>>

    @Query("SELECT * FROM dns_entries WHERE id = :id")
    suspend fun getById(id: String): DnsEntryEntity?

    @Query("SELECT * FROM dns_entries WHERE uid = :uid ORDER BY timestampMs DESC")
    fun observeByUid(uid: Int): Flow<List<DnsEntryEntity>>

    @Query("SELECT * FROM dns_entries WHERE domain LIKE '%' || :query || '%' ORDER BY timestampMs DESC")
    fun observeByDomain(query: String): Flow<List<DnsEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DnsEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DnsEntryEntity>)

    @Query("DELETE FROM dns_entries")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM dns_entries")
    fun observeCount(): Flow<Int>

    @Query("""
        SELECT DISTINCT domain FROM dns_entries 
        WHERE uid = :uid 
        ORDER BY timestampMs DESC
    """)
    suspend fun getDomainsForUid(uid: Int): List<String>
}