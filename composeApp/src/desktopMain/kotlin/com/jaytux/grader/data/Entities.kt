package com.jaytux.grader.data

import com.jaytux.grader.data.GroupAssignment.Companion.referrersOn
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class Course(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, Course>(Courses)

    var name by Courses.name
    val editions by Edition referrersOn Editions.courseId
}

class Edition(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, Edition>(Editions)

    var course by Course referencedOn Editions.courseId
    var name by Editions.name
    val groups by Group referrersOn Groups.editionId
    val soloStudents by Student via EditionStudents
    val soloAssignments by SoloAssignment referrersOn SoloAssignments.editionId
    val groupAssignments by GroupAssignment referrersOn GroupAssignments.editionId
    val peerEvaluations by PeerEvaluation referrersOn PeerEvaluations.editionId
}

class Group(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, Group>(Groups)

    var edition by Edition referencedOn Groups.editionId
    var name by Groups.name
    val students by Student via GroupStudents
    val studentRoles by GroupMember referrersOn GroupStudents.groupId
}

class GroupMember(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, GroupMember>(GroupStudents)

    var student by Student referencedOn GroupStudents.studentId
    var group by Group referencedOn GroupStudents.groupId
    var role by GroupStudents.role
}

class Student(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, Student>(Students)

    var name by Students.name
    var note by Students.note
    var contact by Students.contact
    val groups by Group via GroupStudents
    val courses by Edition via EditionStudents
}

class GroupAssignment(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, GroupAssignment>(GroupAssignments)

    var edition by Edition referencedOn GroupAssignments.editionId
    var number by GroupAssignments.number
    var name by GroupAssignments.name
    var assignment by GroupAssignments.assignment
    var deadline by GroupAssignments.deadline

    val criteria by GroupAssignmentCriterion referrersOn GroupAssignmentCriteria.assignmentId
}

class GroupAssignmentCriterion(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, GroupAssignmentCriterion>(GroupAssignmentCriteria)

    var assignment by GroupAssignment referencedOn GroupAssignmentCriteria.assignmentId
    var name by GroupAssignmentCriteria.name
    var description by GroupAssignmentCriteria.desc
}

class SoloAssignment(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, SoloAssignment>(SoloAssignments)

    var edition by Edition referencedOn SoloAssignments.editionId
    var number by SoloAssignments.number
    var name by SoloAssignments.name
    var assignment by SoloAssignments.assignment
    var deadline by SoloAssignments.deadline

    val criteria by SoloAssignmentCriterion referrersOn SoloAssignmentCriteria.assignmentId
}

class SoloAssignmentCriterion(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, SoloAssignmentCriterion>(SoloAssignmentCriteria)

    var assignment by SoloAssignment referencedOn SoloAssignmentCriteria.assignmentId
    var name by SoloAssignmentCriteria.name
    var description by SoloAssignmentCriteria.desc

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SoloAssignmentCriterion

        if (name != other.name) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = assignment.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }
}

class PeerEvaluation(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, PeerEvaluation>(PeerEvaluations)

    var edition by Edition referencedOn PeerEvaluations.editionId
    var number by PeerEvaluations.number
    var name by PeerEvaluations.name
}