package com.netscope.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.netscope.app.data.local.converter.Converters
import com.netscope.app.data.local.dao.ConnectionEntryDao
import com.netscope.app.data.local.dao.DnsEntryDao
import com.netscope.app.data.local.dao.HttpTransactionDao
import com.netscope.app.data.local.entity.ConnectionEntryEntity
import com.netscope.app.data.local.entity.DnsEntryEntity
import com.netscope.app.data.local.entity.HttpTransactionEntity

@Database(
    entities = [
        HttpTransactionEntity::class,
        DnsEntryEntity::class,
        ConnectionEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NetScopeDatabase : RoomDatabase() {
    abstract fun httpTransactionDao(): HttpTransactionDao
    abstract fun dnsEntryDao(): DnsEntryDao
    abstract fun connectionEntryDao(): ConnectionEntryDao
}