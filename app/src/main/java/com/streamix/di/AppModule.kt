package com.streamix.di

import android.content.Context
import androidx.room.Room
import com.streamix.core.storage.PreferencesManager
import com.streamix.core.storage.StreamixDatabase
import com.streamix.core.storage.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StreamixDatabase {
        return Room.databaseBuilder(
            context,
            StreamixDatabase::class.java,
            "streamix_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideWatchlistDao(db: StreamixDatabase): WatchlistDao {
        return db.watchlistDao()
    }

    @Provides
    fun provideWatchHistoryDao(db: StreamixDatabase): com.streamix.core.storage.WatchHistoryDao {
        return db.watchHistoryDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideUpdateApiService(retrofit: retrofit2.Retrofit): com.streamix.core.network.UpdateApiService {
        return retrofit.create(com.streamix.core.network.UpdateApiService::class.java)
    }
}
