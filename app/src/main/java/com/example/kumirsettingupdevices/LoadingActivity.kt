package com.example.kumirsettingupdevices

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.kumirsettingupdevices.databinding.LoadingActivityBinding

// Загрузочное окно
class LoadingActivity : AppCompatActivity() {
    private lateinit var binding: LoadingActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoadingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Задержка перед переходом к основной активности
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
    }
}