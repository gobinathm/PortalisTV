package io.github.gobinathm.portalistv.core.database

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.database.entity.toEntity
import io.github.gobinathm.portalistv.core.database.entity.toModel
import io.github.gobinathm.portalistv.core.model.TestProfiles
import org.junit.Test

class ProfileMappingTest {
    @Test
    fun `every field survives a round trip`() {
        val profile = TestProfiles.full()
        assertThat(profile.toEntity().toModel()).isEqualTo(profile)
    }

    @Test
    fun `a minimal profile survives a round trip`() {
        val profile = TestProfiles.minimal()
        assertThat(profile.toEntity().toModel()).isEqualTo(profile)
    }
}
