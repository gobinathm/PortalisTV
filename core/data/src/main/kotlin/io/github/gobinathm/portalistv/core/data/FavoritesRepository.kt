package io.github.gobinathm.portalistv.core.data

import io.github.gobinathm.portalistv.core.data.di.IoDispatcher
import io.github.gobinathm.portalistv.core.database.PortalisDatabase
import io.github.gobinathm.portalistv.core.database.entity.toFavoriteEntity
import io.github.gobinathm.portalistv.core.database.entity.toModel
import io.github.gobinathm.portalistv.core.model.LiveChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Each profile's favorite live channels, in the order they were added. */
interface FavoritesRepository {
    fun channels(profileId: Long): Flow<List<LiveChannel>>

    suspend fun getChannels(profileId: Long): List<LiveChannel>

    /** Adds [channel], or removes it if it is already a favorite. @return whether it is now a favorite. */
    suspend fun toggle(profileId: Long, channel: LiveChannel): Boolean
}

@Singleton
class LocalFavoritesRepository @Inject constructor(
    // Lazy for the same reason as in LocalProfileRepository: opening the database needs the Keystore.
    private val database: dagger.Lazy<PortalisDatabase>,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    private val clock: Clock,
) : FavoritesRepository {
    private val dao get() = database.get().favoriteChannelDao()

    override fun channels(profileId: Long): Flow<List<LiveChannel>> =
        flow { emitAll(dao.observe(profileId)) }.map { list -> list.map { it.toModel() } }.flowOn(io)

    override suspend fun getChannels(profileId: Long): List<LiveChannel> =
        withContext(io) { dao.getAll(profileId).map { it.toModel() } }

    override suspend fun toggle(profileId: Long, channel: LiveChannel): Boolean = withContext(io) {
        val isFavorite = dao.getAll(profileId).any { it.channelId == channel.id }
        if (isFavorite) {
            dao.delete(profileId, channel.id)
        } else {
            dao.insert(channel.toFavoriteEntity(profileId, clock.nowMillis()))
        }
        !isFavorite
    }
}

/** Wall-clock time, replaceable in tests. */
fun interface Clock {
    fun nowMillis(): Long
}
