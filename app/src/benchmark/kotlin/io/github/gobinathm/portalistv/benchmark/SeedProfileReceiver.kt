package io.github.gobinathm.portalistv.benchmark

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.data.ProfileDefaults
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.model.Secret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Benchmark builds only: replaces every profile with one pointing at the portal in the `url` extra,
 * usually the mock portal. A single profile opens at start, so the benchmark lands on its home.
 *
 * `adb shell am broadcast -n <package>/io.github.gobinathm.portalistv.benchmark.SeedProfileReceiver --es url <url>`
 */
class SeedProfileReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun profiles(): ProfileRepository

        fun defaults(): ProfileDefaults
    }

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("url") ?: return
        val dependencies = EntryPointAccessors.fromApplication(context.applicationContext, Dependencies::class.java)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profiles = dependencies.profiles()
                profiles.profiles.first().forEach { profiles.delete(it.id) }
                val profile = dependencies.defaults().newProfile()
                profiles.save(
                    profile.copy(
                        name = "Benchmark",
                        portal = profile.portal.copy(url = Secret(url), allowInsecureHttp = true),
                    ),
                )
                pending.resultCode = 1
            } finally {
                pending.finish()
            }
        }
    }
}
