package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jaytux.grader.viewmodel.GroupAssignmentState

@Composable
fun GroupAssignmentView(state: GroupAssignmentState) {
    val (course, edition) = state.editionCourse
    val name by state.name
    val task by state.task
    val allFeedback by state.feedback.entities

    var idx by remember { mutableStateOf(0) }

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
            var updTask by remember { mutableStateOf(task) }
            OutlinedTextField(updTask, { updTask = it }, Modifier.fillMaxWidth().weight(1f), singleLine = false, minLines = 5, label = { Text("Task") })
            CancelSaveRow(updTask != task, { updTask = task }, "Reset", "Update") { state.updateTask(updTask) }
        }
        else {
            val (group, feedback, individual) = allFeedback[idx - 1].second
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
                //
            }
        }
    }
}