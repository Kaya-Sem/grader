package com.jaytux.grader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaytux.grader.ui.CoursesView
import com.jaytux.grader.viewmodel.CourseListState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val courseList = CourseListState()
        Surface(Modifier.fillMaxSize().padding(10.dp)) {
            CoursesView(courseList)
        }
    }
}