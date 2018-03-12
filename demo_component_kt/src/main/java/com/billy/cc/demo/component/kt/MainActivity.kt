package com.billy.cc.demo.component.kt

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView : TextView = TextView(this)
        textView.gravity = Gravity.CENTER
        textView.setText("kotlin page\nclick to finish!")
        textView.setOnClickListener({
            finish()
        })
        setContentView(textView)
    }
}
