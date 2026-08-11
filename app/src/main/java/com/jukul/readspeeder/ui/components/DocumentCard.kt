package com.jukul.readspeeder.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class CardPointerState {
    var coordinates: LayoutCoordinates? = null
    var pressPosition = Offset.Zero
}

@Composable
internal fun DocumentCard(
    documentId: String,
    title: String,
    author: String?,
    hasCover: Boolean,
    loadCover: (String) -> ByteArray?,
    progress: Int,
    onClick: () -> Unit,
    onLongClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionsLabel = stringResource(R.string.document_options)
    val pointerState = remember { CardPointerState() }
    val coverImage by produceState<ImageBitmap?>(null, documentId, hasCover) {
        value = if (hasCover) {
            withContext(Dispatchers.IO) {
                loadCover(documentId)?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
                }
            }
        } else {
            null
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .onGloballyPositioned { pointerState.coordinates = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        pointerState.pressPosition =
                            pointerState.coordinates?.localToRoot(down.position) ?: down.position
                    }
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClickLabel = optionsLabel,
                    onLongClick = { onLongClick(pointerState.pressPosition) },
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Box(Modifier.fillMaxSize()) {
                val image = coverImage
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title.trim().firstOrNull()?.uppercase() ?: "?",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (author != null) {
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
