package io.github.gobinathm.portalistv.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.model.TestProfiles
import io.github.gobinathm.portalistv.core.security.PasswordCipher
import io.github.gobinathm.portalistv.core.security.UnsupportedFormatException
import io.github.gobinathm.portalistv.core.security.WrongPasswordException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ProfileBackupTest {
    private val cipher = PasswordCipher(iterations = PasswordCipher.MIN_ITERATIONS)
    private val password = "backup password".toCharArray()

    private fun backup(repository: ProfileRepository) = ProfileBackup(repository, cipher) { 1_000L }

    private suspend fun exportFrom(vararg profiles: Profile): ByteArray {
        val source = InMemoryProfileRepository().apply { profiles.forEach { save(it) } }
        return ByteArrayOutputStream().also { backup(source).export(it, password) }.toByteArray()
    }

    @Test
    fun `exports and imports every field into an empty device`() = runTest {
        val file = exportFrom(TestProfiles.full(), TestProfiles.minimal())
        val target = InMemoryProfileRepository()

        val result = backup(target).import(ByteArrayInputStream(file), password)

        assertThat(result).isEqualTo(ImportResult(added = 2, replaced = 0, skipped = 0))
        // Ids are local to each device; everything else must survive.
        val imported = target.all().map { it.copy(id = 0) }
        assertThat(imported).containsExactly(TestProfiles.full(), TestProfiles.minimal())
    }

    @Test
    fun `the file does not contain secrets in readable form`() = runTest {
        val text = exportFrom(TestProfiles.full()).decodeToString()
        assertThat(text).doesNotContain("portal.invalid")
        assertThat(text).doesNotContain("00:1A:79")
    }

    @Test
    fun `replaces profiles with the same uuid and keeps their local id`() = runTest {
        val target = InMemoryProfileRepository()
        val localId = target.save(TestProfiles.minimal().copy(name = "Old name"))
        val file = exportFrom(TestProfiles.minimal().copy(name = "New name"))

        val result = backup(target).import(ByteArrayInputStream(file), password)

        assertThat(result).isEqualTo(ImportResult(added = 0, replaced = 1, skipped = 0))
        assertThat(target.all().single().id).isEqualTo(localId)
        assertThat(target.all().single().name).isEqualTo("New name")
    }

    @Test
    fun `skips invalid entries instead of failing the import`() = runTest {
        val broken = TestProfiles.full(uuid = "broken").let {
            it.copy(device = it.device.copy(mac = Secret("not a mac")))
        }
        val file = exportFrom(TestProfiles.minimal(), broken)

        val result = backup(InMemoryProfileRepository()).import(ByteArrayInputStream(file), password)

        assertThat(result).isEqualTo(ImportResult(added = 1, replaced = 0, skipped = 1))
    }

    @Test
    fun `a wrong password imports nothing`() = runTest {
        val file = exportFrom(TestProfiles.minimal())
        val target = InMemoryProfileRepository()
        val error = runCatching { backup(target).import(ByteArrayInputStream(file), "nope".toCharArray()) }
        assertThat(error.exceptionOrNull()).isInstanceOf(WrongPasswordException::class.java)
        assertThat(target.all()).isEmpty()
    }

    @Test
    fun `rejects a backup from a newer app version`() = runTest {
        val newer = """{"version":99,"exportedAtMillis":0,"profiles":[]}""".encodeToByteArray()
        val file = cipher.encrypt(newer, password)
        val error = runCatching { backup(InMemoryProfileRepository()).import(ByteArrayInputStream(file), password) }
        assertThat(error.exceptionOrNull()).isInstanceOf(UnsupportedFormatException::class.java)
    }

    @Test
    fun `rejects files that are too large without reading them whole`() {
        val huge = object : java.io.InputStream() {
            override fun read() = 0

            override fun read(b: ByteArray, off: Int, len: Int) = len
        }
        assertThrows(UnsupportedFormatException::class.java) {
            kotlinx.coroutines.runBlocking { backup(InMemoryProfileRepository()).import(huge, password) }
        }
    }
}

private class InMemoryProfileRepository : ProfileRepository {
    private val store = MutableStateFlow<Map<Long, Profile>>(emptyMap())
    private var nextId = 1L

    fun all() = store.value.values.toList()

    override val profiles: Flow<List<Profile>> = store.map { it.values.toList() }

    override suspend fun get(id: Long) = store.value[id]

    override suspend fun getByUuid(uuid: String) = store.value.values.firstOrNull { it.uuid == uuid }

    override suspend fun save(profile: Profile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        store.value += id to profile.copy(id = id)
        return id
    }

    override suspend fun delete(id: Long) {
        store.value -= id
    }
}
