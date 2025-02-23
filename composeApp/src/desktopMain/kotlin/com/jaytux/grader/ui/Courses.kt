package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.jaytux.grader.UiRoute
import com.jaytux.grader.data.Edition
import com.jaytux.grader.viewmodel.CourseListState
import com.jaytux.grader.viewmodel.EditionListState
import com.jaytux.grader.viewmodel.EditionState

@Composable
fun CoursesView(state: CourseListState, push: (UiRoute) -> Unit) {
    val data by state.courses.entities
    var showDialog by remember { mutableStateOf(false) }

    ListOrEmpty(
        data,
        { Text("You have no courses yet.", Modifier.align(Alignment.CenterHorizontally)) },
        { Text("Add a course") },
        { showDialog = true },
        addAfterLazy = false
    ) { _, it ->
        CourseWidget(state.getEditions(it), { state.delete(it) }, push)
    }

    if(showDialog) AddStringDialog("Course name", data.map { it.name }, { showDialog = false }) { state.new(it) }
}

@Composable
fun CourseWidget(state: EditionListState, onDelete: () -> Unit, push: (UiRoute) -> Unit) {
    val editions by state.editions.entities
    var isOpened by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val callback = { it: Edition ->
        val s = EditionState(it)
        val route = UiRoute("${state.course.name}: ${it.name}") {
            EditionView(s)
        }
        push(route)
    }

    Surface(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 10.dp).clickable { isOpened = !isOpened }, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row {
            Column(Modifier.weight(1f).padding(5.dp)) {
                Row {
                    Icon(
                        if (isOpened) ChevronDown else ChevronRight, "Toggle editions",
                        Modifier.size(MaterialTheme.typography.headlineMedium.fontSize.toDp())
                            .align(Alignment.CenterVertically)
                    )
                    Column {
                        Text(state.course.name, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Row {
                    Spacer(Modifier.width(25.dp))
                    Text(
                        "${editions.size} edition(s)",
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if(isOpened) {
                    Row {
                        Spacer(Modifier.width(25.dp))
                        Column {
                            editions.forEach { EditionWidget(it, { callback(it) }) { state.delete(it) } }
                            Button({ showDialog = true }, Modifier.fillMaxWidth()) { Text("Add edition") }
                        }
                    }
                }
            }
            Column {
                IconButton({ onDelete() }) { Icon(Icons.Default.Delete, "Remove") }
                IconButton({ TODO() }, enabled = false) { Icon(Icons.Default.Edit, "Edit") }
            }
        }
    }

    if(showDialog) AddStringDialog("Edition name", editions.map { it.name }, { showDialog = false }) { state.new(it) }
}

@Composable
fun EditionWidget(edition: Edition, onOpen: () -> Unit, onDelete: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 10.dp).clickable { onOpen() }, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row(Modifier.padding(5.dp)) {
            Text(edition.name, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            IconButton({ onDelete() }) { Icon(Icons.Default.Delete, "Remove") }
        }
    }
}