package com.jaytux.grader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.jaytux.grader.data.GroupAssignment
import com.jaytux.grader.data.GroupAssignmentCriterion
import com.jaytux.grader.data.SoloAssignmentCriterion
import com.jaytux.grader.data.Student
import com.jaytux.grader.viewmodel.GroupAssignmentState
import com.jaytux.grader.viewmodel.PeerEvaluationState
import com.jaytux.grader.viewmodel.SoloAssignmentState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import kotlinx.datetime.LocalDateTime

@Composable
fun GroupAssignmentView(state: GroupAssignmentState) {
    val task by state.task
    val deadline by state.deadline
    val allFeedback by state.feedback.entities
    val criteria by state.criteria.entities

    var idx by remember(state) { mutableStateOf(0) }

    Column(Modifier.padding(10.dp)) {
        if(allFeedback.any { it.second.feedback == null }) {
            Text("Groups in bold have no feedback yet.", fontStyle = FontStyle.Italic)
        }
        else {
            Text("All groups have feedback.", fontStyle = FontStyle.Italic)
        }

        TabRow(idx) {
            Tab(idx == 0, { idx = 0 }) { Text("Task and Criteria") }
            allFeedback.forEachIndexed { i, it ->
                val (group, feedback) = it
                Tab(idx == i + 1, { idx = i + 1 }) {
                    Text(group.name, fontWeight = feedback.feedback?.let { FontWeight.Normal } ?: FontWeight.Bold)
                }
            }
        }

        if(idx == 0) {
            groupTaskWidget(
                task, deadline, criteria,
                onSetTask = { state.updateTask(it) },
                onSetDeadline = { state.updateDeadline(it) },
                onAddCriterion = { state.addCriterion(it) },
                onModCriterion = { c, n, d -> state.updateCriterion(c, n, d) },
                onRmCriterion = { state.deleteCriterion(it) }
            )
        }
        else {
            groupFeedback(state, allFeedback[idx - 1].second)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun groupTaskWidget(
    taskMD: String,
    deadline: LocalDateTime,
    criteria: List<GroupAssignmentCriterion>,
    onSetTask: (String) -> Unit,
    onSetDeadline: (LocalDateTime) -> Unit,
    onAddCriterion: (name: String) -> Unit,
    onModCriterion: (cr: GroupAssignmentCriterion, name: String, desc: String) -> Unit,
    onRmCriterion: (cr: GroupAssignmentCriterion) -> Unit
) {
    var critIdx by remember { mutableStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }

    Row {
        Surface(Modifier.weight(0.25f), tonalElevation = 10.dp) {
            Column(Modifier.padding(10.dp)) {
                LazyColumn(Modifier.weight(1f)) {
                    item {
                        Surface(
                            Modifier.fillMaxWidth().clickable { critIdx = 0 },
                            tonalElevation = if (critIdx == 0) 50.dp else 0.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Assignment", Modifier.padding(5.dp), fontStyle = FontStyle.Italic)
                        }
                    }

                    itemsIndexed(criteria) { i, crit ->
                        Surface(
                            Modifier.fillMaxWidth().clickable { critIdx = i + 1 },
                            tonalElevation = if (critIdx == i + 1) 50.dp else 0.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(crit.name, Modifier.padding(5.dp))
                        }
                    }
                }
                Button({ adding = true }, Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()) {
                    Text("Add evaluation criterion")
                }
            }
        }
        Box(Modifier.weight(0.75f).padding(10.dp)) {
            if (critIdx == 0) {
                val updTask = rememberRichTextState()

                LaunchedEffect(taskMD) { updTask.setMarkdown(taskMD) }

                Column {
                    Row {
                        DateTimePicker(deadline, onSetDeadline)
                    }
                    RichTextStyleRow(state = updTask)
                    OutlinedRichTextEditor(
                        state = updTask,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        singleLine = false,
                        minLines = 5,
                        label = { Text("Task") }
                    )
                    CancelSaveRow(
                        true,
                        { updTask.setMarkdown(taskMD) },
                        "Reset",
                        "Update"
                    ) { onSetTask(updTask.toMarkdown()) }
                }
            }
            else {
                val crit = criteria[critIdx - 1]
                var name by remember(crit) { mutableStateOf(crit.name) }
                var desc by remember(crit) { mutableStateOf(crit.description) }

                Column {
                    Row {
                        OutlinedTextField(name, { name = it }, Modifier.weight(0.8f))
                        Spacer(Modifier.weight(0.1f))
                        Button({ onModCriterion(crit, name, desc) }, Modifier.weight(0.1f)) {
                            Text("Update")
                        }
                    }
                    OutlinedTextField(
                        desc, { desc = it }, Modifier.fillMaxWidth().weight(1f),
                        label = { Text("Description") },
                        singleLine = false,
                        minLines = 5
                    )
                    Button({ confirming = true }, Modifier.fillMaxWidth()) {
                        Text("Remove criterion")
                    }
                }
            }
        }
    }

    if(adding) {
        AddStringDialog(
            "Evaluation criterion name", criteria.map{ it.name }, { adding = false }
        ) { onAddCriterion(it) }
    }

    if(confirming && critIdx != 0) {
        ConfirmDeleteDialog(
            "an evaluation criterion",
            { confirming = false }, { onRmCriterion(criteria[critIdx - 1]); critIdx = 0 }
        ) {
            Text(criteria[critIdx - 1].name)
        }
    }
}

@Composable
fun groupFeedback(state: GroupAssignmentState, fdbk: GroupAssignmentState.LocalGFeedback) {
    val (group, feedback, individual) = fdbk
    var idx by remember(fdbk) { mutableStateOf(0) }
    var critIdx by remember(fdbk) { mutableStateOf(0) }
    val criteria by state.criteria.entities
    val suggestions by state.autofill.entities

    Row {
        Surface(Modifier.weight(0.25f), tonalElevation = 10.dp) {
            LazyColumn(Modifier.fillMaxHeight().padding(10.dp)) {
                item {
                    Surface(
                        Modifier.fillMaxWidth().clickable { idx = 0 },
                        tonalElevation = if (idx == 0) 50.dp else 0.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Group feedback", Modifier.padding(5.dp), fontStyle = FontStyle.Italic)
                    }
                }

                itemsIndexed(individual.toList()) { i, (student, details) ->
                    val (role, _) = details
                    Surface(
                        Modifier.fillMaxWidth().clickable { idx = i + 1 },
                        tonalElevation = if (idx == i + 1) 50.dp else 0.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("${student.name} (${role ?: "no role"})", Modifier.padding(5.dp))
                    }
                }
            }
        }

        val updateGrade = { grade: String ->
            if(idx == 0) {
                state.upsertGroupFeedback(group, feedback.global?.feedback ?: "", grade)
            }
            else {
                val ind = individual[idx - 1]
                val glob = ind.second.second.global
                state.upsertIndividualFeedback(ind.first, group, glob?.feedback ?: "", grade)
            }
        }

        val updateFeedback = { fdbk: String ->
            if(idx == 0) {
                if(critIdx == 0) {
                    state.upsertGroupFeedback(group, fdbk, feedback.global?.grade ?: "", null)
                }
                else {
                    val current = feedback.byCriterion[critIdx - 1]
                    state.upsertGroupFeedback(group, fdbk, current.entry?.grade ?: "", current.criterion)
                }
            }
            else {
                val ind = individual[idx - 1]
                if(critIdx == 0) {
                    val entry = ind.second.second
                    state.upsertIndividualFeedback(ind.first, group, fdbk, entry.global?.grade ?: "", null)
                }
                else {
                    val entry = ind.second.second.byCriterion[critIdx - 1]
                    state.upsertIndividualFeedback(ind.first, group, fdbk, entry.entry?.grade ?: "", entry.criterion)
                }
            }
        }

        groupFeedbackPane(
            criteria, critIdx, { critIdx = it }, feedback.global,
            if(critIdx == 0) feedback.global else feedback.byCriterion[critIdx - 1].entry,
            suggestions, updateGrade, updateFeedback, Modifier.weight(0.75f).padding(10.dp),
            key = idx to critIdx
        )
    }
}

@Composable
fun groupFeedbackPane(
    criteria: List<GroupAssignmentCriterion>,
    currentCriterion: Int,
    onSelectCriterion: (Int) -> Unit,
    globFeedback: GroupAssignmentState.FeedbackEntry?,
    criterionFeedback: GroupAssignmentState.FeedbackEntry?,
    autofill: List<String>,
    onSetGrade: (String) -> Unit,
    onSetFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    var grade by remember(globFeedback, key) { mutableStateOf(globFeedback?.grade ?: "") }
    var feedback by remember(currentCriterion, criteria, criterionFeedback, key) { mutableStateOf(TextFieldValue(criterionFeedback?.feedback ?: "")) }
    Column(modifier) {
        Row {
            Text("Overall grade: ", Modifier.align(Alignment.CenterVertically))
            OutlinedTextField(grade, { grade = it }, Modifier.weight(0.2f))
            Spacer(Modifier.weight(0.6f))
            Button(
                { onSetGrade(grade); onSetFeedback(feedback.text) },
                Modifier.weight(0.2f).align(Alignment.CenterVertically),
                enabled = grade.isNotBlank() || feedback.text.isNotBlank()
            ) {
                Text("Save")
            }
        }
        TabRow(currentCriterion) {
            Tab(currentCriterion == 0, { onSelectCriterion(0) }) { Text("General feedback", fontStyle = FontStyle.Italic) }
            criteria.forEachIndexed { i, c ->
                Tab(currentCriterion == i + 1, { onSelectCriterion(i + 1) }) { Text(c.name) }
            }
        }
        Spacer(Modifier.height(5.dp))
        AutocompleteLineField(
            feedback, { feedback = it }, Modifier.fillMaxWidth().weight(1f), { Text("Feedback") }
        ) { filter ->
            autofill.filter { x -> x.trim().startsWith(filter.trim()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloAssignmentView(state: SoloAssignmentState) {
    val task by state.task
    val deadline by state.deadline
    val suggestions by state.autofill.entities
    val grades by state.feedback.entities
    val criteria by state.criteria.entities

    var tab by remember(state) { mutableStateOf(0) }
    var idx by remember(state, tab) { mutableStateOf(0) }
    var critIdx by remember(state, tab, idx) { mutableStateOf(0) }
    var adding by remember(state, tab) { mutableStateOf(false) }
    var confirming by remember(state, tab) { mutableStateOf(false) }

    val updateGrade = { grade: String ->
        state.upsertFeedback(
            grades[idx].first,
            if(critIdx == 0) grades[idx].second.global?.feedback else grades[idx].second.byCriterion[critIdx - 1].second?.feedback,
            grade,
            if(critIdx == 0) null else criteria[critIdx - 1]
        )
    }

    val updateFeedback = { feedback: String ->
        state.upsertFeedback(
            grades[idx].first,
            feedback,
            if(critIdx == 0) grades[idx].second.global?.grade else grades[idx].second.byCriterion[critIdx - 1].second?.grade,
            if(critIdx == 0) null else criteria[critIdx - 1]
        )
    }

    Column(Modifier.padding(10.dp)) {
        Row {
            Surface(Modifier.weight(0.25f), tonalElevation = 10.dp) {
                Column(Modifier.padding(10.dp)) {
                    TabRow(tab) {
                        Tab(tab == 0, { tab = 0 }) { Text("Task/Criteria") }
                        Tab(tab == 1, { tab = 1 }) { Text("Students") }
                    }

                    LazyColumn(Modifier.weight(1f)) {
                        if (tab == 0) {
                            item {
                                Surface(
                                    Modifier.fillMaxWidth().clickable { idx = 0 },
                                    tonalElevation = if (idx == 0) 50.dp else 0.dp,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Assignment", Modifier.padding(5.dp), fontStyle = FontStyle.Italic)
                                }
                            }

                            itemsIndexed(criteria) { i, crit ->
                                Surface(
                                    Modifier.fillMaxWidth().clickable { idx = i + 1 },
                                    tonalElevation = if (idx == i + 1) 50.dp else 0.dp,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(crit.name, Modifier.padding(5.dp))
                                }
                            }
                        } else {
                            itemsIndexed(grades.toList()) { i, (student, _) ->
                                Surface(
                                    Modifier.fillMaxWidth().clickable { idx = i },
                                    tonalElevation = if (idx == i) 50.dp else 0.dp,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(student.name, Modifier.padding(5.dp))
                                }
                            }
                        }
                    }

                    if (tab == 0) {
                        Button({ adding = true }, Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()) {
                            Text("Add evaluation criterion")
                        }
                    }
                }
            }

            Column(Modifier.weight(0.75f).padding(10.dp)) {
                if(tab == 0) {
                    if (idx == 0) {
                        val updTask = rememberRichTextState()

                        LaunchedEffect(task) { updTask.setMarkdown(task) }

                        Row {
                            DateTimePicker(deadline, { state.updateDeadline(it) })
                        }
                        RichTextStyleRow(state = updTask)
                        OutlinedRichTextEditor(
                            state = updTask,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            singleLine = false,
                            minLines = 5,
                            label = { Text("Task") }
                        )
                        CancelSaveRow(
                            true,
                            { updTask.setMarkdown(task) },
                            "Reset",
                            "Update"
                        ) { state.updateTask(updTask.toMarkdown()) }
                    } else {
                        val crit = criteria[idx - 1]
                        var name by remember(crit) { mutableStateOf(crit.name) }
                        var desc by remember(crit) { mutableStateOf(crit.description) }

                        Column {
                            Row {
                                OutlinedTextField(name, { name = it }, Modifier.weight(0.8f))
                                Spacer(Modifier.weight(0.1f))
                                Button({ state.updateCriterion(crit, name, desc) }, Modifier.weight(0.1f)) {
                                    Text("Update")
                                }
                            }
                            OutlinedTextField(
                                desc, { desc = it }, Modifier.fillMaxWidth().weight(1f),
                                label = { Text("Description") },
                                singleLine = false,
                                minLines = 5
                            )
                            Button({ confirming = true }, Modifier.fillMaxWidth()) {
                                Text("Remove criterion")
                            }
                        }
                    }
                }
                else {
                    soloFeedbackPane(
                        criteria, critIdx, { critIdx = it }, grades[idx].second.global,
                        if(critIdx == 0) grades[idx].second.global else grades[idx].second.byCriterion[critIdx - 1].second,
                        suggestions, updateGrade, updateFeedback,
                        key = tab to idx
                    )
                }
            }
        }
    }

    if(adding) {
        AddStringDialog(
            "Evaluation criterion name", criteria.map{ it.name }, { adding = false }
        ) { state.addCriterion(it) }
    }

    if(confirming && idx != 0) {
        ConfirmDeleteDialog(
            "an evaluation criterion",
            { confirming = false }, { state.deleteCriterion(criteria[idx - 1]); idx = 0 }
        ) {
            Text(criteria[idx - 1].name)
        }
    }
}

@Composable
fun soloFeedbackPane(
    criteria: List<SoloAssignmentCriterion>,
    currentCriterion: Int,
    onSelectCriterion: (Int) -> Unit,
    globFeedback: SoloAssignmentState.LocalFeedback?,
    criterionFeedback: SoloAssignmentState.LocalFeedback?,
    autofill: List<String>,
    onSetGrade: (String) -> Unit,
    onSetFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    var grade by remember(globFeedback, key) { mutableStateOf(globFeedback?.grade ?: "") }
    var feedback by remember(currentCriterion, criteria, key) { mutableStateOf(TextFieldValue(criterionFeedback?.feedback ?: "")) }
    Column(modifier) {
        Row {
            Text("Overall grade: ", Modifier.align(Alignment.CenterVertically))
            OutlinedTextField(grade, { grade = it }, Modifier.weight(0.2f))
            Spacer(Modifier.weight(0.6f))
            Button(
                { onSetGrade(grade); onSetFeedback(feedback.text) },
                Modifier.weight(0.2f).align(Alignment.CenterVertically),
                enabled = grade.isNotBlank() || feedback.text.isNotBlank()
            ) {
                Text("Save")
            }
        }
        TabRow(currentCriterion) {
            Tab(currentCriterion == 0, { onSelectCriterion(0) }) { Text("General feedback", fontStyle = FontStyle.Italic) }
            criteria.forEachIndexed { i, c ->
                Tab(currentCriterion == i + 1, { onSelectCriterion(i + 1) }) { Text(c.name) }
            }
        }
        Spacer(Modifier.height(5.dp))
        AutocompleteLineField(
            feedback, { feedback = it }, Modifier.fillMaxWidth().weight(1f), { Text("Feedback") }
        ) { filter ->
            autofill.filter { x -> x.trim().startsWith(filter.trim()) }
        }
    }
}

@Composable
fun PeerEvaluationView(state: PeerEvaluationState) {
    val contents by state.contents.entities
    var idx by remember(state) { mutableStateOf(0) }
    var editing by remember(state) { mutableStateOf<Triple<Student, Student?, PeerEvaluationState.Student2StudentEntry?>?>(null) }
    val measure = rememberTextMeasurer()

    val isSelected = { from: Student, to: Student? ->
        editing?.let { (f, t, _) -> f == from && t == to } ?: false
    }

    Column(Modifier.padding(10.dp)) {
        TabRow(idx) {
            contents.forEachIndexed { i, it ->
                Tab(idx == i, { idx = i; editing = null }) { Text(it.group.name) }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row {
            val current = contents[idx]
            val horScroll = rememberLazyListState()
            val style = LocalTextStyle.current
            val textLenMeasured = remember(state, idx) {
                current.students.maxOf { (s, _) ->
                    measure.measure(s.name, style).size.width
                } + 10
            }
            val cellSize = 75.dp

            Column(Modifier.weight(0.5f)) {
                Row {
                    Box { FromTo(textLenMeasured.dp) }
                    LazyRow(Modifier.height(textLenMeasured.dp), state = horScroll) {
                        item { VLine() }
                        items(current.students) { (s, _) ->
                            Box(
                                Modifier.width(cellSize).height(textLenMeasured.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                var _h: Int = 0
                                Text(s.name, Modifier.layout{ m, c ->
                                    val p = m.measure(c.copy(minWidth = c.maxWidth, maxWidth = Constraints.Infinity))
                                    _h = p.height
                                    layout(p.height, p.width) { p.place(0, 0) }
                                }.graphicsLayer {
                                    rotationZ = -90f
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    translationX = _h.toFloat() / 2f
                                    translationY = textLenMeasured.dp.value - 15f
                                })
                            }
                        }
                        item { VLine() }
                        item {
                            Box(
                                Modifier.width(cellSize).height(textLenMeasured.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                var _h: Int = 0
                                Text("Group Rating", Modifier.layout{ m, c ->
                                    val p = m.measure(c.copy(minWidth = c.maxWidth, maxWidth = Constraints.Infinity))
                                    _h = p.height
                                    layout(p.height, p.width) { p.place(0, 0) }
                                }.graphicsLayer {
                                    rotationZ = -90f
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    translationX = _h.toFloat() / 2f
                                    translationY = textLenMeasured.dp.value - 15f
                                }, fontWeight = FontWeight.Bold)
                            }
                        }
                        item { VLine() }
                    }
                }
                MeasuredLazyColumn(key = idx) {
                    measuredItem { HLine() }
                    items(current.students) { (from, glob, map) ->
                        Row(Modifier.height(cellSize)) {
                            Text(from.name, Modifier.width(textLenMeasured.dp).align(Alignment.CenterVertically))
                            LazyRow(state = horScroll) {
                                item { VLine() }
                                items(map) { (to, entry) ->
                                    PEGradeWidget(entry,
                                        { editing = Triple(from, to, entry) }, { editing = null },
                                        isSelected(from, to), Modifier.size(cellSize, cellSize)
                                    )
                                }
                                item { VLine() }
                                item {
                                    PEGradeWidget(glob,
                                        { editing = Triple(from, null, glob) }, { editing = null },
                                        isSelected(from, null), Modifier.size(cellSize, cellSize))
                                }
                                item { VLine() }
                            }
                        }
                    }
                    measuredItem { HLine() }
                }
            }

            Column(Modifier.weight(0.5f)) {
                var groupLevel by remember(state, idx) { mutableStateOf(contents[idx].content) }
                editing?.let {
                    Column(Modifier.weight(0.5f)) {
                        val (from, to, data) = it

                        var sGrade by remember(editing) { mutableStateOf(data?.grade ?: "") }
                        var sMsg by remember(editing) { mutableStateOf(data?.feedback ?: "") }

                        Box(Modifier.padding(5.dp)) {
                            to?.let { s2 ->
                                if(from == s2)
                                    Text("Self-evaluation by ${from.name}", fontWeight = FontWeight.Bold)
                                else
                                    Text("Evaluation of ${s2.name} by ${from.name}", fontWeight = FontWeight.Bold)
                            } ?: Text("Group-level evaluation by ${from.name}", fontWeight = FontWeight.Bold)
                        }

                        Row {
                            Text("Grade: ", Modifier.align(Alignment.CenterVertically))
                            OutlinedTextField(sGrade, { sGrade = it }, Modifier.weight(0.2f))
                            Spacer(Modifier.weight(0.6f))
                            Button(
                                { state.upsertIndividualFeedback(from, to, sGrade, sMsg); editing = null },
                                Modifier.weight(0.2f).align(Alignment.CenterVertically),
                                enabled = sGrade.isNotBlank() || sMsg.isNotBlank()
                            ) {
                                Text("Save")
                            }
                        }

                        OutlinedTextField(
                            sMsg, { sMsg = it }, Modifier.fillMaxWidth().weight(1f),
                            label = { Text("Feedback") },
                            singleLine = false,
                            minLines = 5
                        )
                    }
                }

                Column(Modifier.weight(0.5f)) {
                    Row {
                        Text("Group-level notes", Modifier.weight(1f).align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
                        Button(
                            { state.upsertGroupFeedback(current.group, groupLevel); editing = null },
                            enabled = groupLevel != contents[idx].content
                        ) { Text("Update") }
                    }

                    OutlinedTextField(
                        groupLevel, { groupLevel = it }, Modifier.fillMaxWidth().weight(1f),
                        label = { Text("Group-level notes") },
                        singleLine = false,
                        minLines = 5
                    )
                }
            }
        }
    }
}