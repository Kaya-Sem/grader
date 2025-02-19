package com.jaytux.grader.data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    val db by lazy {
        val actual = Database.connect("jdbc:sqlite:file:./grader.db", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(
                Courses, Editions, Groups,
                Students, GroupStudents, EditionStudents,
                GroupAssignments, SoloAssignments,
                GroupFeedbacks, IndividualFeedbacks, SoloFeedbacks
            )
        }
        actual
    }

    fun init() { db }
}