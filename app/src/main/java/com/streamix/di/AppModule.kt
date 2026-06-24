package com.streamix.di

import android.content.Context
import androidx.room.Room
import com.streamix.core.storage.PreferencesManager
import com.streamix.core.storage.StreamixDatabase
import com.streamix.core.storage.WatchlistDao
import com.streamix.scraper.adult.OkxxxScraper
import com.streamix.scraper.adult.PornhatScraper
import com.streamix.scraper.moviebox.MovieboxInProvider
import com.streamix.scraper.moviebox.MovieboxProvider
import com.streamix.scraper.moviebox.MovieboxSiProvider
import com.streamix.scraper.moviebox.VegamoviesScraper
import com.streamix.scraper.youtube.YouTubeMusicScraper
import com.streamix.scraper.youtube.YouTubeScraper
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
    fun provideMovieboxProvider(): MovieboxProvider = MovieboxProvider()

    @Provides
    @Singleton
    fun provideMovieboxInProvider(): MovieboxInProvider = MovieboxInProvider()

    @Provides
    @Singleton
    fun provideMovieboxSiProvider(): MovieboxSiProvider = MovieboxSiProvider()

    @Provides
    @Singleton
    fun provideVegamoviesScraper(): VegamoviesScraper = VegamoviesScraper()

    @Provides
    @Singleton
    fun provideOkxxxScraper(): OkxxxScraper = OkxxxScraper()

    @Provides
    @Singleton
    fun providePornhatScraper(): PornhatScraper = PornhatScraper()

    @Provides
    @Singleton
    fun provideYouTubeScraper(): YouTubeScraper = YouTubeScraper()

    @Provides
    @Singleton
    fun provideYouTubeMusicScraper(): YouTubeMusicScraper = YouTubeMusicScraper()

    @Provides
    @Singleton
    fun provideUpdateApiService(retrofit: retrofit2.Retrofit): com.streamix.core.network.UpdateApiService {
        return retrofit.create(com.streamix.core.network.UpdateApiService::class.java)
    }
}
