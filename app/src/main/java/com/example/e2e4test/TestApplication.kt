package com.example.e2e4test

import android.app.Application
import com.example.e2e4test.di.AppComponent
import com.example.e2e4test.di.DaggerAppComponent

class TestApplication : Application() {

    var appComponent: AppComponent? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDagger()
    }

    override fun onTerminate() {
        appComponent = null
        super.onTerminate()
    }

    private fun initDagger() {
        if (appComponent == null) {
            this.appComponent = DaggerAppComponent.factory().create(this)
        }
        appComponent?.inject(this)
    }

    companion object {
        lateinit var instance: TestApplication
    }
}