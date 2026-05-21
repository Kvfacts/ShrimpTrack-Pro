package com.example

import android.app.Application
import com.example.data.AquacultureDatabase
import com.example.data.AquacultureRepository

class AquaShrimpApplication : Application() {
    val database by lazy { AquacultureDatabase.getDatabase(this) }
    val repository by lazy { AquacultureRepository(database.aquacultureDao()) }
}
