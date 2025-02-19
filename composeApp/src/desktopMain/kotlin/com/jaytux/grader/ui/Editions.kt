package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.jaytux.grader.data.*
import com.jaytux.grader.viewmodel.EditionState

@Composable
fun EditionView(state: EditionState) = Row {
    var isGroup by remember { mutableStateOf(false) }

    val students by state.students.entities
    val groups by state.groups.entities
    val solo by state.solo.entities
    val groupAs by state.groupAs//.entities

    TabLayout(
        listOf("Students", "Groups"),
        if(isGroup) 1 else 0,
        { isGroup = it == 1 },
        { Text(it) },
        Modifier.weight(0.25f)
    ) {
        Column(Modifier.fillMaxSize()) {
            if(isGroup) {
                Box(Modifier.weight(0.5f)) {
                    GroupsWidget(state.course, state.edition, groups, {}) { state.newGroup(it) }
                }
                Box(Modifier.weight(0.5f)) { GroupAssignmentsWidget(groupAs, {}) {} }
            }
            else {
                Box(Modifier.weight(0.5f)) {
                    StudentsWidget(state.course, state.edition, students, {}) { name, note, contact, addToEdition ->
                        state.newStudent(name, note, contact, addToEdition)
                    }
                }
                Box(Modifier.weight(0.5f)) { AssignmentsWidget(solo, {}) {} }
            }
        }
    }
    Box(Modifier.weight(0.75f)) {}
}

@Composable
fun StudentsWidget(
    course: Course,
    edition: Edition,
    students: List<Student>,
    onSelect: (Int) -> Unit,
    onAdd: (name: String, note: String, contact: String, addToEdition: Boolean) -> Unit
) = Column(Modifier.padding(10.dp)) {
    Text("Student list", style = MaterialTheme.typography.headlineMedium)
    var showDialog by remember { mutableStateOf(false) }
    if(students.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                Text(
                    "Course ${course.name} (edition ${edition.name})\nhas no students yet.",
                    Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                Button({ showDialog = true }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Add a student")
                }
            }
        }
    }
    else {
        LazyColumn(Modifier.padding(5.dp).weight(1f)) {
            itemsIndexed(students) { idx, it ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(idx) }) {
                    Text(it.name, Modifier.padding(5.dp))
                }
            }
        }

        Button({ showDialog = true }, Modifier.fillMaxWidth()) {
            Text("Add a student")
        }
    }

    if(showDialog) StudentDialog(course, edition, { showDialog = false }, onAdd)
}

@Composable
fun StudentDialog(
    course: Course,
    edition: Edition,
    onClose: () -> Unit,
    onAdd: (name: String, note: String, contact: String, addToEdition: Boolean) -> Unit
) = DialogWindow(
    onCloseRequest = onClose,
    state = rememberDialogState(size = DpSize(600.dp, 400.dp), position = WindowPosition(Alignment.Center))
) {
    Surface(Modifier.fillMaxSize().padding(10.dp)) {
        Box(Modifier.fillMaxSize()) {
            var name by remember { mutableStateOf("") }
            var contact by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }
            var add by remember { mutableStateOf(true) }

            Column(Modifier.align(Alignment.Center)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Student name") })
                OutlinedTextField(contact, { contact = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Student contact") })
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), singleLine = false, minLines = 3, label = { Text("Note") })
                Row {
                    Checkbox(add, { add = it })
                    Text("Add student to ${course.name} ${edition.name}?", Modifier.align(Alignment.CenterVertically))
                }
                CancelSaveRow(name.isNotBlank() && contact.isNotBlank(), onClose) {
                    onAdd(name, note, contact, add)
                    onClose()
                }
            }
        }
    }
}

@Composable
fun GroupsWidget(
    course: Course,
    edition: Edition,
    groups: List<Group>,
    onSelect: (Int) -> Unit,
    onAdd: (name: String) -> Unit
) = Column(Modifier.padding(10.dp)) {
    Text("Group list", style = MaterialTheme.typography.headlineMedium)
    var showDialog by remember { mutableStateOf(false) }

    if(groups.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                Text(
                    "Course ${course.name} (edition ${edition.name})\nhas no groups yet.",
                    Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                Button({ showDialog = true }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Add a group")
                }
            }
        }
    }
    else {
        LazyColumn(Modifier.padding(5.dp).weight(1f)) {
            itemsIndexed(groups) { idx, it ->
                Surface(Modifier.fillMaxWidth().clickable { onSelect(idx) }) {
                    Text(it.name, Modifier.padding(5.dp))
                }
            }
        }

        Button({ showDialog = true }, Modifier.fillMaxWidth()) {
            Text("Add a group")
        }
    }

    if(showDialog) AddStringDialog("Group name", groups.map { it.name }, { showDialog = false }) { onAdd(it) }
}

@Composable
fun AssignmentsWidget(assignments: List<SoloAssignment>, onSelect: (Int) -> Unit, onAdd: (name: String) -> Unit) {
    //
}

@Composable
fun GroupAssignmentsWidget(assignments: List<GroupAssignment>, onSelect: (Int) -> Unit, onAdd: (name: String) -> Unit) {
    //
}