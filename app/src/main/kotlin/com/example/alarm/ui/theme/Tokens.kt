package com.example.alarm.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Spacing System (8dp grid)
object Spacing {
    val spacing0 = 0.dp
    val spacing1 = 4.dp
    val spacing2 = 8.dp
    val spacing3 = 12.dp
    val spacing4 = 16.dp
    val spacing5 = 20.dp
    val spacing6 = 24.dp
    val spacing7 = 32.dp
    val spacing8 = 48.dp
    val spacing9 = 64.dp
}

// Corner Radius System
object Corners {
    val cornerSmall = 4.dp
    val cornerMedium = 8.dp
    val cornerLarge = 12.dp
    val cornerXLarge = 16.dp
    val cornerFull = 999.dp
}

// Elevation System
object Elevations {
    val elevationNone = 0.dp
    val elevationSmall = 2.dp
    val elevationMedium = 4.dp
    val elevationLarge = 8.dp
    val elevationXLarge = 12.dp
}

// Animation Durations
object AnimationDurations {
    val durationShort: Duration = 150.milliseconds
    val durationMedium: Duration = 300.milliseconds
    val durationLong: Duration = 500.milliseconds
    val durationXLong: Duration = 800.milliseconds
}

// Size System
object Sizes {
    // Button sizes
    val buttonHeightSmall = 36.dp
    val buttonHeightMedium = 44.dp
    val buttonHeightLarge = 56.dp
    val buttonHeightXLarge = 64.dp

    // Icon sizes
    val iconSmall = 16.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconXLarge = 48.dp

    // Touch target minimum
    val touchTargetMinimum = 48.dp

    // Card sizes
    val cardMinHeight = 64.dp
    val cardMaxWidth = 400.dp
}

// Typography Sizes
object TextSizes {
    val displayLarge = 57.sp
    val displayMedium = 45.sp
    val displaySmall = 36.sp

    val headlineLarge = 32.sp
    val headlineMedium = 28.sp
    val headlineSmall = 24.sp

    val titleLarge = 22.sp
    val titleMedium = 16.sp
    val titleSmall = 14.sp

    val bodyLarge = 16.sp
    val bodyMedium = 14.sp
    val bodySmall = 12.sp

    val labelLarge = 14.sp
    val labelMedium = 12.sp
    val labelSmall = 11.sp
}
