package com.example.seizureguard.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun onEmergencyCall(context: Context, phone: String = "112") {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to start emergency call", Toast.LENGTH_SHORT).show()
    }
}

