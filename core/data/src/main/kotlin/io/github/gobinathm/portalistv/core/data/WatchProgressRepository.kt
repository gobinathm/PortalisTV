package io.github.gobinathm.portalistv.core.data

import io.github.gobinathm.portalistv.core.data.di.IoDispatcher
import io.github.gobinathm.portalistv.core.database.PortalisDatabase
import io.github.gobinathm.portalistv.core.database.entity.toEntity
import io.github.gobinathm.portalistv.core.database.entity.toModel
import io.github.gobinathm.portalistv.core.model.VodPlayback
import io.github.gobinathm.portalistv.core.model.WatchProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Where each profile stopped in movies and episodes. */
interface WatchProgressRepository {
    /** One entry per movie or series (its latest episode) that isn't finished, most recent first. */
    fun continueWatching(profileId: Long): Flow<List<WatchProgress>>

    /** A movie's progress, or that of every episode of a series watched so far, most recent first. */
    fun forItem(profileId: Long, itemId: String): Flow<List<WatchProgress>>

    suspend fun get(profileId: Long, playback: VodPlayback): WatchProgress?

    suspend fun save(profileId: Long, progress: WatchProgress)

    /** Takes a movie or series off Continue Watching. */
    suspend fun forget(profileId: Long, itemId: String)
}

@Singleton
class LocalWatchProgressRepository @Inject constructor(
    // Lazy for the same reason as in LocalProfileRepository: opening the database needs the Keystore.
    private val database: dagger.Lazy<PortalisDatabase>,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : WatchProgressRepository {
    private val dao get() = database.get().watchProgressDao()

    override fun continueWatching(profileId: Long): Flow<List<WatchProgress>> =
        flow { emitAll(dao.observe(profileId)) }
            .map { rows -> continueWatching(rows.map { it.toModel() }) }
            .flowOn(io)

    override fun forItem(profileId: Long, itemId: String): Flow<List<WatchProgress>> =
        flow { emitAll(dao.observeItem(profileId, itemId)) }.map { rows -> rows.map { it.toModel() } }.flowOn(io)

    override suspend fun get(profileId: Long, playback: VodPlayback): WatchProgress? =
        withContext(io) { dao.get(profileId, playback.itemId, playback.episodeId.orEmpty())?.toModel() }

    override suspend fun save(profileId: Long, progress: WatchProgress) =
        withContext(io) { dao.upsert(progress.toEntity(profileId)) }

    override suspend fun forget(profileId: Long, itemId: String) = withContext(io) { dao.deleteItem(profileId, itemId) }

    companion object {
        const val CONTINUE_WATCHING_LIMIT = 20

        /** The latest entry of each movie or series, dropped once finished; [rows] are most recent first. */
        internal fun continueWatching(rows: List<WatchProgress>): List<WatchProgress> =
            rows.distinctBy { it.playback.itemId }.filterNot { it.finished }.take(CONTINUE_WATCHING_LIMIT)
    }
}
