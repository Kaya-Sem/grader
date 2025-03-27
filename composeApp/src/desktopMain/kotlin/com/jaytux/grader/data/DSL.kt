package com.jaytux.grader.data

import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Courses : UUIDTable("courses") {
    val name = varchar("name", 50).uniqueIndex()
}

object Editions : UUIDTable("editions") {
    val courseId = reference("course_id", Courses.id)
    val name = varchar("name", 50)

    init {
        uniqueIndex(courseId, name)
    }
}

object Groups : UUIDTable("groups") {
    val editionId = reference("edition_id", Editions.id)
    val name = varchar("name", 50)

    init {
        uniqueIndex(editionId, name)
    }
}

object Students : UUIDTable("students") {
    val name = varchar("name", 50)
    val contact = varchar("contact", 50)
    val note = text("note")
}

object GroupStudents : UUIDTable("grpStudents") {
    val groupId = reference("group_id", Groups.id)
    val studentId = reference("student_id", Students.id)
    val role = varchar("role", 50).nullable()

    init {
        uniqueIndex(groupId, studentId) // can't figure out how to make this a composite key
    }
}

object EditionStudents : Table("editionStudents") {
    val editionId = reference("edition_id", Editions.id)
    val studentId = reference("student_id", Students.id)

    override val primaryKey = PrimaryKey(studentId)
}

object GroupAssignments : UUIDTable("grpAssgmts") {
    val editionId = reference("edition_id", Editions.id)
    val number = integer("number").nullable()
    val name = varchar("name", 50)
    val assignment = text("assignment")
    val deadline = datetime("deadline")
}

object GroupAssignmentCriteria : UUIDTable("grpAsCr") {
    val assignmentId = reference("group_assignment_id", GroupAssignments.id)
    val name = varchar("name", 50)
    val desc = text("description")
}

object SoloAssignments : UUIDTable("soloAssgmts") {
    val editionId = reference("edition_id", Editions.id)
    val number = integer("number").nullable()
    val name = varchar("name", 50)
    val assignment = text("assignment")
    val deadline = datetime("deadline")
}

object SoloAssignmentCriteria : UUIDTable("soloAsCr") {
    val assignmentId = reference("solo_assignment_id", SoloAssignments.id)
    val name = varchar("name", 50)
    val desc = text("description")
}

object PeerEvaluations : UUIDTable("peerEvals") {
    val editionId = reference("edition_id", Editions.id)
    val number = integer("number").nullable()
    val name = varchar("name", 50)
}

object GroupFeedbacks : CompositeIdTable("grpFdbks") {
    val groupAssignmentId = reference("group_assignment_id", GroupAssignments.id)
    val criterionId = reference("criterion_id", GroupAssignments.id).nullable()
    val groupId = reference("group_id", Groups.id)
    val feedback = text("feedback")
    val grade = varchar("grade", 32)

    override val primaryKey = PrimaryKey(groupAssignmentId, groupId)
}

object IndividualFeedbacks : CompositeIdTable("indivFdbks") {
    val groupAssignmentId = reference("group_assignment_id", GroupAssignments.id)
    val criterionId = reference("criterion_id", GroupAssignments.id).nullable()
    val groupId = reference("group_id", Groups.id)
    val studentId = reference("student_id", Students.id)
    val feedback = text("feedback")
    val grade = varchar("grade", 32)

    override val primaryKey = PrimaryKey(groupAssignmentId, studentId)
}

object SoloFeedbacks : CompositeIdTable("soloFdbks") {
    val soloAssignmentId = reference("solo_assignment_id", SoloAssignments.id)
    val criterionId = reference("criterion_id", SoloAssignments.id).nullable()
    val studentId = reference("student_id", Students.id)
    val feedback = text("feedback")
    val grade = varchar("grade", 32)

    override val primaryKey = PrimaryKey(soloAssignmentId, studentId)
}

object PeerEvaluationContents : CompositeIdTable("peerEvalCnts") {
    val peerEvaluationId = reference("peer_evaluation_id", PeerEvaluations.id)
    val groupId = reference("group_id", Groups.id)
    val content = text("content")

    override val primaryKey = PrimaryKey(peerEvaluationId, groupId)
}

object StudentToGroupEvaluation : CompositeIdTable("stToGrEv") {
    val peerEvaluationId = reference("peer_evaluation_id", PeerEvaluations.id)
    val studentId = reference("student_id", Students.id)
    val grade = varchar("grade", 32)
    val note = text("note")

    override val primaryKey = PrimaryKey(peerEvaluationId, studentId)
}

object StudentToStudentEvaluation : CompositeIdTable("stToStEv") {
    val peerEvaluationId = reference("peer_evaluation_id", PeerEvaluations.id)
    val studentIdFrom = reference("student_id_from", Students.id)
    val studentIdTo = reference("student_id_to", Students.id)
    val grade = varchar("grade", 32)
    val note = text("note")

    override val primaryKey = PrimaryKey(peerEvaluationId, studentIdFrom, studentIdTo)
}