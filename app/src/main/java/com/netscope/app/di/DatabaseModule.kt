package com.netscope.app.di

import android.content.Context
import androidx.room.Room
import com.netscope.app.data.local.dao.ConnectionEntryDao
import com.netscope.app.data.local.dao.DnsEntryDao
import com.netscope.app.data.local.dao.HttpTransactionDao
import com.netscope.app.data.local.database.NetScopeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NetScopeDatabase =
        Room.databaseBuilder(
            context,
            NetScopeDatabase::class.java,
            "netscope.db",
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHttpTransactionDao(db: NetScopeDatabase): HttpTransactionDao =
        db.httpTransactionDao()

    @Provides
    fun provideDnsEntryDao(db: NetScopeDatabase): DnsEntryDao =
        db.dnsEntryDao()

    @Provides
    fun provideConnectionEntryDao(db: NetScopeDatabase): ConnectionEntryDao =
        db.connectionEntryDao()
}