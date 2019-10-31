package com.tompee.kotlinbuilder

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tompee.kotlinbuilder.models.AddressBuilder
import com.tompee.kotlinbuilder.models.PersonFactory

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        person()
        address()
    }

    private fun person() {
        val person = PersonFactory("name", 12) {
            setFullName { "full_name" }
        }
        val builder = PersonFactory("builderName", 0)
        builder.age { 16 }
        builder.setFullName { "xyz" }
        Log.i("MainActivity", "Person: $person")
        Log.i("MainActivity", "Person from builder: ${builder.build()}")
    }

    private fun address() {
        val address = AddressBuilder() {
            province { "province" }
        }
        Log.i("MainActivity", "Address: $address")
    }
}
