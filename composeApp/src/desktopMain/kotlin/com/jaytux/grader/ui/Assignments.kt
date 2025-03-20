package com.jaytux.grader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.jaytux.grader.data.Student
import com.jaytux.grader.viewmodel.GroupAssignmentState
import com.jaytux.grader.viewmodel.PeerEvaluationState
import com.jaytux.grader.viewmodel.SoloAssignmentState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAssignmentView(state: GroupAssignmentState) {
    val task by state.task
    val deadline by state.deadline
    val allFeedback by state.feedback.entities

    var idx by remember(state) { mutableStateOf(0) }

    Column(Modifier.padding(10.dp)) {
        if(allFeedback.any { it.second.feedback == null }) {
            Text("Groups in bold have no feedback yet.", fontStyle = FontStyle.Italic)
        }
        else {
            Text("All groups have feedback.", fontStyle = FontStyle.Italic)
        }

        TabRow(idx) {
            Tab(idx == 0, { idx = 0 }) { Text("Assignment") }
            allFeedback.forEachIndexed { i, it ->
                val (group, feedback) = it
                Tab(idx == i + 1, { idx = i + 1 }) {
                    Text(group.name, fontWeight = feedback.feedback?.let { FontWeight.Normal } ?: FontWeight.Bold)
                }
            }
        }

        if(idx == 0) {
            val updTask = rememberRichTextState()

            LaunchedEffect(task) { updTask.setMarkdown(task) }

            Row {
                DateTimePicker(deadline, { state.updateDeadline(it) })
            }
            RichTextStyleRow(state = updTask)
            OutlinedRichTextEditor(
                state = updTask,
                modifier = Modifier.fillMaxWidth().weight(1f),
                singleLine = false,
                minLines = 5,
                label = { Text("Task") }
            )
            CancelSaveRow(true, { updTask.setMarkdown(task) }, "Reset", "Update") { state.updateTask(updTask.toMarkdown()) }
        }
        else {
            groupFeedback(state, allFeedback[idx - 1].second)
        }
    }
}

@Composable
fun groupFeedback(state: GroupAssignmentState, fdbk: GroupAssignmentState.LocalGFeedback) {
    val (group, feedback, individual) = fdbk
    var grade by remember(fdbk) { mutableStateOf(feedback?.grade ?: "") }
    var msg by remember(fdbk) { mutableStateOf(TextFieldValue(feedback?.feedback ?: "")) }
    var idx by remember(fdbk) { mutableStateOf(0) }
    val suggestions by state.autofill.entities

    Row {
        Surface(Modifier.weight(0.25f), tonalElevation = 10.dp) {
            LazyColumn(Modifier.fillMaxHeight().padding(10.dp)) {
                item {
                    Surface(
                        Modifier.fillMaxWidth().clickable { idx = 0 },
                        tonalElevation = if (idx == 0) 50.dp else 0.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Group feedback", Modifier.padding(5.dp), fontStyle = FontStyle.Italic)
                    }
                }

                itemsIndexed(individual.toList()) { i, (student, details) ->
                    val (role, _) = details
                    Surface(
                        Modifier.fillMaxWidth().clickable { idx = i + 1 },
                        tonalElevation = if (idx == i + 1) 50.dp else 0.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("${student.name} (${role ?: "no role"})", Modifier.padding(5.dp))
                    }
                }
            }
        }

        Column(Modifier.weight(0.75f).padding(10.dp)) {
            if(idx == 0) {
                Row {
                    Text("Grade: ", Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(grade, { grade = it }, Modifier.weight(0.2f))
                    Spacer(Modifier.weight(0.6f))
                    Button({ state.upsertGroupFeedback(group, msg.text, grade) }, Modifier.weight(0.2f).align(Alignment.CenterVertically),
                        enabled = grade.isNotBlank() || msg.text.isNotBlank()) {
                        Text("Save")
                    }
                }

                AutocompleteLineField(
                    msg, { msg = it }, Modifier.fillMaxWidth().weight(1f), { Text("Feedback") }
                ) { filter ->
                    suggestions.filter { x -> x.trim().startsWith(filter.trim()) }
                }
            }
            else {
                val (student, details) = individual[idx - 1]
                var sGrade by remember { mutableStateOf(details.second?.grade ?: "") }
                var sMsg by remember { mutableStateOf(TextFieldValue(details.second?.feedback ?: "")) }
                Row {
                    Text("Grade: ", Modifier.align(Alignment.CenterVertically))
                    OutlinedTextField(sGrade, { sGrade = it }, Modifier.weight(0.2f))
                    Spacer(Modifier.weight(0.6f))
                    Button({ state.upsertIndividualFeedback(student, group, sMsg.text, sGrade) }, Modifier.weight(0.2f).align(Alignment.CenterVertically),
                        enabled = sGrade.isNotBlank() || sMsg.text.isNotBlank()) {
                        Text("Save")
                    }
                }

                AutocompleteLineField(
                    sMsg, { sMsg = it }, Modifier.fillMaxWidth().weight(1f), { Text("Feedback") }
                ) { filter ->
                    suggestions.filter { x -> x.trim().startsWith(filter.trim()) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloAssignmentView(state: SoloAssignmentState) {
    val task by state.task
    val deadline by state.deadline
    val suggestions by state.autofill.entities
    val grades by state.feedback.entities

    var idx by remember(state) { mutableStateOf(0) }

    Column(Modifier.padding(10.dp)) {
        Row {
            Surface(Modifier.weight(0.25f), tonalElevation = 10.dp) {
                LazyColumn(Modifier.fillMaxHeight().padding(10.dp)) {
                    item {
                        Surface(
                            Modifier.fillMaxWidth().clickable { idx = 0 },
                            tonalElevation = if (idx == 0) 50.dp else 0.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Assignment", Modifier.padding(5.dp), fontStyle = FontStyle.Italic)
                        }
                    }

                    itemsIndexed(grades.toList()) { i, (student, _) ->
                        Surface(
                            Modifier.fillMaxWidth().clickable { idx = i + 1 },
                            tonalElevation = if (idx == i + 1) 50.dp else 0.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(student.name, Modifier.padding(5.dp))
                        }
                    }
                }
            }

            Column(Modifier.weight(0.75f).padding(10.dp)) {
                if (idx == 0) {
                    val updTask = rememberRichTextState()

                    LaunchedEffect(task) { updTask.setMarkdown(task) }

                    Row {
                        DateTimePicker(deadline, { state.updateDeadline(it) })
                    }
                    RichTextStyleRow(state = updTask)
                    OutlinedRichTextEditor(
                        state = updTask,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        singleLine = false,
                        minLines = 5,
                        label = { Text("Task") }
                    )
                    CancelSaveRow(
                        true,
                        { updTask.setMarkdown(task) },
                        "Reset",
                        "Update"
                    ) { state.updateTask(updTask.toMarkdown()) }
                } else {
                    val (student, fg) = grades[idx - 1]
                    var sGrade by remember(idx) { mutableStateOf(fg?.grade ?: "") }
                    var sMsg by remember(idx) { mutableStateOf(TextFieldValue(fg?.feedback ?: "")) }
                    Row {
                        Text("Grade: ", Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(sGrade, { sGrade = it }, Modifier.weight(0.2f))
                        Spacer(Modifier.weight(0.6f))
                        Button(
                            { state.upsertFeedback(student, sMsg.text, sGrade) },
                            Modifier.weight(0.2f).align(Alignment.CenterVertically),
                            enabled = sGrade.isNotBlank() || sMsg.text.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }

                    AutocompleteLineField(
                        sMsg, { sMsg = it }, Modifier.fillMaxWidth().weight(1f), { Text("Feedback") }
                    ) { filter ->
                        suggestions.filter { x -> x.trim().startsWith(filter.trim()) }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerEvaluationView(state: PeerEvaluationState) {
    val contents by state.contents.entities
    var idx by remember(state) { mutableStateOf(0) }
    var editing by remember(state) { mutableStateOf<Triple<Student, Student?, PeerEvaluationState.Student2StudentEntry?>?>(null) }
    val measure = rememberTextMeasurer()

    val isSelected = { from: Student, to: Student? ->
        editing?.let { (f, t, _) -> f == from && t == to } ?: false
    }

    Column(Modifier.padding(10.dp)) {
        TabRow(idx) {
            contents.forEachIndexed { i, it ->
                Tab(idx == i, { idx = i; editing = null }) { Text(it.group.name) }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row {
            val current = contents[idx]
            val horScroll = rememberLazyListState()
            val style = LocalTextStyle.current
            val textLenMeasured = remember(state, idx) {
                current.students.maxOf { (s, _) ->
                    measure.measure(s.name, style).size.width
                } + 10
            }
            val cellSize = 75.dp

            Column(Modifier.weight(0.5f)) {
                Row {
                    Box { FromTo(textLenMeasured.dp) }
                    LazyRow(Modifier.height(textLenMeasured.dp), state = horScroll) {
                        item { VLine() }
                        items(current.students) { (s, _) ->
                            Box(
                                Modifier.width(cellSize).height(textLenMeasured.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                var _h: Int = 0
                                Text(s.name, Modifier.layout{ m, c ->
                                    val p = m.measure(c.copy(minWidth = c.maxWidth, maxWidth = Constraints.Infinity))
                                    _h = p.height
                                    layout(p.height, p.width) { p.place(0, 0) }
                                }.graphicsLayer {
                                    rotationZ = -90f
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    translationX = _h.toFloat() / 2f
                                    translationY = textLenMeasured.dp.value - 15f
                                })
                            }
                        }
                        item { VLine() }
                        item {
                            Box(
                                Modifier.width(cellSize).height(textLenMeasured.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                var _h: Int = 0
                                Text("Group Rating", Modifier.layout{ m, c ->
                                    val p = m.measure(c.copy(minWidth = c.maxWidth, maxWidth = Constraints.Infinity))
                                    _h = p.height
                                    layout(p.height, p.width) { p.place(0, 0) }
                                }.graphicsLayer {
                                    rotationZ = -90f
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    translationX = _h.toFloat() / 2f
                                    translationY = textLenMeasured.dp.value - 15f
                                }, fontWeight = FontWeight.Bold)
                            }
                        }
                        item { VLine() }
                    }
                }
                MeasuredLazyColumn(key = idx) {
                    measuredItem { HLine() }
                    items(current.students) { (from, glob, map) ->
                        Row(Modifier.height(cellSize)) {
                            Text(from.name, Modifier.width(textLenMeasured.dp).align(Alignment.CenterVertically))
                            LazyRow(state = horScroll) {
                                item { VLine() }
                                items(map) { (to, entry) ->
                                    PEGradeWidget(entry,
                                        { editing = Triple(from, to, entry) }, { editing = null },
                                        isSelected(from, to), Modifier.size(cellSize, cellSize)
                                    )
                                }
                                item { VLine() }
                                item {
                                    PEGradeWidget(glob,
                                        { editing = Triple(from, null, glob) }, { editing = null },
                                        isSelected(from, null), Modifier.size(cellSize, cellSize))
                                }
                                item { VLine() }
                            }
                        }
                    }
                    measuredItem { HLine() }
                }
            }

            Column(Modifier.weight(0.5f)) {
                var groupLevel by remember(state, idx) { mutableStateOf(contents[idx].content) }
                editing?.let {
                    Column(Modifier.weight(0.5f)) {
                        val (from, to, data) = it

                        var sGrade by remember(editing) { mutableStateOf(data?.grade ?: "") }
                        var sMsg by remember(editing) { mutableStateOf(data?.feedback ?: "") }

                        Box(Modifier.padding(5.dp)) {
                            to?.let { s2 ->
                                if(from == s2)
                                    Text("Self-evaluation by ${from.name}", fontWeight = FontWeight.Bold)
                                else
                                    Text("Evaluation of ${s2.name} by ${from.name}", fontWeight = FontWeight.Bold)
                            } ?: Text("Group-level evaluation by ${from.name}", fontWeight = FontWeight.Bold)
                        }

                        Row {
                            Text("Grade: ", Modifier.align(Alignment.CenterVertically))
                            OutlinedTextField(sGrade, { sGrade = it }, Modifier.weight(0.2f))
                            Spacer(Modifier.weight(0.6f))
                            Button(
                                { state.upsertIndividualFeedback(from, to, sGrade, sMsg); editing = null },
                                Modifier.weight(0.2f).align(Alignment.CenterVertically),
                                enabled = sGrade.isNotBlank() || sMsg.isNotBlank()
                            ) {
                                Text("Save")
                            }
                        }

                        OutlinedTextField(
                            sMsg, { sMsg = it }, Modifier.fillMaxWidth().weight(1f),
                            label = { Text("Feedback") },
                            singleLine = false,
                            minLines = 5
                        )
                    }
                }

                Column(Modifier.weight(0.5f)) {
                    Row {
                        Text("Group-level notes", Modifier.weight(1f).align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
                        Button(
                            { state.upsertGroupFeedback(current.group, groupLevel); editing = null },
                            enabled = groupLevel != contents[idx].content
                        ) { Text("Update") }
                    }

                    OutlinedTextField(
                        groupLevel, { groupLevel = it }, Modifier.fillMaxWidth().weight(1f),
                        label = { Text("Group-level notes") },
                        singleLine = false,
                        minLines = 5
                    )
                }
            }
        }
    }
}