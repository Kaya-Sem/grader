package com.jaytux.grader.data

import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

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
    val note = text("note")
}

object GroupStudents : Table("grpStudents") {
    val groupId = reference("group_id", Groups.id)
    val studentId = reference("student_id", Students.id)

    override val primaryKey = PrimaryKey(groupId, studentId)
}

object SoloStudents : Table("soloStudents") {
    val editionId = reference("edition_id", Editions.id)
    val studentId = reference("student_id", Students.id)

    override val primaryKey = PrimaryKey(studentId)
}

object GroupAssignments : UUIDTable("grpAssgmts") {
    val editionId = reference("edition_id_", Editions.id)
    val name = varchar("name_", 50)
    val assignment = text("assignment_")
}

object SoloAssignments : UUIDTable("soloAssgmts") {
    val editionId = GroupAssignments.reference("edition_id", Editions.id)
    val name = GroupAssignments.varchar("name", 50)
    val assignment = GroupAssignments.text("assignment")
}

object GroupFeedbacks : CompositeIdTable("grpFdbks") {
    val groupAssignmentId = reference("group_assignment_id", GroupAssignments.id)
    val groupId = reference("group_id", Groups.id)
    val feedback = text("feedback")
    val grade = varchar("grade", 32)

    override val primaryKey = PrimaryKey(groupAssignmentId, groupId)
}

object IndividualFeedbacks : CompositeIdTable("indivFdbks") {
    val groupAssignmentId = reference("group_assignment_id", GroupAssignments.id)
    val groupId = reference("group_id", Groups.id)
    val studentId = reference("student_id", Students.id)
    val feedback = text("feedback")
    val grade = varchar("grade", 32)

    override val primaryKey = PrimaryKey(groupAssignmentId, studentId)
}

object SoloFeedbacks : CompositeIdTable("soloFdbks") {
    val soloAssignmentId = reference("solo_assignment_id", SoloAssignments.id)
    val studentId = reference("student_id", Students.id)
    val feedback = text("feedback")
    val grade = varchar("grade", 32)

    override val primaryKey = PrimaryKey(soloAssignmentId, studentId)
}