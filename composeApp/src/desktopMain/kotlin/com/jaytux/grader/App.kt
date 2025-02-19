package com.jaytux.grader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaytux.grader.ui.ChevronLeft
import com.jaytux.grader.ui.CoursesView
import com.jaytux.grader.ui.toDp
import com.jaytux.grader.viewmodel.CourseListState
import org.jetbrains.compose.ui.tooling.preview.Preview

data class UiRoute(val heading: String, val content: @Composable (push: (UiRoute) -> Unit) -> Unit)

@Composable
@Preview
fun App() {
    MaterialTheme {
        val courseList = CourseListState()
        var stack by remember {
            val start = UiRoute("Courses Overview") { CoursesView(courseList, it) }
            mutableStateOf(listOf(start))
        }

        Column {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, tonalElevation = 10.dp, shadowElevation = 10.dp) {
                Row(Modifier.padding(10.dp)) {
                    IconButton({ stack = stack.toMutableList().also { it.removeLast() } }, enabled = stack.size >= 2) {
                        Icon(ChevronLeft, "Back", Modifier.size(MaterialTheme.typography.headlineLarge.fontSize.toDp()))
                    }
                    Text(stack.last().heading, Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.headlineLarge)
                }
            }
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.padding(10.dp)) {
                    stack.last().content { stack += (it) }
                }
            }
        }
    }
}