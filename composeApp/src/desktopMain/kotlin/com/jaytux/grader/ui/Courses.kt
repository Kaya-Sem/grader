package com.jaytux.grader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.jaytux.grader.data.Course
import com.jaytux.grader.viewmodel.CourseListState

@Composable
fun CoursesView(state: CourseListState) {
    val data by state.courses.entities
    var showDialog by remember { mutableStateOf(false) }

    if(data.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center)) {
                Text("You have no courses yet.", Modifier.align(Alignment.CenterHorizontally))
                Button({ showDialog = true }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Add a course")
                }
            }
        }
    }
    else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(data) { CourseWidget(it) { state.delete(it) } }

            item {
                Button({ showDialog = true }, Modifier.fillMaxWidth()) {
                    Text("Add course")
                }
            }
        }
    }

    if(showDialog) AddCourseDialog(data.map { it.name }, { showDialog = false }) { state.new(it) }
}

@Composable
fun AddCourseDialog(taken: List<String>, onClose: () -> Unit, onSave: (String) -> Unit) = DialogWindow(
    onCloseRequest = onClose,
    state = rememberDialogState(size = DpSize(400.dp, 300.dp), position = WindowPosition(Alignment.Center))
) {
    Surface(Modifier.fillMaxSize().padding(10.dp)) {
        Box(Modifier.fillMaxSize()) {
            var name by remember { mutableStateOf("") }
            Column(Modifier.align(Alignment.Center)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Course name") }, isError = name in taken)
                Row {
                    Button({ onClose() }, Modifier.weight(0.45f)) { Text("Cancel") }
                    Spacer(Modifier.weight(0.1f))
                    Button({ onSave(name); onClose() }, Modifier.weight(0.45f)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun CourseWidget(course: Course, onDelete: () -> Unit) {
    val editions = remember(course) { course.loadEditions().size }
    Surface(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 10.dp), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(course.name, Modifier.padding(5.dp), style = MaterialTheme.typography.headlineMedium)
                Row {
                    Spacer(Modifier.width(15.dp))
                    Text("$editions editions", fontStyle = FontStyle.Italic)
                }
            }
            Column {
                IconButton({ onDelete() }) { Icon(Icons.Default.Delete, "Remove") }
                IconButton({ TODO() }) { Icon(Icons.Default.Edit, "Edit") }
            }
        }
    }
}