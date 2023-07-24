package com.example.appdatacleanproject

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val packageManager: PackageManager = getApplication().getPackageManager()
        val button1 = findViewById<Button>(R.id.button1)
        button1.setOnClickListener {
            AppDataCleanManager.commandLineClearData(applicationContext)
        }

        val button2 = findViewById<Button>(R.id.button2)
        button2.setOnClickListener {
            AppDataCleanManager.clearInternalExternalStorage(application, this)
        }
    }
}