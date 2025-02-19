package com.jaytux.grader.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.jaytux.grader.data.Course
import com.jaytux.grader.data.Edition
import com.jaytux.grader.data.Editions
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class RawDbState<T: Entity<UUID>>(private val loader: (Transaction.() -> List<T>)) {
    private fun <T> MutableState<T>.immutable(): State<T> = this@immutable

    private val rawEntities by lazy {
        mutableStateOf(transaction { loader() })
    }
    val entities = rawEntities.immutable()

    fun refresh() {
        rawEntities.value = transaction { loader() }
    }
}

class CourseListState {
    val courses = RawDbState { Course.all().toList() }

    fun new(name: String) {
        transaction { Course.new { this.name = name } }
        courses.refresh()
    }

    fun delete(course: Course) {
        transaction { course.delete() }
        courses.refresh()
    }
}

class EditionListState(private val course: Course) {
    val editions = RawDbState { Edition.find { Editions.courseId eq course.id }.toList() }
}