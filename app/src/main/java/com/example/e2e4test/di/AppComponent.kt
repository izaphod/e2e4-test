package com.example.e2e4test.di

import android.content.Context
import com.example.e2e4test.TestApplication
import com.example.e2e4test.di.module.PresenterModule
import com.example.e2e4test.presentation.mapscreen.MapScreenFragment
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import javax.inject.Singleton

@Singleton
@Component(modules = [PresenterModule::class])
interface AppComponent : AndroidInjector<TestApplication> {

    override fun inject(instance: TestApplication)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }

    fun injectFragment(fragment: MapScreenFragment)
}