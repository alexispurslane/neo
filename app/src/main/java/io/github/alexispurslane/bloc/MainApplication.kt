package io.github.alexispurslane.bloc

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class MainApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
class MyApplicationModule {

    @Provides
    fun providesMainApplicationInstance(@ApplicationContext context: Context): MainApplication {
        return context as MainApplication
    }
}