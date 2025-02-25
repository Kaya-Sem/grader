package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.jaytux.grader.data.*
import com.jaytux.grader.viewmodel.EditionState
import com.jaytux.grader.viewmodel.GroupAssignmentState
import com.jaytux.grader.viewmodel.GroupState
import com.jaytux.grader.viewmodel.StudentState

enum class Panel { Student, Group, Solo, GroupAs }
data class Current(val p: Panel, val i: Int)
fun Current?.studentIdx() = this?.let { if(p == Panel.Student) i else null }
fun Current?.groupIdx() = this?.let { if(p == Panel.Group) i else null }
fun Current?.soloIdx() = this?.let { if(p == Panel.Solo) i else null }
fun Current?.groupAsIdx() = this?.let { if(p == Panel.GroupAs) i else null }

@Composable
fun EditionView(state: EditionState) = Row(Modifier.padding(0.dp)) {
    var isGroup by remember { mutableStateOf(false) }
    var idx by remember { mutableStateOf<Current?>(null) }

    val students by state.students.entities
    val groups by state.groups.entities
    val solo by state.solo.entities
    val groupAs by state.groupAs.entities
    val available by state.availableStudents.entities

    val toggle = { i: Int, p: Panel ->
        idx = if(idx?.p == p && idx?.i == i) null else Current(p, i)
    }


    Surface(Modifier.weight(0.25f), tonalElevation = 5.dp) {
        TabLayout(
            listOf("Students", "Groups"),
            if (isGroup) 1 else 0,
            { isGroup = it == 1 },
            { Text(it) }
        ) {
            Column(Modifier.fillMaxSize()) {
                if (isGroup) {
                    Box(Modifier.weight(0.5f)) {
                        GroupsWidget(
                            state.course,
                            state.edition,
                            groups,
                            idx.groupIdx(),
                            { toggle(it, Panel.Group) },
                            { state.newGroup(it) }) { group, name ->
                            state.setGroupName(group, name)
                        }
                    }
                    Box(Modifier.weight(0.5f)) {
                        GroupAssignmentsWidget(
                            state.course, state.edition, groupAs, idx.groupAsIdx(), { toggle(it, Panel.GroupAs) },
                            { state.newGroupAssignment(it) }) { assignment, title ->
                            state.setGroupAssignmentTitle(
                                assignment,
                                title
                            )
                        }
                    }
                } else {
                    Box(Modifier.weight(0.5f)) {
                        StudentsWidget(
                            state.course, state.edition, students, idx.studentIdx(), { toggle(it, Panel.Student) },
                            available, { state.addToCourse(it) }
                        ) { name, note, contact, addToEdition ->
                            state.newStudent(name, contact, note, addToEdition)
                        }
                    }
                    Box(Modifier.weight(0.5f)) {
                        AssignmentsWidget(
                            state.course,
                            state.edition,
                            solo,
                            idx.soloIdx(),
                            { toggle(it, Panel.Solo) },
                            { state.newSoloAssignment(it) }) { assignment, title ->
                            state.setSoloAssignmentTitle(assignment, title)
                        }
                    }
                }
            }
        }
    }
    Box(Modifier.weight(0.75f)) {
        idx?.let { i ->
            when(i.p) {
                Panel.Student -> StudentView(StudentState(students[i.i], state.edition))
                Panel.Group -> GroupView(GroupState(groups[i.i]))
                Panel.GroupAs -> GroupAssignmentView(GroupAssignmentState(groupAs[i.i]))
                else -> {}
            }
        }
    }
}

@Composable
fun <T> EditionSideWidget(
    course: Course, edition: Edition, header: String, hasNoX: String, addX: String,
    data: List<T>, selected: Int?, onSelect: (Int) -> Unit,
    singleWidget: @Composable (T) -> Unit,
    editDialog: @Composable ((current: T, onExit: () -> Unit) -> Unit)? = null,
    dialog: @Composable (onExit: () -> Unit) -> Unit
) = Column(Modifier.padding(10.dp)) {
    Text(header, style = MaterialTheme.typography.headlineMedium)
    var showDialog by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<T?>(null) }

    ListOrEmpty(
        data,
        { Text("Course ${course.name} (edition ${edition.name})\nhas no $hasNoX yet.", Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center) },
        { Text("Add $addX") },
        { showDialog = true }
    ) { idx, it ->
        Surface(
            Modifier.fillMaxWidth().clickable { onSelect(idx) },
            tonalElevation = if (selected == idx) 50.dp else 0.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Row {
                Box(Modifier.weight(1f).align(Alignment.CenterVertically)) { singleWidget(it) }
                editDialog?.let { _ ->
                    IconButton({ current = it }, Modifier.align(Alignment.CenterVertically)) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                }
            }
        }
    }

    if(showDialog) dialog { showDialog = false }
    editDialog?.let { d ->
        current?.let { c ->
            d(c) { current = null }
        }
    }
}

@Composable
fun StudentsWidget(
    course: Course, edition: Edition, students: List<Student>, selected: Int?, onSelect: (Int) -> Unit,
    availableStudents: List<Student>, onImport: (List<Student>) -> Unit,
    onAdd: (name: String, note: String, contact: String, addToEdition: Boolean) -> Unit
) = EditionSideWidget(
    course, edition, "Student list (${students.size})", "students", "a student", students, selected, onSelect,
    { Text(it.name, Modifier.padding(5.dp)) }
) { onExit ->
    StudentDialog(course, edition, onExit, availableStudents, onImport, onAdd)
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

@Composable
fun GroupsWidget(
    course: Course, edition: Edition, groups: List<Group>, selected: Int?, onSelect: (Int) -> Unit,
    onAdd: (name: String) -> Unit, onUpdate: (Group, String) -> Unit
) = EditionSideWidget(
    course, edition, "Group list (${groups.size})", "groups", "a group", groups, selected, onSelect,
    { Text(it.name, Modifier.padding(5.dp)) },
    { current, onExit -> AddStringDialog("Group name", groups.map { it.name }, onExit, current.name) { onUpdate(current, it) } }
) { onExit ->
    AddStringDialog("Group name", groups.map { it.name }, onExit) { onAdd(it) }
}

@Composable
fun AssignmentsWidget(
    course: Course, edition: Edition, assignments: List<SoloAssignment>, selected: Int?,
    onSelect: (Int) -> Unit, onAdd: (name: String) -> Unit, onUpdate: (SoloAssignment, String) -> Unit
) = EditionSideWidget(
    course, edition, "Assignment list", "assignments", "an assignment", assignments, selected, onSelect,
    { Text(it.name, Modifier.padding(5.dp)) },
    { current, onExit -> AddStringDialog("Assignment title", assignments.map { it.name }, onExit, current.name) { onUpdate(current, it) } }
) { onExit ->
    AddStringDialog("Assignment title", assignments.map { it.name }, onExit) { onAdd(it) }
}

@Composable
fun GroupAssignmentsWidget(
    course: Course, edition: Edition, assignments: List<GroupAssignment>, selected: Int?,
    onSelect: (Int) -> Unit, onAdd: (name: String) -> Unit, onUpdate: (GroupAssignment, String) -> Unit
) = EditionSideWidget(
    course, edition, "Group assignment list", "group assignments", "an assignment", assignments, selected, onSelect,
    { Text(it.name, Modifier.padding(5.dp)) },
    { current, onExit -> AddStringDialog("Assignment title", assignments.map { it.name }, onExit, current.name) { onUpdate(current, it) } }
) { onExit ->
    AddStringDialog("Assignment title", assignments.map { it.name }, onExit) { onAdd(it) }
}