package io.github.gobinathm.portalistv.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.SourceException
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class HomeViewModelTest {
    @Test
    fun `stream summary hides host and query`() {
        assertThat(HomeViewModel.withoutHost("http://streams.example:8080/live/101.ts?play_token=abc"))
            .isEqualTo("http://…/live/101.ts")
    }

    @Test
    fun `maps failures to user-facing errors`() {
        assertThat(SourceException(SourceException.Reason.DEVICE_REFUSED).toConnectionError())
            .isEqualTo(ConnectionError.DEVICE_REFUSED)
        assertThat(UnknownHostException().toConnectionError()).isEqualTo(ConnectionError.NO_NETWORK)
        assertThat(SocketTimeoutException().toConnectionError()).isEqualTo(ConnectionError.TIMEOUT)
    }
}
