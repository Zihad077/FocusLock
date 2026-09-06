package com.example

import android.app.Application
import com.example.data.AppRepository
import com.example.database.AppDatabase

class FocusLockApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.focusDao()) }
}
