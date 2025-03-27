package com.jaytux.grader.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.jaytux.grader.data.*
import com.jaytux.grader.data.EditionStudents.editionId
import com.jaytux.grader.data.EditionStudents.studentId
import com.jaytux.grader.viewmodel.GroupAssignmentState.*
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

enum class AssignmentType(val show: String) { Solo("Solo Assignment"), Group("Group Assignment"), Peer("Peer Evaluation") }
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
    class PeerEval(val evaluation: com.jaytux.grader.data.PeerEvaluation) : Assignment() {
        override fun name(): String = evaluation.name
        override fun id(): EntityID<UUID> = evaluation.id
        override fun index(): Int? = evaluation.number
    }

    abstract fun name(): String
    abstract fun id(): EntityID<UUID>
    abstract fun index(): Int?

    companion object {
        fun from(assignment: GroupAssignment) = GAssignment(assignment)
        fun from(assignment: SoloAssignment) = SAssignment(assignment)
        fun from(pEval: PeerEvaluation) = PeerEval(pEval)

        fun merge(groups: List<GroupAssignment>, solos: List<SoloAssignment>, peers: List<PeerEvaluation>): List<Assignment> {
            val g = groups.map { from(it) }
            val s = solos.map { from(it) }
            val p = peers.map { from(it) }
            return (g + s + p).sortedWith(compareBy<Assignment> { it.index() }.thenBy { it.name() })
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
    val peer = RawDbState { edition.peerEvaluations.sortAsc(PeerEvaluations.name).toList() }
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
    fun newPeerEvaluation(name: String) {
        transaction {
            PeerEvaluation.new {
                this.name = name; this.edition = this@EditionState.edition
                this.number = nextIdx()
            }
            peer.refresh()
        }
    }
    fun setPeerEvaluationTitle(assignment: PeerEvaluation, title: String) {
        transaction {
            assignment.name = title
        }
        peer.refresh()
    }

    fun newAssignment(type: AssignmentType, name: String) = when(type) {
        AssignmentType.Solo -> newSoloAssignment(name)
        AssignmentType.Group -> newGroupAssignment(name)
        AssignmentType.Peer -> newPeerEvaluation(name)
    }
    fun setAssignmentTitle(assignment: Assignment, title: String) = when(assignment) {
        is Assignment.GAssignment -> setGroupAssignmentTitle(assignment.assignment, title)
        is Assignment.SAssignment -> setSoloAssignmentTitle(assignment.assignment, title)
        is Assignment.PeerEval -> setPeerEvaluationTitle(assignment.evaluation, title)
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
                        is Assignment.PeerEval -> {
                            val temp = a1.assignment.number
                            a1.assignment.number = nextIdx()
                            a2.evaluation.number = temp
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
                        is Assignment.PeerEval -> {
                            val temp = a1.assignment.number
                            a1.assignment.number = nextIdx()
                            a2.evaluation.number = temp
                        }
                    }
                }
                is Assignment.PeerEval -> {
                    when(a2) {
                        is Assignment.GAssignment -> {
                            val temp = a1.evaluation.number
                            a1.evaluation.number = a2.assignment.number
                            a2.assignment.number = temp
                        }
                        is Assignment.SAssignment -> {
                            val temp = a1.evaluation.number
                            a1.evaluation.number = a2.assignment.number
                            a2.assignment.number = temp
                        }
                        is Assignment.PeerEval -> {
                            val temp = a1.evaluation.number
                            a1.evaluation.number = a2.evaluation.number
                            a2.evaluation.number = temp
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
    fun delete(pe: PeerEvaluation) {
        transaction {
            PeerEvaluationContents.deleteWhere { peerEvaluationId eq pe.id }
            StudentToStudentEvaluation.deleteWhere { peerEvaluationId eq pe.id }
            pe.delete()
        }
        peer.refresh()
    }
    fun delete(assignment: Assignment) = when(assignment) {
        is Assignment.GAssignment -> delete(assignment.assignment)
        is Assignment.SAssignment -> delete(assignment.assignment)
        is Assignment.PeerEval -> delete(assignment.evaluation)
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
    data class FeedbackEntry(val feedback: String, val grade: String)
    data class LocalCriterionFeedback(
        val criterion: GroupAssignmentCriterion, val entry: FeedbackEntry?
    )
    data class LocalFeedback(
        val global: FeedbackEntry?, val byCriterion: List<LocalCriterionFeedback>
    )
    data class LocalGFeedback(
        val group: Group,
        val feedback: LocalFeedback,
        val individuals: List<Pair<Student, Pair<String?, LocalFeedback>>>
    )

    val editionCourse = transaction { assignment.edition.course to assignment.edition }
    private val _name = mutableStateOf(assignment.name); val name = _name.immutable()
    private val _task = mutableStateOf(assignment.assignment); val task = _task.immutable()
    private val _deadline = mutableStateOf(assignment.deadline); val deadline = _deadline.immutable()
    val criteria = RawDbState {
        assignment.criteria.orderBy(GroupAssignmentCriteria.name to SortOrder.ASC).toList()
    }
    val feedback = RawDbState { loadFeedback() }

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
        return Group.find {
            (Groups.editionId eq assignment.edition.id)
        }.sortAsc(Groups.name).map { group ->
            // step 1: group-level feedback, including criteria
            val forGroup = GroupFeedbacks.selectAll().where {
                (GroupFeedbacks.groupAssignmentId eq assignment.id) and
                        (GroupFeedbacks.groupId eq group.id)
            }.associate {
                val criterion = it[GroupFeedbacks.criterionId]?.let { id -> GroupAssignmentCriterion[id] }
                val fe = FeedbackEntry(it[GroupFeedbacks.feedback], it[GroupFeedbacks.grade])
                criterion to fe
            }
            val feedback = LocalFeedback(
                global = forGroup[null],
                byCriterion = criteria.entities.value.map { c -> LocalCriterionFeedback(c, forGroup[c]) }
            )

            // step 2: individual feedback
            val individuals = group.studentRoles.map { sr ->
                val student = sr.student
                val role = sr.role

                val forStudent = IndividualFeedbacks.selectAll().where {
                    (IndividualFeedbacks.groupAssignmentId eq assignment.id) and
                            (IndividualFeedbacks.groupId eq group.id) and
                            (IndividualFeedbacks.studentId eq student.id)
                }.associate {
                    val criterion = it[IndividualFeedbacks.criterionId]?.let { id -> GroupAssignmentCriterion[id] }
                    val fe = FeedbackEntry(it[IndividualFeedbacks.feedback], it[IndividualFeedbacks.grade])
                    criterion to fe
                }
                val studentFeedback = LocalFeedback(
                    global = forStudent[null],
                    byCriterion = criteria.entities.value.map { c -> LocalCriterionFeedback(c, forStudent[c]) }
                )

                student to (role to studentFeedback)
            }.sortedBy { it.first.name }

            group to LocalGFeedback(group, feedback, individuals)
        }
    }

    fun upsertGroupFeedback(group: Group, msg: String, grd: String, criterion: GroupAssignmentCriterion? = null) {
        transaction {
            GroupFeedbacks.upsert {
                it[groupAssignmentId] = assignment.id
                it[groupId] = group.id
                it[this.feedback] = msg
                it[this.grade] = grd
                it[criterionId] = criterion?.id
            }
        }
        feedback.refresh(); autofill.refresh()
    }

    fun upsertIndividualFeedback(student: Student, group: Group, msg: String, grd: String, criterion: GroupAssignmentCriterion? = null) {
        transaction {
            IndividualFeedbacks.upsert {
                it[groupAssignmentId] = assignment.id
                it[groupId] = group.id
                it[studentId] = student.id
                it[this.feedback] = msg
                it[this.grade] = grd
                it[criterionId] = criterion?.id
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

    fun addCriterion(name: String) {
        transaction {
            GroupAssignmentCriterion.new {
                this.name = name;
                this.description = "";
                this.assignment = this@GroupAssignmentState.assignment
            }
            criteria.refresh()
        }
    }

    fun updateCriterion(criterion: GroupAssignmentCriterion, name: String, desc: String) {
        transaction {
            criterion.name = name
            criterion.description = desc
        }
        criteria.refresh()
    }

    fun deleteCriterion(criterion: GroupAssignmentCriterion) {
        transaction {
            GroupFeedbacks.deleteWhere { criterionId eq criterion.id }
            IndividualFeedbacks.deleteWhere { criterionId eq criterion.id }
            criterion.delete()
        }
        criteria.refresh()
    }
}

class SoloAssignmentState(val assignment: SoloAssignment) {
    data class LocalFeedback(val feedback: String, val grade: String)
    data class FullFeedback(val global: LocalFeedback?, val byCriterion: List<Pair<SoloAssignmentCriterion, LocalFeedback?>>)

    val editionCourse = transaction { assignment.edition.course to assignment.edition }
    private val _name = mutableStateOf(assignment.name); val name = _name.immutable()
    private val _task = mutableStateOf(assignment.assignment); val task = _task.immutable()
    private val _deadline = mutableStateOf(assignment.deadline); val deadline = _deadline.immutable()
    val criteria = RawDbState {
        assignment.criteria.orderBy(SoloAssignmentCriteria.name to SortOrder.ASC).toList()
    }
    val feedback = RawDbState { loadFeedback() }

    val autofill = RawDbState {
        SoloFeedbacks.selectAll().where { SoloFeedbacks.soloAssignmentId eq assignment.id }.map {
            it[SoloFeedbacks.feedback].split('\n')
        }.flatten().distinct().sorted()
    }

    private fun Transaction.loadFeedback(): List<Pair<Student, FullFeedback>> {
        return editionCourse.second.soloStudents.sortAsc(Students.name).map { student ->
            val each = SoloFeedbacks.selectAll().where {
                (SoloFeedbacks.soloAssignmentId eq assignment.id) and
                        (SoloFeedbacks.studentId eq student.id)
            }.associate {
                val criterion = it[SoloFeedbacks.criterionId]?.let { id -> SoloAssignmentCriterion[id] }
                val fe = LocalFeedback(it[SoloFeedbacks.feedback], it[SoloFeedbacks.grade])
                criterion to fe
            }
            val feedback = FullFeedback(
                global = each[null],
                byCriterion = criteria.entities.value.map { c -> c to each[c] }
            )

            student to feedback
        }
    }

    fun upsertFeedback(student: Student, msg: String?, grd: String?, criterion: SoloAssignmentCriterion? = null) {
        transaction {
            SoloFeedbacks.upsert {
                it[soloAssignmentId] = assignment.id
                it[studentId] = student.id
                it[this.feedback] = msg ?: ""
                it[this.grade] = grd ?: ""
                it[criterionId] = criterion?.id
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

    fun addCriterion(name: String) {
        transaction {
            SoloAssignmentCriterion.new {
                this.name = name;
                this.description = "";
                this.assignment = this@SoloAssignmentState.assignment
            }
            criteria.refresh()
        }
    }

    fun updateCriterion(criterion: SoloAssignmentCriterion, name: String, desc: String) {
        transaction {
            criterion.name = name
            criterion.description = desc
        }
        criteria.refresh()
    }

    fun deleteCriterion(criterion: SoloAssignmentCriterion) {
        transaction {
            SoloFeedbacks.deleteWhere { criterionId eq criterion.id }
            criterion.delete()
        }
        criteria.refresh()
    }
}

class PeerEvaluationState(val evaluation: PeerEvaluation) {
    data class Student2StudentEntry(val grade: String, val feedback: String)
    data class StudentEntry(val student: Student, val global: Student2StudentEntry?, val others: List<Pair<Student, Student2StudentEntry?>>)
    data class GroupEntry(val group: Group, val content: String, val students: List<StudentEntry>)
    val editionCourse = transaction { evaluation.edition.course to evaluation.edition }
    private val _name = mutableStateOf(evaluation.name); val name = _name.immutable()
    val contents = RawDbState { loadContents() }

    private fun Transaction.loadContents(): List<GroupEntry> {
        val found = (Groups leftJoin PeerEvaluationContents).selectAll().where {
            Groups.editionId eq evaluation.edition.id
        }.associate { gc ->
            val group = Group[gc[Groups.id]]
            val content = gc[PeerEvaluationContents.content] ?: ""
            val students = group.students.map { student1 ->
                val others = group.students.map { student2 ->
                    val eval = StudentToStudentEvaluation.selectAll().where {
                        StudentToStudentEvaluation.peerEvaluationId eq evaluation.id and
                        (StudentToStudentEvaluation.studentIdFrom eq student1.id) and
                        (StudentToStudentEvaluation.studentIdTo eq student2.id)
                    }.firstOrNull()
                    student2 to eval?.let {
                        Student2StudentEntry(
                            it[StudentToStudentEvaluation.grade], it[StudentToStudentEvaluation.note]
                        )
                    }
                }.sortedBy { it.first.name }
                val global = StudentToGroupEvaluation.selectAll().where {
                    StudentToGroupEvaluation.peerEvaluationId eq evaluation.id and
                    (StudentToGroupEvaluation.studentId eq student1.id)
                }.firstOrNull()?.let {
                    Student2StudentEntry(it[StudentToGroupEvaluation.grade], it[StudentToGroupEvaluation.note])
                }

                StudentEntry(student1, global, others)
            }.sortedBy { it.student.name } // enforce synchronized order

            group to GroupEntry(group, content, students)
        }

        return editionCourse.second.groups.map {
            found[it] ?: GroupEntry(
                it, "",
                it.students.map { s1 -> StudentEntry(s1, null, it.students.map { s2 -> s2 to null }) }
            )
        }
    }

    fun upsertGroupFeedback(group: Group, feedback: String) {
        transaction {
            PeerEvaluationContents.upsert {
                it[peerEvaluationId] = evaluation.id
                it[groupId] = group.id
                it[this.content] = feedback
            }
        }
        contents.refresh()
    }

    fun upsertIndividualFeedback(from: Student, to: Student?, grade: String, feedback: String) {
        transaction {
            to?.let {
                StudentToStudentEvaluation.upsert {
                    it[peerEvaluationId] = evaluation.id
                    it[studentIdFrom] = from.id
                    it[studentIdTo] = to.id
                    it[this.grade] = grade
                    it[this.note] = feedback
                }
            } ?: StudentToGroupEvaluation.upsert {
                it[peerEvaluationId] = evaluation.id
                it[studentId] = from.id
                it[this.grade] = grade
                it[this.note] = feedback
            }
        }
        contents.refresh()
    }
}



















