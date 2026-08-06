package com.streamix.di

import android.content.Context
import androidx.room.Room
import com.streamix.data.local.AppDatabase
import com.streamix.data.local.MovieDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "streamix_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideLibraryDatabase(@ApplicationContext context: Context): com.streamix.data.LibraryDatabase {
        return Room.databaseBuilder(
            context,
            com.streamix.data.LibraryDatabase::class.java,
            "library_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLibraryDao(database: com.streamix.data.LibraryDatabase): com.streamix.data.LibraryDao {
        return database.libraryDao()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
