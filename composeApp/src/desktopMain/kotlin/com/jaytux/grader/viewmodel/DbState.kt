package com.jaytux.grader.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.jaytux.grader.data.*
import com.jaytux.grader.data.EditionStudents.editionId
import com.jaytux.grader.data.EditionStudents.studentId
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.math.max

fun <T> MutableState<T>.immutable(): State<T> = this
fun <T> SizedIterable<T>.sortAsc(vararg columns: Expression<*>) = this.orderBy(*(columns.map { it to SortOrder.ASC }.toTypedArray()))

enum class AssignmentType { Solo, Group }
sealed class Assignment {
    class GAssignment(val assignment: GroupAssignment) : Assignment() {
        override fun name(): String = assignment.name
        override fun id(): EntityID<UUID> = assignment.id
        override fun index(): Int? = assignment.number
    }
    class SAssignment(val assignment: SoloAssignment) : Assignment() {
        override fun name(): String = assignment.name
        override fun id(): EntityID<UUID> = assignment.id
        override fun index(): Int? = assignment.number
    }

    abstract fun name(): String
    abstract fun id(): EntityID<UUID>
    abstract fun index(): Int?

    companion object {
        fun from(assignment: GroupAssignment) = GAssignment(assignment)
        fun from(assignment: SoloAssignment) = SAssignment(assignment)

        fun merge(groups: List<GroupAssignment>, solos: List<SoloAssignment>): List<Assignment> {
            val g = groups.map { from(it) }
            val s = solos.map { from(it) }
            return (g + s).sortedWith(compareBy<Assignment> { it.index() }.thenBy { it.name() })
        }
    }
}

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

enum class OpenPanel(val tabName: String) {
    Student("Students"), Group("Groups"), Assignment("Assignments")
}

class EditionState(val edition: Edition) {
    val course = transaction { edition.course }
    val students = RawDbState { edition.soloStudents.sortAsc(Students.name).toList() }
    val groups = RawDbState { edition.groups.sortAsc(Groups.name).toList() }
    val solo = RawDbState { edition.soloAssignments.sortAsc(SoloAssignments.name).toList() }
    val groupAs = RawDbState { edition.groupAssignments.sortAsc(GroupAssignments.name).toList() }
    private val _history = mutableStateOf(listOf(-1 to OpenPanel.Assignment))
    val history = _history.immutable()

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
    fun setStudentName(student: Student, name: String) {
        transaction {
            student.name = name
        }
        students.refresh()
    }
    fun addToCourse(students: List<Student>) {
        transaction {
            EditionStudents.batchInsert(students) {
                this[editionId] = edition.id
                this[studentId] = it.id
            }
        }
        availableStudents.refresh()
        this.students.refresh()
    }

    fun newGroup(name: String) {
        transaction {
            Group.new { this.name = name; this.edition = this@EditionState.edition }
            groups.refresh()
        }
    }
    fun setGroupName(group: Group, name: String) {
        transaction {
            group.name = name
        }
        groups.refresh()
    }

    private fun now(): LocalDateTime {
        val instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        return instant.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private fun nextIdx(): Int = max(
        solo.entities.value.maxOfOrNull { it.number ?: 0 } ?: 0,
        groupAs.entities.value.maxOfOrNull { it.number ?: 0 } ?: 0
    ) + 1

    fun newSoloAssignment(name: String) {
        transaction {
            SoloAssignment.new {
                this.name = name; this.edition = this@EditionState.edition; assignment = ""; deadline = now()
                this.number = nextIdx()
            }
            solo.refresh()
        }
    }
    fun setSoloAssignmentTitle(assignment: SoloAssignment, title: String) {
        transaction {
            assignment.name = title
        }
        solo.refresh()
    }
    fun newGroupAssignment(name: String) {
        transaction {
            GroupAssignment.new {
                this.name = name; this.edition = this@EditionState.edition; assignment = ""; deadline = now()
                this.number = nextIdx()
            }
            groupAs.refresh()
        }
    }
    fun setGroupAssignmentTitle(assignment: GroupAssignment, title: String) {
        transaction {
            assignment.name = title
        }
        groupAs.refresh()
    }

    fun newAssignment(type: AssignmentType, name: String) = when(type) {
        AssignmentType.Solo -> newSoloAssignment(name)
        AssignmentType.Group -> newGroupAssignment(name)
    }
    fun setAssignmentTitle(assignment: Assignment, title: String) = when(assignment) {
        is Assignment.GAssignment -> setGroupAssignmentTitle(assignment.assignment, title)
        is Assignment.SAssignment -> setSoloAssignmentTitle(assignment.assignment, title)
    }

    fun swapOrder(a1: Assignment, a2: Assignment) {
        transaction {
            when(a1) {
                is Assignment.GAssignment -> {
                    when(a2) {
                        is Assignment.GAssignment -> {
                            val temp = a1.assignment.number
                            a1.assignment.number = a2.assignment.number
                            a2.assignment.number = temp
                        }
                        is Assignment.SAssignment -> {
                            val temp = a1.assignment.number
                            a1.assignment.number = nextIdx()
                            a2.assignment.number = temp
                        }
                    }
                }
                is Assignment.SAssignment -> {
                    when(a2) {
                        is Assignment.GAssignment -> {
                            val temp = a1.assignment.number
                            a1.assignment.number = a2.assignment.number
                            a2.assignment.number = temp
                        }
                        is Assignment.SAssignment -> {
                            val temp = a1.assignment.number
                            a1.assignment.number = a2.assignment.number
                            a2.assignment.number = temp
                        }
                    }
                }
            }
        }
        solo.refresh(); groupAs.refresh()
    }

    fun delete(s: Student) {
        transaction {
            EditionStudents.deleteWhere { studentId eq s.id }
            GroupStudents.deleteWhere { studentId eq s.id }
            IndividualFeedbacks.deleteWhere { studentId eq s.id }
        }
        students.refresh(); availableStudents.refresh()
    }
    fun delete(g: Group) {
        transaction {
            GroupFeedbacks.deleteWhere { groupId eq g.id }
            IndividualFeedbacks.deleteWhere { groupId eq g.id }
            GroupStudents.deleteWhere { groupId eq g.id }
            g.delete()
        }
        groups.refresh(); groupAs.refresh()
    }
    fun delete(sa: SoloAssignment) {
        transaction {
            SoloFeedbacks.deleteWhere { soloAssignmentId eq sa.id }
            sa.delete()
        }
        solo.refresh()
    }
    fun delete(ga: GroupAssignment) {
        transaction {
            GroupFeedbacks.deleteWhere { groupAssignmentId eq ga.id }
            IndividualFeedbacks.deleteWhere { groupAssignmentId eq ga.id }
            ga.delete()
        }
        groupAs.refresh()
    }
    fun delete(assignment: Assignment) = when(assignment) {
        is Assignment.GAssignment -> delete(assignment.assignment)
        is Assignment.SAssignment -> delete(assignment.assignment)
    }

    fun navTo(panel: OpenPanel, id: Int = -1) {
        _history.value += (id to panel)
    }
    fun navTo(id: Int) = navTo(_history.value.last().second, id)
    fun back() {
        var temp = _history.value.dropLast(1)
        while(temp.last().first == -1 && temp.size >= 2) temp = temp.dropLast(1)
        _history.value = temp
    }
}

class StudentState(val student: Student, edition: Edition) {
    data class LocalGroupGrade(val groupName: String, val assignmentName: String, val groupGrade: String?, val indivGrade: String?)
    data class LocalSoloGrade(val assignmentName: String, val grade: String)

    val editionCourse = transaction { edition.course to edition }
    val groups = RawDbState { student.groups.sortAsc(Groups.name).map { it to (it.edition.course.name to it.edition.name) }.toList() }
    val courseEditions = RawDbState { student.courses.map{ it to it.course }.sortedWith {
        (e1, c1), (e2, c2) -> c1.name.compareTo(c2.name).let { if(it == 0) e1.name.compareTo(e2.name) else it }
    }.toList() }

    val groupGrades = RawDbState {
        val groupsForEdition = Group.find {
            (Groups.editionId eq edition.id) and (Groups.id inList student.groups.map { it.id })
        }.associate { it.id to it.name }

        val asGroup = (GroupAssignments innerJoin GroupFeedbacks innerJoin Groups).selectAll().where {
            GroupFeedbacks.groupId inList groupsForEdition.keys.toList()
        }.map { it[GroupFeedbacks.groupAssignmentId] to it }

        val asIndividual = (GroupAssignments innerJoin IndividualFeedbacks innerJoin Groups).selectAll().where {
            IndividualFeedbacks.studentId eq student.id
        }.map { it[IndividualFeedbacks.groupAssignmentId] to it }

        val res = mutableMapOf<EntityID<UUID>, LocalGroupGrade>()
        asGroup.forEach {
            val (gAId, gRow) = it

            res[gAId] = LocalGroupGrade(
                gRow[Groups.name], gRow[GroupAssignments.name], gRow[GroupFeedbacks.grade], null
            )
        }

        asIndividual.forEach {
            val (gAId, iRow) = it

            val og = res[gAId] ?: LocalGroupGrade(iRow[Groups.name], iRow[GroupAssignments.name], null, null)
            res[gAId] = og.copy(indivGrade = iRow[IndividualFeedbacks.grade])
        }

        res.values.toList()
    }

    val soloGrades = RawDbState {
        (SoloAssignments innerJoin SoloFeedbacks).selectAll().where {
            SoloFeedbacks.studentId eq student.id
        }.map { LocalSoloGrade(it[SoloAssignments.name], it[SoloFeedbacks.grade]) }.toList()
    }

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
        feedback.refresh(); autofill.refresh()
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
        feedback.refresh(); autofill.refresh()
    }

    fun updateTask(t: String) {
        transaction {
            assignment.assignment = t
        }
        _task.value = t
    }

    fun updateDeadline(d: LocalDateTime) {
        transaction {
            assignment.deadline = d
        }
        _deadline.value = d
    }
}

class SoloAssignmentState(val assignment: SoloAssignment) {
    data class LocalFeedback(val feedback: String, val grade: String)

    val editionCourse = transaction { assignment.edition.course to assignment.edition }
    private val _name = mutableStateOf(assignment.name); val name = _name.immutable()
    private val _task = mutableStateOf(assignment.assignment); val task = _task.immutable()
    val feedback = RawDbState { loadFeedback() }
    private val _deadline = mutableStateOf(assignment.deadline); val deadline = _deadline.immutable()

    val autofill = RawDbState {
        SoloFeedbacks.selectAll().where { SoloFeedbacks.soloAssignmentId eq assignment.id }.map {
            it[SoloFeedbacks.feedback].split('\n')
        }.flatten().distinct().sorted()
    }

    private fun Transaction.loadFeedback(): List<Pair<Student, LocalFeedback?>> {
        val students = editionCourse.second.soloStudents
        val feedbacks = SoloFeedbacks.selectAll().where {
            SoloFeedbacks.soloAssignmentId eq assignment.id
        }.associate {
            it[SoloFeedbacks.studentId] to LocalFeedback(it[SoloFeedbacks.feedback], it[SoloFeedbacks.grade])
        }

        return students.map { s -> s to feedbacks[s.id] }
    }

    fun upsertFeedback(student: Student, msg: String, grd: String) {
        transaction {
            SoloFeedbacks.upsert {
                it[soloAssignmentId] = assignment.id
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

    fun updateDeadline(d: LocalDateTime) {
        transaction {
            assignment.deadline = d
        }
        _deadline.value = d
    }
}




















