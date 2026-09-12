package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.database.AppDatabase
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DbTest {
    @Test
    fun testDbInit() {
        val db = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
        db.focusDao().getAllLimits() // trigger DB open
    }
}
