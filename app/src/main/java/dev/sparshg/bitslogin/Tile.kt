package dev.sparshg.bitslogin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ShapeDefaults.Medium
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


enum class TileState {
    TICKED, CROSS, EXCLAMATION, INFO
}

@Composable
fun Tile(
    title: String,
    desc: String,
    state: TileState = TileState.INFO,
    expanded: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
//    var selected by remember { mutableStateOf(false) }
//    val expanded = true
    val icon = when (state) {
        TileState.TICKED -> Icons.Filled.Check
        TileState.CROSS -> Icons.Filled.Close
        TileState.EXCLAMATION -> Icons.Filled.Warning
        TileState.INFO -> Icons.Filled.Info
    }
    val color = when (state) {
        TileState.TICKED -> MaterialTheme.colorScheme.primaryContainer
        TileState.CROSS -> MaterialTheme.colorScheme.errorContainer
        TileState.EXCLAMATION -> MaterialTheme.colorScheme.secondaryContainer
        TileState.INFO -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            color
//            containerColor = if (selected) color
//            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onClick
                )
                .padding(vertical = 20.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = when (state) {
                    TileState.TICKED -> "Ticked"
                    TileState.CROSS -> "Cross"
                    TileState.EXCLAMATION -> "Exclamation"
                    TileState.INFO -> "Info"
                },
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterVertically)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc, style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                )
            }
        }
    }
}