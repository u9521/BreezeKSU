package me.weishu.kernelsu.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.breeze.bar.BreezeTopAppBar
import me.weishu.kernelsu.ui.component.breeze.button.BreezeBackButton
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedListGroup
import me.weishu.kernelsu.ui.component.breeze.list.SegmentedNavItem
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScaffold
import me.weishu.kernelsu.ui.component.breeze.scaffold.BreezeScrollColumn

@Composable
fun AboutScreenBreeze(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    BreezeScaffold(
        topBar = {
            BreezeTopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    BreezeBackButton(
                        onClick = actions.onBack,
                        collapseFraction = scrollBehavior.state.collapsedFraction,
                    )
                },
            )
        },
    ) { innerPadding ->
        BreezeScrollColumn(
            innerPadding = innerPadding,
            horizontalPadding = 0.dp,
            bottomExtraSpacing = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        contentScale = FixedScale(1f)
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(id = R.string.app_name),
                    fontWeight = FontWeight.Medium,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
            SegmentedListGroup(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                state.links.forEach { linkInfo ->
                    item(key = linkInfo.url) {
                        SegmentedNavItem(
                            title = linkInfo.fullText,
                            onClick = { actions.onOpenLink(linkInfo.url) },
                            trailing = {},
                        )
                    }
                }
            }
        }
    }
}
