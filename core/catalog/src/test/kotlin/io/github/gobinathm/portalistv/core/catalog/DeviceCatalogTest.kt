package io.github.gobinathm.portalistv.core.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class DeviceCatalogTest {
    private val catalog = DeviceCatalog.load()

    @Test
    fun `bundled catalog loads and validates`() {
        assertThat(catalog.version).isEqualTo(1)
        assertThat(catalog.models).hasSize(24)
        assertThat(catalog.model(catalog.defaultModel)).isNotNull()
    }

    @Test
    fun `every model has a default firmware from its own list`() {
        for (model in catalog.models) {
            assertThat(model.firmware).contains(catalog.defaultFirmware(model).id)
        }
    }

    @Test
    fun `models with several firmware images default to the configured one`() {
        val mag250 = catalog.model("MAG250")!!
        assertThat(catalog.firmwareFor(mag250).map { it.id })
            .containsExactly("0.2.16-234", "0.2.18-r14-pub-250", "0.2.18-r19-pub-250").inOrder()
        assertThat(catalog.defaultFirmware(mag250).id).isEqualTo("0.2.18-r19-pub-250")
    }

    @Test
    fun `rejects a model that references unknown firmware`() {
        val text = minimalCatalog(modelFirmware = "\"missing\"")
        assertThrows(IllegalStateException::class.java) { DeviceCatalog.parse(text) }
    }

    @Test
    fun `rejects unknown fields so typos in the data are caught`() {
        val text = minimalCatalog().replace("\"version\": 1", "\"version\": 1, \"verison\": 2")
        assertThrows(IllegalArgumentException::class.java) { DeviceCatalog.parse(text) }
    }

    @Test
    fun `parses a minimal catalog`() {
        assertThat(DeviceCatalog.parse(minimalCatalog()).models.single().id).isEqualTo("M1")
    }

    private fun minimalCatalog(modelFirmware: String = "\"f1\"") = """
        {
          "version": 1,
          "defaultModel": "M1",
          "apiVersions": { "js": "1", "stb": "2", "playerEngine": "0x3" },
          "userAgents": [{ "id": "default", "name": "Default", "value": "UA" }],
          "displayResolutions": ["1920x1080"],
          "firmware": [{ "id": "f1", "imageVersion": "1", "imageDate": "d" }],
          "models": [{ "id": "M1", "firmware": [$modelFirmware] }]
        }
    """.trimIndent()
}
