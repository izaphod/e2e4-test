package com.example.e2e4test.di.module

import com.example.e2e4test.data.network.ApiService
import com.example.e2e4test.presentation.mapscreen.MapScreenPresenter
import dagger.Module
import dagger.Provides

@Module(includes = [ApiServiceModule::class])
class PresenterModule {

    @Provides
    fun provideMapScreenPresenter(apiService: ApiService): MapScreenPresenter =
        MapScreenPresenter(apiService)
}