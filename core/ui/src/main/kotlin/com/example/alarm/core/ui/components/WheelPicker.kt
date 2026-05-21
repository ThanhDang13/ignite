package com.example.alarm.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.alarm.core.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemCount: Int = 5
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val coroutineScope = rememberCoroutineScope()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    var itemHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(itemHeight) {
        with(density) {
            itemHeightPx = itemHeight.toPx().toInt()
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex +
                (listState.firstVisibleItemScrollOffset.toFloat() / itemHeightPx).toInt()
            val actualIndex = centerIndex.coerceIn(0, items.size - 1)
            if (actualIndex != selectedIndex) {
                onSelectionChanged(actualIndex)
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress) {
            coroutineScope.launch {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemCount)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val fadeHeight = size.height / visibleItemCount
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
                val centerY = size.height / 2
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, centerY - itemHeight.toPx() / 2),
                    end = Offset(size.width, centerY - itemHeight.toPx() / 2),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, centerY + itemHeight.toPx() / 2),
                    end = Offset(size.width, centerY + itemHeight.toPx() / 2),
                    strokeWidth = 2f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2))
        ) {
            items(items.size) { index ->
                val centerIndex = listState.firstVisibleItemIndex +
                    (listState.firstVisibleItemScrollOffset.toFloat() / itemHeightPx).toInt()
                val distanceFromCenter = abs(index - centerIndex)
                val scale = (1f - (distanceFromCenter * 0.2f)).coerceIn(0.6f, 1f)
                val alpha = (1f - (distanceFromCenter * 0.3f)).coerceIn(0.3f, 1f)

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .onSizeChanged {
                            if (itemHeightPx == 0) {
                                itemHeightPx = it.height
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = if (distanceFromCenter == 0) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun IntWheelPicker(
    range: IntRange,
    selectedValue: Int,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemCount: Int = 5,
    formatValue: (Int) -> String = { "%02d".format(it) }
) {
    val items = remember(range) { range.map { formatValue(it) } }
    val selectedIndex = (selectedValue - range.first).coerceIn(0, items.size - 1)

    WheelPicker(
        items = items,
        selectedIndex = selectedIndex,
        onSelectionChanged = { index ->
            val value = range.first + index
            if (value != selectedValue) {
                onValueChanged(value)
            }
        },
        modifier = modifier,
        itemHeight = itemHeight,
        visibleItemCount = visibleItemCount
    )
}

@Composable
fun TimeWheelPicker(
    selectedHour: Int,
    selectedMinute: Int,
    selectedSecond: Int? = null,
    onTimeChanged: (hour: Int, minute: Int, second: Int?) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemCount: Int = 5
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IntWheelPicker(
            range = 0..23,
            selectedValue = selectedHour,
            onValueChanged = { onTimeChanged(it, selectedMinute, selectedSecond) },
            modifier = Modifier.weight(1f),
            itemHeight = itemHeight,
            visibleItemCount = visibleItemCount
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.spacing2)
        )

        IntWheelPicker(
            range = 0..59,
            selectedValue = selectedMinute,
            onValueChanged = { onTimeChanged(selectedHour, it, selectedSecond) },
            modifier = Modifier.weight(1f),
            itemHeight = itemHeight,
            visibleItemCount = visibleItemCount
        )

        if (selectedSecond != null) {
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Spacing.spacing2)
            )

            IntWheelPicker(
                range = 0..59,
                selectedValue = selectedSecond,
                onValueChanged = { onTimeChanged(selectedHour, selectedMinute, it) },
                modifier = Modifier.weight(1f),
                itemHeight = itemHeight,
                visibleItemCount = visibleItemCount
            )
        }
    }
}
