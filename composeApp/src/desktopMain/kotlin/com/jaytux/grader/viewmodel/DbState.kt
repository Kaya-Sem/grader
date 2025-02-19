package com.jaytux.grader.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.jaytux.grader.data.*
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
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

    fun getEditions(course: Course) = EditionListState(course)
}

class EditionListState(val course: Course) {
    val editions = RawDbState { Edition.find { Editions.courseId eq course.id }.toList() }

    fun new(name: String) {
        transaction { Edition.new { this.name = name; this.course = this@EditionListState.course } }
        editions.refresh()
    }

    fun delete(edition: Edition) {
        transaction { edition.delete() }
        editions.refresh()
    }
}

class EditionState(val edition: Edition) {
    val course = transaction { edition.course }
    val students = RawDbState { edition.soloStudents.toList() }
    val groups = RawDbState { edition.groups.toList() }
    val groupAs = mutableStateOf(listOf<GroupAssignment>())
    val solo = RawDbState { edition.soloAssignments.toList() }

    fun newStudent(name: String, contact: String, note: String, addToEdition: Boolean) {
        transaction {
            val student = Student.new { this.name = name; this.contact = contact; this.note = note }
            if(addToEdition) EditionStudents.insert {
                it[editionId] = edition.id
                it[studentId] = student.id
            }
        }

        if(addToEdition) students.refresh()
    }

    fun newGroup(name: String) {
        transaction {
            Group.new { this.name = name; this.edition = this@EditionState.edition }
            groups.refresh()
        }
    }
}