package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jaytux.grader.viewmodel.GroupAssignmentState
import com.jaytux.grader.viewmodel.SoloAssignmentState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAssignmentView(state: GroupAssignmentState) {
    val (course, edition) = state.editionCourse
    val name by state.name
    val task by state.task
    val deadline by state.deadline
    val allFeedback by state.feedback.entities

    var idx by remember(state) { mutableStateOf(0) }

    Column(Modifier.padding(10.dp)) {
        PaneHeader(name, "group assignment", course, edition)
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
    val name by state.name
    val (course, edition) = state.editionCourse
    val task by state.task
    val deadline by state.deadline
    val suggestions by state.autofill.entities
    val grades by state.feedback.entities

    var idx by remember(state) { mutableStateOf(0) }

    Column(Modifier.padding(10.dp)) {
        PaneHeader(name, "individual assignment", course, edition)
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
                    var sGrade by remember { mutableStateOf(fg?.grade ?: "") }
                    var sMsg by remember { mutableStateOf(TextFieldValue(fg?.feedback ?: "")) }
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