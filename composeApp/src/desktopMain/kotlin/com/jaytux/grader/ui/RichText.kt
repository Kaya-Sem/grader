package com.jaytux.grader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaytux.grader.loadClipboard
import com.jaytux.grader.toClipboard
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun RichTextStyleRow(
    modifier: Modifier = Modifier,
    state: RichTextState,
) {
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Row(modifier.fillMaxWidth()) {
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                    icon = Icons.Outlined.FormatBold
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                    icon = Icons.Outlined.FormatItalic
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                    icon = Icons.Outlined.FormatUnderlined
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                    icon = Icons.Outlined.FormatStrikethrough
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                fontSize = 28.sp
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.fontSize == 28.sp,
                    icon = Icons.Outlined.FormatSize
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                color = Color.Red
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.color == Color.Red,
                    icon = Icons.Filled.Circle,
                    tint = Color.Red
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleSpanStyle(
                            SpanStyle(
                                background = Color.Yellow
                            )
                        )
                    },
                    isSelected = state.currentSpanStyle.background == Color.Yellow,
                    icon = Icons.Outlined.Circle,
                    tint = Color.Yellow
                )
            }

            item {
                Box(
                    Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color(0xFF393B3D))
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleUnorderedList()
                    },
                    isSelected = state.isUnorderedList,
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleOrderedList()
                    },
                    isSelected = state.isOrderedList,
                    icon = Icons.Outlined.FormatListNumbered,
                )
            }

            item {
                Box(
                    Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color(0xFF393B3D))
                )
            }

            item {
                RichTextStyleButton(
                    onClick = {
                        state.toggleCodeSpan()
                    },
                    isSelected = state.isCodeSpan,
                    icon = Icons.Outlined.Code,
                )
            }
        }

        IconButton({ state.toClipboard(clip) }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy markdown")
        }
        IconButton({ state.loadClipboard(clip, scope) }) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste markdown")
        }
    }
}

@Composable
fun RichTextStyleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color? = null,
    isSelected: Boolean = false,
) {
    IconButton(
        modifier = Modifier
            // Workaround to prevent the rich editor
            // from losing focus when clicking on the button
            // (Happens only on Desktop)
            .focusProperties { canFocus = false },
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        ),
    ) {
        Icon(
            icon,
            contentDescription = icon.name,
            tint = tint ?: LocalContentColor.current,
            modifier = Modifier
                .background(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    shape = CircleShape
                )
        )
    }
}