package com.jaytux.grader.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.jaytux.grader.data.*
import com.jaytux.grader.data.EditionStudents.editionId
import com.jaytux.grader.data.EditionStudents.studentId
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> MutableState<T>.immutable(): State<T> = this
fun <T> SizedIterable<T>.sortAsc(vararg columns: Expression<*>) = this.orderBy(*(columns.map { it to SortOrder.ASC }.toTypedArray()))

class RawDbState<T: Any>(private val loader: (Transaction.() -> List<T>)) {

    private val rawEntities by lazy {
        mutableStateOf(transaction { loader() })
    }
    val entities = rawEntities.immutable()

    fun refresh() {
        rawEntities.value = transaction { loader() }
    }
}

class CourseListState {
    val courses = RawDbState { Course.all().sortAsc(Courses.name).toList() }

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
    val editions = RawDbState { Edition.find { Editions.courseId eq course.id }.sortAsc(Editions.name).toList() }

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
    val students = RawDbState { edition.soloStudents.sortAsc(Students.name).toList() }
    val groups = RawDbState { edition.groups.sortAsc(Groups.name).toList() }
    val solo = RawDbState { edition.soloAssignments.sortAsc(SoloAssignments.name).toList() }
    val groupAs = RawDbState { edition.groupAssignments.sortAsc(GroupAssignments.name).toList() }

    val availableStudents = RawDbState {
        Student.find {
            (Students.id notInList edition.soloStudents.map { it.id })
        }.toList()
    }

    fun newStudent(name: String, contact: String, note: String, addToEdition: Boolean) {
        transaction {
            val student = Student.new { this.name = name; this.contact = contact; this.note = note }
            if(addToEdition) EditionStudents.insert {
                it[editionId] = edition.id
                it[studentId] = student.id
            }
        }

        if(addToEdition) students.refresh()
        else availableStudents.refresh()
    }

    fun addToCourse(students: List<Student>) {
        transaction {
            EditionStudents.batchInsert(students) {
                this[editionId] = edition.id
                this[studentId] = it.id
            }
        }
        availableStudents.refresh();
        this.students.refresh()
    }

    fun newGroup(name: String) {
        transaction {
            Group.new { this.name = name; this.edition = this@EditionState.edition }
            groups.refresh()
        }
    }

    fun newSoloAssignment(name: String) {
        transaction {
            SoloAssignment.new { this.name = name; this.edition = this@EditionState.edition; assignment = "" }
            solo.refresh()
        }
    }
    fun newGroupAssignment(name: String) {
        transaction {
            GroupAssignment.new { this.name = name; this.edition = this@EditionState.edition; assignment = "" }
            groupAs.refresh()
        }
    }
}

class StudentState(val student: Student, edition: Edition) {
    val editionCourse = transaction { edition.course to edition }
    val groups = RawDbState { student.groups.sortAsc(Groups.name).map { it to (it.edition.course.name to it.edition.name) }.toList() }
    val courseEditions = RawDbState { student.courses.map{ it to it.course }.sortedWith {
        (e1, c1), (e2, c2) -> c1.name.compareTo(c2.name).let { if(it == 0) e1.name.compareTo(e2.name) else it }
    }.toList() }

    fun update(f: Student.() -> Unit) {
        transaction {
            student.f()
        }
    }
}

class GroupState(val group: Group) {
    val members = RawDbState { group.studentRoles.map{ it.student to it.role }.sortedBy { it.first.name }.toList() }
    val availableStudents = RawDbState { Student.find {
        // not yet in the group
        (Students.id notInList group.students.map { it.id }) and
        // but in the same course (edition)
        (Students.id inList group.edition.soloStudents.map { it.id })
    }.sortAsc(Students.name).toList() }
    val course = transaction { group.edition.course to group.edition }
    val roles = RawDbState {
        GroupStudents.select(GroupStudents.role).where{ GroupStudents.role.isNotNull() }
            .withDistinct(true).sortAsc(GroupStudents.role).map{ it[GroupStudents.role] ?: "" }.toList()
    }

    fun addStudent(student: Student) {
        transaction {
            GroupStudents.insert {
                it[groupId] = group.id
                it[studentId] = student.id
            }
        }
        members.refresh(); availableStudents.refresh()
    }

    fun removeStudent(student: Student) {
        transaction {
            GroupStudents.deleteWhere { groupId eq group.id and (studentId eq student.id) }
        }
        members.refresh(); availableStudents.refresh()
    }

    fun updateRole(student: Student, role: String?) {
        transaction {
            GroupStudents.update({ GroupStudents.groupId eq group.id and (GroupStudents.studentId eq student.id) }) {
                it[this.role] = role
            }
            members.refresh()
            roles.refresh()
        }
    }
}

class GroupAssignmentState(val assignment: GroupAssignment) {
    data class LocalFeedback(val feedback: String, val grade: String)
    data class LocalGFeedback(
        val group: Group,
        val feedback: LocalFeedback?,
        val individuals: List<Pair<Student, Pair<String?, LocalFeedback?>>>
    )

    val editionCourse = transaction { assignment.edition.course to assignment.edition }
    private val _name = mutableStateOf(assignment.name); val name = _name.immutable()
    private val _task = mutableStateOf(assignment.assignment); val task = _task.immutable()
    val feedback = RawDbState { loadFeedback() }
    private val _deadline = mutableStateOf(assignment.deadline); val deadline = _deadline.immutable()

    val autofill = RawDbState {
        val forGroups = GroupFeedbacks.selectAll().where { GroupFeedbacks.groupAssignmentId eq assignment.id }.flatMap {
            it[GroupFeedbacks.feedback].split('\n')
        }

        val forIndividuals = IndividualFeedbacks.selectAll().where { IndividualFeedbacks.groupAssignmentId eq assignment.id }.flatMap {
            it[IndividualFeedbacks.feedback].split('\n')
        }

        (forGroups + forIndividuals).distinct().sorted()
    }

    private fun Transaction.loadFeedback(): List<Pair<Group, LocalGFeedback>> {
        val individuals = IndividualFeedbacks.selectAll().where {
            IndividualFeedbacks.groupAssignmentId eq assignment.id
        }.map {
            it[IndividualFeedbacks.studentId] to LocalFeedback(it[IndividualFeedbacks.feedback], it[IndividualFeedbacks.grade])
        }.associate { it }

        val groupFeedbacks = GroupFeedbacks.selectAll().where {
            GroupFeedbacks.groupAssignmentId eq assignment.id
        }.map {
            it[GroupFeedbacks.groupId] to (it[GroupFeedbacks.feedback] to it[GroupFeedbacks.grade])
        }.associate { it }

        val groups = Group.find {
            (Groups.editionId eq assignment.edition.id)
        }.sortAsc(Groups.name).map { group ->
            val students = group.studentRoles.sortedBy { it.student.name }.map { sR ->
                val student = sR.student
                val role = sR.role
                val feedback = individuals[student.id]

                student to (role to feedback)
            }

            groupFeedbacks[group.id]?.let { (f, g) ->
                group to LocalGFeedback(group, LocalFeedback(f, g), students)
            } ?: (group to LocalGFeedback(group, null, students))
        }

        return groups
    }

    fun upsertGroupFeedback(group: Group, msg: String, grd: String) {
        transaction {
            GroupFeedbacks.upsert {
                it[groupAssignmentId] = assignment.id
                it[groupId] = group.id
                it[this.feedback] = msg
                it[this.grade] = grd
            }
        }
        feedback.refresh()
    }

    fun upsertIndividualFeedback(student: Student, group: Group, msg: String, grd: String) {
        transaction {
            IndividualFeedbacks.upsert {
                it[groupAssignmentId] = assignment.id
                it[groupId] = group.id
                it[studentId] = student.id
                it[this.feedback] = msg
                it[this.grade] = grd
            }
        }
        feedback.refresh()
    }

    fun updateTask(t: String) {
        transaction {
            assignment.assignment = t
        }
        _task.value = t
    }

    fun updateDeadline(instant: Long) {
        val d = Instant.fromEpochMilliseconds(instant).toLocalDateTime(TimeZone.currentSystemDefault())
        transaction {
            assignment.deadline = d
        }
        _deadline.value = d
    }
}




















