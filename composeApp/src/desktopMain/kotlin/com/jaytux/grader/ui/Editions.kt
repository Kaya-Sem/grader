package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.jaytux.grader.data.*
import com.jaytux.grader.viewmodel.*

enum class OpenPanel(val tabName: String) {
    Student("Students"), Group("Groups"), Assignment("Assignments")
}

data class Navigators(
    val student: (Student) -> Unit,
    val group: (Group) -> Unit,
    val assignment: (Assignment) -> Unit
)

@Composable
fun EditionView(state: EditionState) = Row(Modifier.padding(0.dp)) {
    val course = state.course; val edition = state.edition
    val students by state.students.entities
    val availableStudents by state.availableStudents.entities
    val groups by state.groups.entities
    val solo by state.solo.entities
    val groupAs by state.groupAs.entities
    val mergedAssignments by remember(solo, groupAs) {
        mutableStateOf(Assignment.merge(groupAs, solo))
    }
    var selected by remember { mutableStateOf(-1) }
    var tab by remember { mutableStateOf(OpenPanel.Assignment) }

    val navs = Navigators(
        student = { tab = OpenPanel.Student; selected = students.indexOfFirst { s -> s.id == it.id } },
        group = { tab = OpenPanel.Group; selected = groups.indexOfFirst { g -> g.id == it.id } },
        assignment = { tab = OpenPanel.Assignment; selected = mergedAssignments.indexOfFirst { a -> a.id() == it.id() } }
    )

    Surface(Modifier.weight(0.25f), tonalElevation = 5.dp) {
        TabLayout(
            OpenPanel.entries,
            tab.ordinal,
            { tab = OpenPanel.entries[it]; selected = -1 },
            { Text(it.tabName) }
        ) {
            when(tab) {
                OpenPanel.Student -> StudentPanel(
                    course, edition, students, availableStudents, selected,
                    { selected = it },
                    { name, note, contact, add -> state.newStudent(name, contact, note, add) },
                    { students -> state.addToCourse(students) },
                    { s, name -> state.setStudentName(s, name) }
                ) { s -> state.delete(s) }

                OpenPanel.Group -> GroupPanel(
                    course, edition, groups, selected,
                    { selected = it },
                    { name -> state.newGroup(name) },
                    { g, name -> state.setGroupName(g, name) }
                ) { g -> state.delete(g) }

                OpenPanel.Assignment -> AssignmentPanel(
                    course, edition, mergedAssignments, selected,
                    { selected = it },
                    { type, name -> state.newAssignment(type, name) },
                    { a, name -> state.setAssignmentTitle(a, name) }
                ) { a -> state.delete(a) }
            }
        }
    }

    Box(Modifier.weight(0.75f)) {
        if(selected != -1) {
            when(tab) {
                OpenPanel.Student -> StudentView(StudentState(students[selected], edition), navs)
                OpenPanel.Group -> GroupView(GroupState(groups[selected]), navs)
                OpenPanel.Assignment -> {
                    when(val a = mergedAssignments[selected]) {
                        is Assignment.SAssignment -> SoloAssignmentView(SoloAssignmentState(a.assignment))
                        is Assignment.GAssignment -> GroupAssignmentView(GroupAssignmentState(a.assignment))
                    }
                }
            }
        }
    }
}

@Composable
fun StudentPanel(
    course: Course, edition: Edition, students: List<Student>, available: List<Student>,
    selected: Int, onSelect: (Int) -> Unit,
    onAdd: (name: String, note: String, contact: String, addToEdition: Boolean) -> Unit,
    onImport: (List<Student>) -> Unit, onUpdate: (Student, String) -> Unit, onDelete: (Student) -> Unit
) = Column(Modifier.padding(10.dp)) {
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(-1) }
    var editing by remember { mutableStateOf(-1) }

    Text("Student list (${students.size})", style = MaterialTheme.typography.headlineMedium)

    ListOrEmpty(
        students,
        { Text(
            "Course ${course.name} (edition ${edition.name})\nhas no students yet.",
            Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center
        ) },
        { Text("Add a student") },
        { showDialog = true }
    ) { idx, it ->
        SelectEditDeleteRow(
            selected == idx,
            { onSelect(idx) }, { onSelect(-1) },
            { editing = idx }, { deleting = idx }
        ) {
            Text(it.name, Modifier.padding(5.dp))
        }
    }

    if(showDialog) {
        StudentDialog(course, edition, { showDialog = false }, available, onImport, onAdd)
    }
    else if(editing != -1) {
        AddStringDialog("Student name", students.map { it.name }, { editing = -1 }, students[editing].name) {
            onUpdate(students[editing], it)
        }
    }
    else if(deleting != -1) {
        ConfirmDeleteDialog(
            "a student",
            { deleting = -1 },
            { onDelete(students[deleting]) }
        ) { Text(students[deleting].name) }
    }
}

@Composable
fun GroupPanel(
    course: Course, edition: Edition, groups: List<Group>,
    selected: Int, onSelect: (Int) -> Unit,
    onAdd: (String) -> Unit, onUpdate: (Group, String) -> Unit, onDelete: (Group) -> Unit
) = Column(Modifier.padding(10.dp)) {
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(-1) }
    var editing by remember { mutableStateOf(-1) }

    Text("Group list (${groups.size})", style = MaterialTheme.typography.headlineMedium)

    ListOrEmpty(
        groups,
        { Text(
            "Course ${course.name} (edition ${edition.name})\nhas no groups yet.",
            Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center
        ) },
        { Text("Add a group") },
        { showDialog = true }
    ) { idx, it ->
        SelectEditDeleteRow(
            selected == idx,
            { onSelect(idx) }, { onSelect(-1) },
            { editing = idx }, { deleting = idx }
        ) {
            Text(it.name, Modifier.padding(5.dp))
        }
    }

    if(showDialog) {
        AddStringDialog("Group name", groups.map{ it.name }, { showDialog = false }) { onAdd(it) }
    }
    else if(editing != -1) {
        AddStringDialog("Group name", groups.map { it.name }, { editing = -1 }, groups[editing].name) {
            onUpdate(groups[editing], it)
        }
    }
    else if(deleting != -1) {
        ConfirmDeleteDialog(
            "a group",
            { deleting = -1 },
            { onDelete(groups[deleting]) }
        ) { Text(groups[deleting].name) }
    }
}

@Composable
fun AssignmentPanel(
    course: Course, edition: Edition, assignments: List<Assignment>,
    selected: Int, onSelect: (Int) -> Unit,
    onAdd: (AssignmentType, String) -> Unit, onUpdate: (Assignment, String) -> Unit,
    onDelete: (Assignment) -> Unit
) = Column(Modifier.padding(10.dp)) {
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(-1) }
    var editing by remember { mutableStateOf(-1) }

    val dialog: @Composable (String, List<String>, () -> Unit, String, (AssignmentType, String) -> Unit) -> Unit =
        { label, taken, onClose, current, onSave ->
            DialogWindow(
                onCloseRequest = onClose,
                state = rememberDialogState(size = DpSize(400.dp, 300.dp), position = WindowPosition(Alignment.Center))
            ) {
                var name by remember(current) { mutableStateOf(current) }
                var tab by remember { mutableStateOf(AssignmentType.Solo) }

                Surface(Modifier.fillMaxSize()) {
                    TabLayout(
                        AssignmentType.entries,
                        tab.ordinal,
                        { tab = AssignmentType.entries[it] },
                        { Text(it.name) }
                    ) {
                        Box(Modifier.fillMaxSize().padding(10.dp)) {
                            Column(Modifier.align(Alignment.Center)) {
                                OutlinedTextField(
                                    name,
                                    { name = it },
                                    Modifier.fillMaxWidth(),
                                    label = { Text(label) },
                                    isError = name in taken
                                )
                                CancelSaveRow(name.isNotBlank() && name !in taken, onClose) {
                                    onSave(tab, name)
                                    onClose()
                                }
                            }
                        }
                    }
                }
            }
        }

    Text("Assignment list (${assignments.size})", style = MaterialTheme.typography.headlineMedium)

    ListOrEmpty(
        assignments,
        { Text(
            "Course ${course.name} (edition ${edition.name})\nhas no assignments yet.",
            Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center
        ) },
        { Text("Add an assignment") },
        { showDialog = true }
    ) { idx, it ->
        SelectEditDeleteRow(
            selected == idx,
            { onSelect(idx) }, { onSelect(-1) },
            { editing = idx }, { deleting = idx }
        ) {
            Text(it.name(), Modifier.padding(5.dp))
        }
    }

    if(showDialog) {
        dialog("Assignment name", assignments.map{ it.name() }, { showDialog = false }, "", onAdd)
    }
    else if(editing != -1) {
        AddStringDialog("Assignment name", assignments.map { it.name() }, { editing = -1 }, assignments[editing].name()) {
            onUpdate(assignments[editing], it)
        }
    }
    else if(deleting != -1) {
        ConfirmDeleteDialog(
            "an assignment",
            { deleting = -1 },
            { onDelete(assignments[deleting]) }
        ) { Text(assignments[deleting].name()) }
    }
}

@Composable
fun StudentDialog(
    course: Course,
    edition: Edition,
    onClose: () -> Unit,
    availableStudents: List<Student>,
    onImport: (List<Student>) -> Unit,
    onAdd: (name: String, note: String, contact: String, addToEdition: Boolean) -> Unit
) = DialogWindow(
    onCloseRequest = onClose,
    state = rememberDialogState(size = DpSize(600.dp, 400.dp), position = WindowPosition(Alignment.Center))
) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(10.dp)) {
            var isImport by remember { mutableStateOf(false) }
            TabRow(if(isImport) 1 else 0) {
                Tab(!isImport, { isImport = false }) { Text("Add new student") }
                Tab(isImport, { isImport = true }) { Text("Add existing student") }
            }

            if(isImport) {
                if(availableStudents.isEmpty()) {
                    Box(Modifier.fillMaxSize()) {
                        Text("No students available to add to this course.", Modifier.align(Alignment.Center))
                    }
                }
                else {
                    var selected by remember { mutableStateOf(setOf<Int>()) }

                    val onClick = { idx: Int ->
                        selected = if(idx in selected) selected - idx else selected + idx
                    }

                    Text("Select students to add to ${course.name} ${edition.name}")
                    LazyColumn {
                        itemsIndexed(availableStudents) { idx, student ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { onClick(idx) },
                                tonalElevation = if (selected.contains(idx)) 5.dp else 0.dp
                            ) {
                                Row {
                                    Checkbox(selected.contains(idx), { onClick(idx) })
                                    Text(student.name, Modifier.padding(5.dp))
                                }
                            }
                        }
                    }
                    CancelSaveRow(selected.isNotEmpty(), onClose) {
                        onImport(selected.map { idx -> availableStudents[idx] })
                        onClose()
                    }
                }
            }
            else {
                Box(Modifier.fillMaxSize()) {
                    var name by remember { mutableStateOf("") }
                    var contact by remember { mutableStateOf("") }
                    var note by remember { mutableStateOf("") }
                    var add by remember { mutableStateOf(true) }

                    Column(Modifier.align(Alignment.Center)) {
                        OutlinedTextField(
                            name,
                            { name = it },
                            Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Student name") })
                        OutlinedTextField(
                            contact,
                            { contact = it },
                            Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Student contact") })
                        OutlinedTextField(
                            note,
                            { note = it },
                            Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 3,
                            label = { Text("Note") })
                        Row {
                            Checkbox(add, { add = it })
                            Text(
                                "Add student to ${course.name} ${edition.name}?",
                                Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        CancelSaveRow(name.isNotBlank() && contact.isNotBlank(), onClose) {
                            onAdd(name, note, contact, add)
                            onClose()
                        }
                    }
                }
            }
        }
    }
}