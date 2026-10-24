package io.github.gobinathm.portalistv.core.data

import io.github.gobinathm.portalistv.core.common.log.Redactor
import io.github.gobinathm.portalistv.core.data.di.IoDispatcher
import io.github.gobinathm.portalistv.core.database.PortalisDatabase
import io.github.gobinathm.portalistv.core.database.entity.toEntity
import io.github.gobinathm.portalistv.core.database.entity.toModel
import io.github.gobinathm.portalistv.core.model.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    /** All profiles in display order. Fails with `DatabaseKeyUnavailableException` if the data is unreadable. */
    val profiles: Flow<List<Profile>>

    suspend fun get(id: Long): Profile?

    suspend fun getByUuid(uuid: String): Profile?

    /** Inserts a new profile (id 0) or updates an existing one. @return the profile's id. */
    suspend fun save(profile: Profile): Long

    suspend fun delete(id: Long)
}

@Singleton
class LocalProfileRepository @Inject constructor(
    // Lazy: opening the database unwraps its key with the Keystore, which must not run on the main thread.
    private val database: dagger.Lazy<PortalisDatabase>,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ProfileRepository {
    private val dao get() = database.get().profileDao()

    override val profiles: Flow<List<Profile>> = flow { emitAll(dao.observeAll()) }
        .map { entities -> entities.map { it.toModel().registerSecrets() } }
        .flowOn(io)

    override suspend fun get(id: Long): Profile? = withContext(io) { dao.get(id)?.toModel()?.registerSecrets() }

    override suspend fun getByUuid(uuid: String): Profile? =
        withContext(io) { dao.getByUuid(uuid)?.toModel()?.registerSecrets() }

    override suspend fun save(profile: Profile): Long = withContext(io) {
        profile.registerSecrets()
        if (profile.id == 0L) {
            dao.insert(profile.toEntity())
        } else {
            check(dao.update(profile.toEntity()) == 1) { "Profile ${profile.id} no longer exists" }
            profile.id
        }
    }

    override suspend fun delete(id: Long) = withContext(io) {
        // The foreign key cascades too; this keeps it independent of SQLite's foreign key setting.
        database.get().favoriteChannelDao().deleteAll(id)
        dao.delete(id)
    }

    /** Masks values without a recognizable shape (MAC addresses and tokens are caught by pattern). */
    private fun Profile.registerSecrets(): Profile = also {
        listOfNotNull(
            portal.url,
            portal.login,
            portal.password,
            proxy.username,
            proxy.password,
            device.serial,
            deviceIds.deviceId,
            deviceIds.deviceIdSeed,
            deviceIds.deviceId2,
            deviceIds.signature,
        ).forEach { Redactor.register(it.value) }
    }
}
