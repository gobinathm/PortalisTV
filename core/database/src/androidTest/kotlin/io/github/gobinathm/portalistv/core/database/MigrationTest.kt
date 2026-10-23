package io.github.gobinathm.portalistv.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Creates the oldest exported schema and migrates it to the current version, so a missing or
 * wrong migration fails here instead of on users' devices. Runs on plain SQLite: migrations are
 * SQL and behave the same under SQLCipher.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PortalisDatabase::class.java,
    )

    @Test
    fun migratesFromFirstVersionToLatest() {
        helper.createDatabase(DB_NAME, FIRST_VERSION).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PortalisDatabase::class.java,
            DB_NAME,
        ).addMigrations(*Migrations.ALL).build()
        // Opening runs the migrations and validates the result against the current schema.
        database.openHelper.writableDatabase
        database.close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
        const val FIRST_VERSION = 1
    }
}
