package com.jspythonista.ide

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "JS Pythonista IDE\n\nJavaScript/TypeScript/React development environment for Android"
        textView.textSize = 18f
        textView.setPadding(40, 40, 40, 40)
        
        setContentView(textView)
    }
}
