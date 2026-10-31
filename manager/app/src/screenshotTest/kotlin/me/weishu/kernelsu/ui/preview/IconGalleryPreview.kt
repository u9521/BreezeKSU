package me.weishu.kernelsu.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.ui.BreezePreviewTheme
import me.weishu.kernelsu.ui.icons.MaterialSymbol
import me.weishu.kernelsu.ui.icons.MaterialSymbols

/**
 * The icon set the manager ships, rendered for browsing.
 *
 * `MaterialSymbols.icons` is the generated registry, so this page shows exactly what
 * `icons.toml` declares -- including the style each icon was generated with, which is
 * the thing a reader cannot tell from the name alone. Adding an icon to the manifest
 * and regenerating makes it appear here with no edit to this file.
 *
 * It lives in `src/screenshotTest`: Compose previews render it, no APK contains it.
 */
@Composable
fun IconGalleryPreview() {
    BreezePreviewTheme {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 132.dp),
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MaterialSymbols.icons, key = { it.name + it.style.fill }) { icon ->
                IconCell(icon)
            }
        }
    }
}

@Composable
private fun IconCell(icon: MaterialSymbol) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon.vector,
            contentDescription = icon.name,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.padding(start = 8.dp).width(88.dp)) {
            Text(
                text = icon.name,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (icon.style.fill.value == 1) "filled" else "outlined",
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun IconGalleryPreviewRender() {
    IconGalleryPreview()
}
