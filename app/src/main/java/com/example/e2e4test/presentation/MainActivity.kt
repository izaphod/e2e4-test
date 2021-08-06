package com.example.e2e4test.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.e2e4test.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}