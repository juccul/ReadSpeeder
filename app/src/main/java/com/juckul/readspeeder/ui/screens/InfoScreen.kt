package com.juckul.readspeeder.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juckul.readspeeder.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

private const val RepositoryUrl = "https://github.com/juccul/ReadSpeeder"
private const val KofiUrl = "https://ko-fi.com/juckul"

private enum class LicenseDocument(val titleRes: Int, val assetPath: String) {
    Gpl(R.string.gnu_gpl_3, "licenses/gpl_3_0.txt"),
    Apache(R.string.apache_license_2, "licenses/apache_2_0.txt"),
    PdfBox(R.string.pdfbox_notices, "licenses/pdfbox_notices.txt"),
    GoogleSans(R.string.google_sans_license, "licenses/google_sans.txt"),
}

private data class LicenseEntry(
    val name: String,
    val description: String,
    val license: LicenseDocument,
)

@Composable
internal fun InfoScreen(
    contentPadding: PaddingValues,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var displayedLicense by remember { mutableStateOf<LicenseDocument?>(null) }
    val licenseEntries = listOf(
        LicenseEntry(
            stringResource(R.string.readspeeder_license),
            stringResource(R.string.gnu_gpl_summary),
            LicenseDocument.Gpl,
        ),
        LicenseEntry(
            stringResource(R.string.apache_license_2),
            stringResource(R.string.apache_license_summary),
            LicenseDocument.Apache,
        ),
        LicenseEntry(
            stringResource(R.string.pdfbox_attribution),
            stringResource(R.string.pdfbox_notices_summary),
            LicenseDocument.PdfBox,
        ),
        LicenseEntry(
            stringResource(R.string.google_sans_attribution),
            stringResource(R.string.google_sans_copyright),
            LicenseDocument.GoogleSans,
        ),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().hazeSource(hazeState),
        contentPadding = contentPadding,
    ) {
        item { InfoSectionTitle(stringResource(R.string.info)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.free_and_oss)) },
                supportingContent = {
                    Text(stringResource(R.string.free_and_oss_description))
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.version)) },
                supportingContent = { Text(appVersionName(context)) },
            )
        }

        item { InfoSectionTitle(stringResource(R.string.links)) }
        item {
            ExternalLinkItem(
                title = stringResource(R.string.source_code),
                summary = stringResource(R.string.source_code_summary),
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                onClick = { context.openUrl(RepositoryUrl) },
            )
        }
        item {
            ExternalLinkItem(
                title = stringResource(R.string.support_development),
                summary = stringResource(R.string.support_development_summary),
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                onClick = { context.openUrl(KofiUrl) },
            )
        }

        item { InfoSectionTitle(stringResource(R.string.open_source_licenses)) }
        item {
            Text(
                text = stringResource(R.string.open_source_licenses_summary),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(licenseEntries) { licenseEntry ->
            ListItem(
                headlineContent = { Text(licenseEntry.name) },
                supportingContent = { Text(licenseEntry.description) },
                trailingContent = {
                    Text(
                        stringResource(R.string.view_license),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { displayedLicense = licenseEntry.license },
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }

    displayedLicense?.let { license ->
        LicenseDialog(
            license = license,
            onDismiss = { displayedLicense = null },
        )
    }
}

@Composable
private fun ExternalLinkItem(
    title: String,
    summary: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = leadingIcon,
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun InfoSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun LicenseDialog(license: LicenseDocument, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember(license) {
        context.assets.open(license.assetPath).bufferedReader().use { it.readText() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(license.titleRes)) },
        text = {
            LazyColumn {
                item {
                    SelectionContainer {
                        Text(licenseText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

internal fun appVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
