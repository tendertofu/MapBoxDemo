package com.enterpreta.mapboxdemo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button

class SecondActivity : AppCompatActivity() {
    private lateinit var btnButton: Button
    //@SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.activity_second, null)


        btnButton = findViewById(R.id.btnBack)
        btnButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}