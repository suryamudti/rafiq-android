package com.smiledev.rafiq.di

import android.content.Context
import com.smiledev.rafiq.core.DatabaseCopier
import com.smiledev.rafiq.data.local.BookmarkDatabase
import com.smiledev.rafiq.data.local.BookmarkDao
import com.smiledev.rafiq.data.local.PrayerLogDatabase
import com.smiledev.rafiq.data.local.PrayerLogDao
import com.smiledev.rafiq.data.preferences.PreferencesManager

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
    fun provideDatabaseCopier(@ApplicationContext context: Context): DatabaseCopier {
        return DatabaseCopier(context)
    }

    @Provides
    @Singleton
    fun provideBookmarkDatabase(@ApplicationContext context: Context): BookmarkDatabase {
        return BookmarkDatabase.getInstance(context)
    }

    @Provides
    fun provideBookmarkDao(database: BookmarkDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun providePrayerLogDatabase(@ApplicationContext context: Context): PrayerLogDatabase {
        return PrayerLogDatabase.getInstance(context)
    }

    @Provides
    fun providePrayerLogDao(database: PrayerLogDatabase): PrayerLogDao {
        return database.prayerLogDao()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

}
