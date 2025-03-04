package com.jaytux.grader.data

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.CompositeID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
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
}

class SoloAssignment(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, SoloAssignment>(SoloAssignments)

    var edition by Edition referencedOn SoloAssignments.editionId
    var number by SoloAssignments.number
    var name by SoloAssignments.name
    var assignment by SoloAssignments.assignment
    var deadline by SoloAssignments.deadline
}

class GroupFeedback(id: EntityID<CompositeID>) : Entity<CompositeID>(id) {
    companion object : EntityClass<CompositeID, GroupFeedback>(GroupFeedbacks)

    var group by Group referencedOn GroupFeedbacks.groupId
    var assignment by GroupAssignment referencedOn GroupFeedbacks.groupAssignmentId
    var feedback by GroupFeedbacks.feedback
    var grade by GroupFeedbacks.grade
}

class IndividualFeedback(id: EntityID<CompositeID>) : Entity<CompositeID>(id) {
    companion object : EntityClass<CompositeID, IndividualFeedback>(IndividualFeedbacks)

    var student by Student referencedOn IndividualFeedbacks.studentId
    var assignment by SoloAssignment referencedOn IndividualFeedbacks.groupAssignmentId
    var feedback by IndividualFeedbacks.feedback
    var grade by IndividualFeedbacks.grade
}

class SoloFeedback(id: EntityID<CompositeID>) : Entity<CompositeID>(id) {
    companion object : EntityClass<CompositeID, SoloFeedback>(SoloFeedbacks)

    var student by Student referencedOn SoloFeedbacks.studentId
    var assignment by SoloAssignment referencedOn SoloFeedbacks.soloAssignmentId
    var feedback by SoloFeedbacks.feedback
    var grade by SoloFeedbacks.grade
}