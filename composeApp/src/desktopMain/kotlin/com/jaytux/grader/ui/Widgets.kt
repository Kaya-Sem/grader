package com.jaytux.grader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState

@Composable
fun CancelSaveRow(canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row {
        Button({ onCancel() }, Modifier.weight(0.45f)) { Text("Cancel") }
        Spacer(Modifier.weight(0.1f))
        Button({ onSave() }, Modifier.weight(0.45f), enabled = canSave) { Text("Save") }
    }
}

@Composable
fun <T> TabLayout(
    options: List<T>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    optionContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = Column(modifier) {
    TabRow(currentIndex) {
        options.forEachIndexed { idx, it ->
            Tab(
                selected = idx == currentIndex,
                onClick = { onSelect(idx) },
                text = { optionContent(it) }
            )
        }
    }
    content()
}

@Composable
fun AddStringDialog(label: String, taken: List<String>, onClose: () -> Unit, onSave: (String) -> Unit) = DialogWindow(
    onCloseRequest = onClose,
    state = rememberDialogState(size = DpSize(400.dp, 300.dp), position = WindowPosition(Alignment.Center))
) {
    Surface(Modifier.fillMaxSize().padding(10.dp)) {
        Box(Modifier.fillMaxSize()) {
            var name by remember { mutableStateOf("") }
            Column(Modifier.align(Alignment.Center)) {
                androidx.compose.material.OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(label) }, isError = name in taken)
                CancelSaveRow(name.isNotBlank() && name !in taken, onClose) {
                    onSave(name)
                    onClose()
                }
            }
        }
    }
}