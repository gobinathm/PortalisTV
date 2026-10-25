package io.github.gobinathm.portalistv.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.SectionHeader
import io.github.gobinathm.portalistv.core.designsystem.component.backdrop
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing

/** One entry: a title and its text. */
private data class AboutEntry(@param:StringRes val title: Int, @param:StringRes val text: Int)

private val privacy = listOf(
    AboutEntry(R.string.about_privacy_nothing, R.string.about_privacy_nothing_text),
    AboutEntry(R.string.about_privacy_portal, R.string.about_privacy_portal_text),
    AboutEntry(R.string.about_privacy_storage, R.string.about_privacy_storage_text),
)

private val licenses = listOf(
    AboutEntry(R.string.about_license_app, R.string.about_license_app_text),
    AboutEntry(R.string.about_source, R.string.about_source_text),
)

// Keep in step with THIRD_PARTY_NOTICES.md.
private val components = listOf(
    AboutEntry(R.string.about_libvlc, R.string.about_libvlc_license),
    AboutEntry(R.string.about_sqlcipher, R.string.about_sqlcipher_license),
    AboutEntry(R.string.about_libcxx, R.string.about_libcxx_license),
    AboutEntry(R.string.about_apache_libraries, R.string.about_apache_license),
    AboutEntry(R.string.about_icons, R.string.about_apache_license),
)

/**
 * The app's version, a privacy summary, its licenses and the third-party components it contains.
 * Every entry takes focus, so the remote scrolls through them.
 */
@Composable
fun AboutScreen(version: String, modifier: Modifier = Modifier) {
    val first = remember { FocusRequester() }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .backdrop(MaterialTheme.colorScheme),
        contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
    ) {
        item { Text(stringResource(R.string.about_title), style = MaterialTheme.typography.displayMedium) }
        item {
            AboutRow(
                stringResource(R.string.about_app_name),
                stringResource(R.string.about_version, version),
                Modifier.focusRequester(first),
            )
        }
        item { AboutRow(stringResource(R.string.about_provider), stringResource(R.string.about_provider_text)) }
        section(R.string.about_section_privacy, privacy)
        section(R.string.about_section_licenses, licenses)
        section(R.string.about_section_components, components)
        item {
            Text(
                stringResource(R.string.about_full_notices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.m),
            )
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

private fun LazyListScope.section(@StringRes title: Int, entries: List<AboutEntry>) {
    item { SectionHeader(stringResource(title)) }
    entries.forEach { entry -> item { AboutRow(stringResource(entry.title), stringResource(entry.text)) } }
}

/** Read-only, but focusable so the list scrolls with the remote. */
@Composable
private fun AboutRow(title: String, text: String, modifier: Modifier = Modifier) {
    ListItem(
        selected = false,
        onClick = {},
        modifier = modifier,
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun AboutPreview() {
    PortalisTheme { AboutScreen(version = "1.0.0") }
}
