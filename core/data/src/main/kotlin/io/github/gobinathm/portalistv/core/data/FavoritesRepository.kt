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

    /** The favorites chosen for Home's Favorites row. */
    fun homeChannels(profileId: Long): Flow<List<LiveChannel>>

    /** Ids of the favorites on Home. */
    fun homeIds(profileId: Long): Flow<Set<String>>

    /** Adds [channel], or removes it if it is already a favorite. @return whether it is now a favorite. */
    suspend fun toggle(profileId: Long, channel: LiveChannel): Boolean

    /** Adds [channel] if it isn't a favorite yet; it starts off Home. */
    suspend fun add(profileId: Long, channel: LiveChannel)

    suspend fun remove(profileId: Long, channelId: String)

    /** Shows a favorite in Home's Favorites row, or hides it there. */
    suspend fun setOnHome(profileId: Long, channelId: String, onHome: Boolean)
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

    override fun homeChannels(profileId: Long): Flow<List<LiveChannel>> =
        flow { emitAll(dao.observe(profileId)) }
            .map { list -> list.filter { it.onHome }.map { it.toModel() } }
            .flowOn(io)

    override fun homeIds(profileId: Long): Flow<Set<String>> =
        flow { emitAll(dao.observe(profileId)) }
            .map { list -> list.filter { it.onHome }.mapTo(HashSet()) { it.channelId } }
            .flowOn(io)

    override suspend fun add(profileId: Long, channel: LiveChannel) =
        withContext(io) { dao.insert(channel.toFavoriteEntity(profileId, clock.nowMillis())) }

    override suspend fun remove(profileId: Long, channelId: String) =
        withContext(io) { dao.delete(profileId, channelId) }

    override suspend fun setOnHome(profileId: Long, channelId: String, onHome: Boolean) =
        withContext(io) { dao.setOnHome(profileId, channelId, onHome) }

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
