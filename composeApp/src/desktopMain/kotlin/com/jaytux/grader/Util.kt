package com.jaytux.grader

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun String.maxN(n: Int): String {
    return if (this.length > n) {
        this.substring(0, n - 3) + "..."
    } else {
        this
    }
}

fun RichTextState.toClipboard(clip: ClipboardManager) {
    clip.setText(AnnotatedString(this.toMarkdown()))
}

fun RichTextState.loadClipboard(clip: ClipboardManager, scope: CoroutineScope) {
    scope.launch { setMarkdown(clip.getText()?.text ?: "") }
}