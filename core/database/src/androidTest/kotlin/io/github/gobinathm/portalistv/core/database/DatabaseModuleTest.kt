package io.github.gobinathm.portalistv.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.database.di.DatabaseModule
import io.github.gobinathm.portalistv.core.database.entity.toEntity
import io.github.gobinathm.portalistv.core.model.TestProfiles
import io.github.gobinathm.portalistv.core.security.KeyManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The production setup: a Keystore-wrapped key file opening the SQLCipher database. */
@RunWith(AndroidJUnit4::class)
class DatabaseModuleTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val keyFile = File(context.noBackupFilesDir, "database.key")

    @Before
    @After
    fun clean() {
        context.deleteDatabase(PortalisDatabase.FILE_NAME)
        keyFile.delete()
    }

    @Test
    fun createsKeyAndReopensDatabase() = runTest {
        val first = DatabaseModule.provideDatabase(context, KeyManager(context))
        val id = first.profileDao().insert(TestProfiles.full().toEntity())
        first.close()
        assertThat(keyFile.exists()).isTrue()

        val second = DatabaseModule.provideDatabase(context, KeyManager(context))
        assertThat(second.profileDao().get(id)?.name).isEqualTo("Full")
        second.close()
    }
}
