package io.github.gobinathm.portalistv.feature.profiles.editor

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.data.ProfileDefaults
import io.github.gobinathm.portalistv.core.data.ProfileError
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.model.TestProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditorViewModelTest {
    private val catalog = DeviceCatalog.load()
    private val repository = FakeProfileRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: Long = 0) = ProfileEditorViewModel(
        SavedStateHandle(mapOf(ProfileEditorViewModel.PROFILE_ID_ARG to id)),
        repository,
        ProfileDefaults(catalog),
        catalog,
    )

    private val ProfileEditorViewModel.draft get() = state.value.draft!!

    @Test
    fun `a new profile starts from the catalog defaults without showing errors`() {
        val vm = viewModel()
        assertThat(vm.state.value.isNew).isTrue()
        assertThat(vm.draft.device.stbModel).isEqualTo("MAG250")
        assertThat(vm.state.value.errors).contains(ProfileError.NAME_REQUIRED)
        assertThat(vm.state.value.showErrors).isFalse()
    }

    @Test
    fun `saving an invalid profile shows errors and stores nothing`() {
        val vm = viewModel()
        vm.save()
        assertThat(vm.state.value.showErrors).isTrue()
        assertThat(vm.state.value.finished).isFalse()
        assertThat(repository.saved).isEmpty()
    }

    @Test
    fun `saving a valid profile stores the normalized draft and finishes`() {
        val vm = viewModel()
        vm.update {
            it.copy(
                name = " Living room ",
                portal = it.portal.copy(url = Secret("https://portal.invalid/c/")),
                device = it.device.copy(mac = Secret("00:1a:79:aa:bb:cc")),
            )
        }
        vm.save()
        assertThat(vm.state.value.finished).isTrue()
        val saved = repository.saved.single()
        assertThat(saved.name).isEqualTo("Living room")
        assertThat(saved.device.mac.value).isEqualTo("00:1A:79:AA:BB:CC")
    }

    @Test
    fun `a failed save keeps the editor open`() {
        repository.failSaves = true
        val vm = viewModel(id = repository.add(TestProfiles.minimal()))
        vm.save()
        assertThat(vm.state.value.saveFailed).isTrue()
        assertThat(vm.state.value.finished).isFalse()
    }

    @Test
    fun `changing the model resets a preset to the new model's default`() {
        val vm = viewModel()
        vm.selectModel("MAG424")
        assertThat(vm.draft.device.stbModel).isEqualTo("MAG424")
        assertThat(vm.draft.firmware.presetId).isEqualTo("2.20.02-pub-424")
    }

    @Test
    fun `custom firmware is prefilled with the values in effect and survives a model change`() {
        val vm = viewModel()
        vm.selectFirmware(null)
        assertThat(vm.draft.firmware.presetId).isNull()
        assertThat(vm.draft.firmware.imageDescription).isEqualTo("0.2.18-r19-pub-250")
        assertThat(vm.draft.firmware.jsApiVersion).isEqualTo("343")
        vm.selectModel("MAG424")
        assertThat(vm.draft.firmware.imageDescription).isEqualTo("0.2.18-r19-pub-250")
    }

    @Test
    fun `resetting device ids clears them and creates a new seed`() {
        val vm = viewModel(id = repository.add(TestProfiles.full()))
        val oldSeed = vm.draft.deviceIds.deviceIdSeed
        vm.resetDeviceIds()
        with(vm.draft.deviceIds) {
            assertThat(deviceId).isNull()
            assertThat(deviceId2).isNull()
            assertThat(signature).isNull()
            assertThat(deviceIdSeed).isNotEqualTo(oldSeed)
            assertThat(sendDeviceId).isTrue()
        }
    }

    @Test
    fun `deleting an existing profile removes it and finishes`() {
        val id = repository.add(TestProfiles.minimal())
        val vm = viewModel(id)
        vm.delete()
        assertThat(repository.stored(id)).isNull()
        assertThat(vm.state.value.finished).isTrue()
    }

    @Test
    fun `a profile that no longer exists closes the editor`() {
        assertThat(viewModel(id = 99).state.value.finished).isTrue()
    }
}

private class FakeProfileRepository : ProfileRepository {
    private val store = MutableStateFlow<Map<Long, Profile>>(emptyMap())
    val saved = mutableListOf<Profile>()
    var failSaves = false

    fun add(profile: Profile): Long {
        val id = store.value.size + 1L
        store.value += id to profile.copy(id = id)
        return id
    }

    fun stored(id: Long) = store.value[id]

    override val profiles: Flow<List<Profile>> get() = error("unused")

    override suspend fun get(id: Long): Profile? = store.value[id]

    override suspend fun save(profile: Profile): Long {
        check(!failSaves) { "disk full" }
        saved += profile
        return if (profile.id == 0L) add(profile) else profile.id.also { store.value += it to profile }
    }

    override suspend fun delete(id: Long) {
        store.value -= id
    }
}
