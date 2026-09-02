package com.customwidgets.app.di

import android.content.Context
import androidx.room.Room
import com.customwidgets.app.data.local.WidgetDatabase
import com.customwidgets.app.data.local.dao.McpServerDao
import com.customwidgets.app.data.local.dao.WidgetDao
import com.customwidgets.app.data.repository.WidgetRepository
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
    fun provideWidgetDatabase(@ApplicationContext context: Context): WidgetDatabase {
        return Room.databaseBuilder(
            context,
            WidgetDatabase::class.java,
            WidgetDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWidgetDao(database: WidgetDatabase): WidgetDao {
        return database.widgetDao()
    }

    @Provides
    @Singleton
    fun provideMcpServerDao(database: WidgetDatabase): McpServerDao {
        return database.mcpServerDao()
    }

    @Provides
    @Singleton
    fun provideWidgetRepository(widgetDao: WidgetDao): WidgetRepository {
        return WidgetRepository(widgetDao)
    }
}
