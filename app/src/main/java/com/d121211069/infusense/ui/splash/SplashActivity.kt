package com.d121211069.infusense.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.d121211069.infusense.ui.main.MainActivity
import com.d121211069.infusense.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            val i = Intent(
                this@SplashActivity,
                MainActivity::class.java
            )

            startActivity(i)
            finish()
        }, SPLASH_SCREEN_DELAY)
    }

    companion object {
        const val SPLASH_SCREEN_DELAY: Long = 1000
    }
}