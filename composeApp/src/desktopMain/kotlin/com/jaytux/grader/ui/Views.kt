package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.jaytux.grader.viewmodel.GroupState
import com.jaytux.grader.viewmodel.StudentState

@Composable
fun StudentView(state: StudentState) {
    val groups by state.groups.entities
    val courses by state.courseEditions.entities

    Column(Modifier.padding(10.dp)) {
        PaneHeader(state.student.name, "student", state.editionCourse)
        Row {
            Column(Modifier.padding(10.dp).weight(0.45f)) {
                Spacer(Modifier.height(10.dp))
                InteractToEdit(state.student.name, { state.update { this.name = it } }, "Name")
                InteractToEdit(state.student.contact, { state.update { this.contact = it } }, "Contact")
                InteractToEdit(state.student.note, { state.update { this.note = it } }, "Note", singleLine = false)
            }
            Box(Modifier.weight(0.55f)) {}
        }
        Row {
            Column(Modifier.weight(0.5f)) {
                Text("Courses", style = MaterialTheme.typography.headlineSmall)
                ListOrEmpty(courses, { Text("Not a member of any course") }) { _, it ->
                    val (ed, course) = it
                    Text("${course.name} (${ed.name})", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Column(Modifier.weight(0.5f)) {
                Text("Groups", style = MaterialTheme.typography.headlineSmall)
                ListOrEmpty(groups, { Text("Not a member of any group") }) { _, it ->
                    Row {
                        val (group, c) = it
                        val (course, ed) = c
                        Text(group.name, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(5.dp))
                        Text("(in course $course ($ed))", Modifier.align(Alignment.Bottom), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupView(state: GroupState) {
    val members by state.members.entities
    val available by state.availableStudents.entities
    val allRoles by state.roles.entities
    val (course, edition) = state.course

    var pickRole: Pair<String?, (String?) -> Unit>? by remember { mutableStateOf(null) }

    Column(Modifier.padding(10.dp)) {
        PaneHeader(state.group.name, "group", course, edition)
        Row {
            Column(Modifier.weight(0.5f)) {
                Text("Students", style = MaterialTheme.typography.headlineSmall)
                ListOrEmpty(members, { Text("No students in this group") }) { _, it ->
                    val (student, role) = it
                    Row {
                        Text(
                            "${student.name} (${role ?: "no role"})",
                            Modifier.weight(0.75f).align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton({ pickRole = role to { r -> state.updateRole(student, r) } }, Modifier.weight(0.12f)) {
                            Icon(Icons.Default.Edit, "Change role")
                        }
                        IconButton({ state.removeStudent(student) }, Modifier.weight(0.12f)) {
                            Icon(Icons.Default.Delete, "Remove student")
                        }
                    }
                }
            }
            Column(Modifier.weight(0.5f)) {
                Text("Available students", style = MaterialTheme.typography.headlineSmall)
                ListOrEmpty(available, { Text("No students available") }) { _, it ->
                    Row(Modifier.padding(5.dp)) {
                        IconButton({ state.addStudent(it) }) {
                            Icon(ChevronLeft, "Add student")
                        }
                        Text(it.name, Modifier.weight(0.75f).align(Alignment.CenterVertically), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    pickRole?.let {
        val (curr, onPick) = it
        RolePicker(allRoles, curr, { pickRole = null }, { role -> onPick(role); pickRole = null })
    }
}

@Composable
fun RolePicker(used: List<String>, curr: String?, onClose: () -> Unit, onSave: (String?) -> Unit) = DialogWindow(
    onCloseRequest = onClose,
    state = rememberDialogState(size = DpSize(400.dp, 500.dp), position = WindowPosition(Alignment.Center))
) {
    Surface(Modifier.fillMaxSize().padding(10.dp)) {
        Box(Modifier.fillMaxSize()) {
            var role by remember { mutableStateOf(curr ?: "") }
            Column {
                Text("Used roles:")
                LazyColumn(Modifier.weight(1.0f).padding(5.dp)) {
                    items(used) {
                        Surface(Modifier.fillMaxWidth().clickable { role = it }, tonalElevation = 5.dp) {
                            Text(it, Modifier.padding(5.dp))
                        }
                        Spacer(Modifier.height(5.dp))
                    }
                }
                OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth())
                CancelSaveRow(true, onClose) {
                    onSave(role.ifBlank { null })
                    onClose()
                }
            }
        }
    }
}