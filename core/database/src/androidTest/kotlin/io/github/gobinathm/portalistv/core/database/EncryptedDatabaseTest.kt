package io.github.gobinathm.portalistv.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.database.entity.toEntity
import io.github.gobinathm.portalistv.core.database.entity.toFavoriteEntity
import io.github.gobinathm.portalistv.core.database.entity.toModel
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.TestProfiles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file: File = context.getDatabasePath("encrypted-test.db")
    private val key = "x'${"ab".repeat(32)}'"

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context.deleteDatabase(file.name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(file.name)
    }

    @Test
    fun storesAndReadsProfiles() = runTest {
        open(key).useDb { db ->
            val dao = db.profileDao()
            val id = dao.insert(TestProfiles.full().toEntity())
            dao.insert(TestProfiles.minimal().toEntity())

            assertThat(dao.get(id)?.toModel()).isEqualTo(TestProfiles.full().copy(id = id))
            // Ordered by sort order: minimal (0) before full (3).
            assertThat(dao.observeAll().first().map { it.name }).containsExactly("Minimal", "Full").inOrder()
        }
    }

    @Test
    fun updatesAndDeletesProfiles() = runTest {
        open(key).useDb { db ->
            val dao = db.profileDao()
            val id = dao.insert(TestProfiles.minimal().toEntity())

            val renamed = TestProfiles.minimal().copy(id = id, name = "Renamed")
            assertThat(dao.update(renamed.toEntity())).isEqualTo(1)
            assertThat(dao.getByUuid(renamed.uuid)?.name).isEqualTo("Renamed")

            dao.delete(id)
            assertThat(dao.get(id)).isNull()
            assertThat(dao.update(renamed.toEntity())).isEqualTo(0)
        }
    }

    @Test
    fun keepsFavoritesPerProfileInOrderAdded() = runTest {
        open(key).useDb { db ->
            val profileId = db.profileDao().insert(TestProfiles.minimal().toEntity())
            val otherId = db.profileDao().insert(TestProfiles.full().toEntity())
            val favorites = db.favoriteChannelDao()
            favorites.insert(channel("2").toFavoriteEntity(profileId, addedAtMillis = 10))
            favorites.insert(channel("1").toFavoriteEntity(profileId, addedAtMillis = 20))
            favorites.insert(channel("9").toFavoriteEntity(otherId, addedAtMillis = 5))
            // Adding again keeps the original position.
            favorites.insert(channel("2").toFavoriteEntity(profileId, addedAtMillis = 30))

            assertThat(favorites.getAll(profileId).map { it.channelId }).containsExactly("2", "1").inOrder()
            assertThat(favorites.getAll(profileId).first().toModel()).isEqualTo(channel("2"))

            favorites.delete(profileId, "2")
            favorites.deleteAll(otherId)
            assertThat(favorites.observe(profileId).first().map { it.channelId }).containsExactly("1")
            assertThat(favorites.getAll(otherId)).isEmpty()
        }
    }

    @Test
    fun rejectsDuplicateUuid() = runTest {
        open(key).useDb { db ->
            val dao = db.profileDao()
            dao.insert(TestProfiles.minimal().toEntity())
            val duplicate = runCatching { dao.insert(TestProfiles.minimal().toEntity()) }
            assertThat(duplicate.exceptionOrNull()).isInstanceOf(SQLiteConstraintException::class.java)
        }
    }

    private fun channel(id: String) =
        LiveChannel(id, "Channel $id", id.toInt(), "https://logos.invalid/$id.png", "3", false, "ffrt http://x/$id")

    @Test
    fun fileIsEncryptedOnDisk() = runTest {
        open(key).useDb { db -> db.profileDao().insert(TestProfiles.full().toEntity()) }

        val bytes = file.readBytes()
        assertThat(bytes.copyOfRange(0, 15).decodeToString()).isNotEqualTo("SQLite format 3")
        val text = bytes.decodeToString()
        assertThat(text).doesNotContain("portal.invalid")
        assertThat(text).doesNotContain("00:1A:79:00:00:02")
    }

    @Test
    fun wrongKeyCannotOpen() = runTest {
        open(key).useDb { db -> db.profileDao().insert(TestProfiles.minimal().toEntity()) }

        val wrongKey = "x'${"cd".repeat(32)}'"
        open(wrongKey).useDb { db ->
            assertThrows(SQLiteException::class.java) { db.openHelper.readableDatabase }
        }
    }

    private fun open(key: String) =
        Room.databaseBuilder(context, PortalisDatabase::class.java, file.absolutePath)
            .openHelperFactory(SupportOpenHelperFactory(key.encodeToByteArray()))
            .addMigrations(*Migrations.ALL)
            .build()

    private inline fun <T> PortalisDatabase.useDb(block: (PortalisDatabase) -> T): T =
        try {
            block(this)
        } finally {
            close()
        }
}
