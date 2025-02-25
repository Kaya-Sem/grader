package com.jaytux.grader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.jaytux.grader.data.Course
import com.jaytux.grader.data.Edition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.byUnicodePattern
import java.util.*

@Composable
fun CancelSaveRow(canSave: Boolean, onCancel: () -> Unit, cancelText: String = "Cancel", saveText: String = "Save", onSave: () -> Unit) {
    Row {
        Button({ onCancel() }, Modifier.weight(0.45f)) { Text(cancelText) }
        Spacer(Modifier.weight(0.1f))
        Button({ onSave() }, Modifier.weight(0.45f), enabled = canSave) { Text(saveText) }
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
fun AddStringDialog(label: String, taken: List<String>, onClose: () -> Unit, current: String = "", onSave: (String) -> Unit) = DialogWindow(
    onCloseRequest = onClose,
    state = rememberDialogState(size = DpSize(400.dp, 300.dp), position = WindowPosition(Alignment.Center))
) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(10.dp)) {
            var name by remember(current) { mutableStateOf(current) }
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

@Composable
fun <T> ListOrEmpty(
    data: List<T>,
    emptyText: @Composable ColumnScope.() -> Unit,
    addText: @Composable RowScope.() -> Unit,
    onAdd: () -> Unit,
    addAfterLazy: Boolean = true,
    item: @Composable LazyItemScope.(idx: Int, it: T) -> Unit
) {
    if(data.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                emptyText()
                Button(onAdd, Modifier.align(Alignment.CenterHorizontally)) {
                    addText()
                }
            }
        }
    }
    else {
        Column {
            LazyColumn(Modifier.padding(5.dp).weight(1f)) {
                itemsIndexed(data) { idx, it ->
                    item(idx, it)
                }

                if(!addAfterLazy) {
                    item {
                        Button(onAdd, Modifier.fillMaxWidth()) {
                            addText()
                        }
                    }
                }
            }

            if(addAfterLazy) {
                Button(onAdd, Modifier.fillMaxWidth()) {
                    addText()
                }
            }
        }
    }
}

@Composable
fun <T> ListOrEmpty(
    data: List<T>,
    emptyText: @Composable ColumnScope.() -> Unit,
    item: @Composable LazyItemScope.(idx: Int, it: T) -> Unit
) {
    if(data.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                emptyText()
            }
        }
    }
    else {
        Column {
            LazyColumn(Modifier.padding(5.dp).weight(1f)) {
                itemsIndexed(data) { idx, it ->
                    item(idx, it)
                }
            }
        }
    }
}

@Composable
fun InteractToEdit(
    content: String, onSave: (String) -> Unit, pre: String, modifier: Modifier = Modifier,
    w1: Float = 0.75f, w2: Float = 0.25f,
    singleLine: Boolean = true
) {
    var text by remember(content) { mutableStateOf(content) }

    Row(modifier.padding(5.dp)) {
        val base = if(singleLine) Modifier.align(Alignment.CenterVertically) else Modifier
        OutlinedTextField(
            text, { text = it }, base.weight(w1), label = { Text(pre) },
            singleLine = singleLine, minLines = if(singleLine) 1 else 5
        )
        IconButton({ onSave(text) }, base.weight(w2)) { Icon(Icons.Default.Check, "Save") }
    }
}

@Composable
fun PaneHeader(name: String, type: String, course: Course, edition: Edition) = Column {
    Text(name, style = MaterialTheme.typography.headlineMedium)
    Text("${type.capitalize(Locale.current)} in ${course.name} (${edition.name})", fontStyle = FontStyle.Italic)
}

@Composable
fun PaneHeader(name: String, type: String, courseEdition: Pair<Course, Edition>) = PaneHeader(name, type, courseEdition.first, courseEdition.second)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutocompleteLineField(
    value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier, label: @Composable (() -> Unit)? = null,
    onFilter: (String) -> List<String>
) = Column(modifier) {
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    var selected by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val posToLine = { pos: Int ->
        (value.text.take(pos).count { it == '\n' }) to (value.text.take(pos).lastIndexOf('\n'))
    }

    val autoComplete = { str: String ->
        val pos = value.selection.start
        val lines = value.text.split("\n").toMutableList()
        val (lineno, lineStart) = posToLine(pos)

        lines[lineno] = str
        onValueChange(value.copy(text = lines.joinToString("\n"), selection = TextRange(lineStart + str.length + 1)))
    }

    val currentLine = {
        value.text.split('\n')[posToLine(value.selection.start).first]
    }

    val gotoOption = { idx: Int ->
        selected = if(suggestions.isEmpty()) 0 else ((idx + suggestions.size) % suggestions.size)
        scope.launch {
            scrollState.animateScrollToItem(if(suggestions.isNotEmpty()) (selected + 1) else 0)
        }
        Unit
    }

    val onKey = { kev: KeyEvent ->
        var res = true
        if(suggestions.isNotEmpty()) {
            when (kev.key) {
                Key.Tab -> autoComplete(suggestions[selected])
                Key.DirectionUp -> gotoOption(selected - 1)
                Key.DirectionDown -> gotoOption(selected + 1)
                Key.Escape -> suggestions = listOf()
                else -> res = false
            }
        }
        else res = false

        res
    }

    LaunchedEffect(value.text) {
        delay(300)
        suggestions = onFilter(currentLine())
        gotoOption(if(suggestions.isEmpty()) 0 else selected % suggestions.size)
    }

    OutlinedTextField(
        value, onValueChange,
        Modifier.fillMaxWidth().weight(0.75f).onKeyEvent(onKey), label = label, singleLine = false, minLines = 5
    )

    if(suggestions.isNotEmpty()) {
        LazyColumn(Modifier.weight(0.25f), state = scrollState) {
            stickyHeader {
                Surface(tonalElevation = 5.dp) {
                    Text("Suggestions", Modifier.padding(5.dp).fillMaxWidth(), fontStyle = FontStyle.Italic)
                }
            }
            itemsIndexed(suggestions) { idx, it ->
                Surface(Modifier.padding(5.dp).fillMaxWidth(), tonalElevation = if(selected == idx) 50.dp else 0.dp) {
                    Text(it, Modifier.clickable { autoComplete(it) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    value: LocalDateTime,
    onPick: (LocalDateTime) -> Unit,
    formatter: (LocalDateTime) -> String = { java.text.DateFormat.getDateTimeInstance().format(Date.from(it.toInstant(TimeZone.currentSystemDefault()).toJavaInstant())) },
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(modifier) {
        Text(
            formatter(value),
            Modifier.align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(10.dp))
        Button({ showPicker = true }) { Text("Change") }

        if (showPicker) {
            val dateState = rememberDatePickerState(value.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
            val timeState = rememberTimePickerState(value.hour, value.minute)

            Dialog(
                { showPicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp,
                    modifier = Modifier.width(800.dp).height(600.dp)
                ) {
                    val colors = TimePickerDefaults.colors(
                                    selectorColor = MaterialTheme.colorScheme.primary,
                                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ) // the colors are fucked, and I don't get why :(

                    Column(Modifier.padding(10.dp)) {
                        Row {
                            DatePicker(
                                dateState,
                                Modifier.padding(10.dp).weight(0.5f),
                            )
                            TimePicker(
                                timeState,
                                Modifier.weight(0.5f).align(Alignment.CenterVertically),
                                layoutType = TimePickerLayoutType.Vertical,
                                colors = colors
                            )
                        }
                        CancelSaveRow(true, { showPicker = false }) {
                            val date = (dateState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()) } ?: value).date
                            val time = LocalTime(timeState.hour, timeState.minute)

                            onPick(LocalDateTime(date, time))
                            showPicker = false
                        }
                    }
                }
            }
        }


//            DatePickerDialog(
//            { showPicker = false },
//            {
//                Button({
//                    showPicker = false; dateState.selectedDateMillis?.let { state.updateDeadline(it) }
//                }) { Text("Set deadline") }
//            },
//            Modifier,
//            { Button({ showPicker = false }) { Text("Cancel") } },
//            shape = MaterialTheme.shapes.medium,
//            tonalElevation = 10.dp,
//            colors = DatePickerDefaults.colors(),
//            properties = DialogProperties()
//        ) {
//            DatePicker(
//                dateState,
//                Modifier.fillMaxWidth().padding(10.dp),
//            )
//        }
    }
}