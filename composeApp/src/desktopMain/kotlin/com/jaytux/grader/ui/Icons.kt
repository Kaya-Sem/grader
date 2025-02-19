package com.jaytux.grader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ChevronRight: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChevronRight",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            fillAlpha = 1.0f,
            stroke = SolidColor(Color(0xFF000000)),
            strokeAlpha = 1.0f,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(9f, 18f)
            lineToRelative(6f, -6f)
            lineToRelative(-6f, -6f)
        }
    }.build()
}

val ChevronDown: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChevronDown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            fillAlpha = 1.0f,
            stroke = SolidColor(Color(0xFF000000)),
            strokeAlpha = 1.0f,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6f, 9f)
            lineToRelative(6f, 6f)
            lineToRelative(6f, -6f)
        }
    }.build()
}

public val ChevronLeft: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChevronLeft",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            fillAlpha = 1.0f,
            stroke = SolidColor(Color(0xFF000000)),
            strokeAlpha = 1.0f,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(15f, 18f)
            lineToRelative(-6f, -6f)
            lineToRelative(6f, -6f)
        }
    }.build()
}
