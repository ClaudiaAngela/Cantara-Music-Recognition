package com.example.musicrecognizer
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Dark mode forțat
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}