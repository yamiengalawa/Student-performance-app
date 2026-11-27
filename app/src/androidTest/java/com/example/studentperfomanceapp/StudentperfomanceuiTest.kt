package com.example.studentperfomanceapp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * One test for each test case in the submitted test design.
 * Each test starts with an empty database, so DO NOT run these on data you want to keep.
 */
class StudentPerformanceUiTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var dao: StudentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.getDatabase(context)
        dao = db.studentDao()
        clearData()
    }

    @After
    fun tearDown() = clearData()

    // ---------- helpers ----------

    private fun clearData() = runBlocking(Dispatchers.IO) { db.clearAllTables() }

    /** Saves a student (and optional marks) straight into the database. */
    private fun seed(name: String, form: String, marks: List<Pair<String, Int>> = emptyList()): Student =
        runBlocking(Dispatchers.IO) {
            dao.insertStudent(Student(name = name, className = form))
            val saved = dao.getAllStudents().first()
                .filter { it.name == name && it.className == form }
                .maxByOrNull { it.id }!!
            marks.forEach { (subject, mark) ->
                dao.insertSubject(Subject(studentId = saved.id, subjectName = subject, mark = mark))
            }
            saved
        }

    private fun allStudents(): List<Student> = runBlocking(Dispatchers.IO) { dao.getAllStudents().first() }

    private fun subjectsOf(student: Student): List<Subject> =
        runBlocking(Dispatchers.IO) { dao.getSubjectsForStudent(student.id).first() }

    private fun waitForText(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun openPerformanceScreen(
        student: Student,
        onUpdated: (Student) -> Unit = {},
        onDeleted: () -> Unit = {}
    ) {
        rule.setContent {
            StudentPerformanceScreen(
                student = student,
                onBack = {},
                onStudentUpdated = onUpdated,
                onStudentDeleted = onDeleted
            )
        }
    }

    // =====================================================
    // ADD STUDENT   (SPA_ADDSTU_001, SPA_ADDSTU_002)
    // =====================================================

    @Test
    fun addStudent_withSubjectsAndMarks_isSaved() {
        var added = false
        rule.setContent { AddStudentScreen(onBack = {}, onStudentAdded = { added = true }) }

        rule.onNodeWithText("Enter student name").performTextInput("Chikondi Banda")
        rule.onNodeWithText("Select form").performClick()
        rule.onNodeWithText("Form 1").performClick()

        rule.onNodeWithText("Subject").performClick()
        rule.onNodeWithText("Mathematics").performClick()
        rule.onAllNodesWithText("Mark").onFirst().performTextInput("85")

        rule.onNodeWithText("Add Subject").performClick()
        rule.onNodeWithText("Subject").performClick()
        rule.onNodeWithText("English").performClick()
        rule.onAllNodesWithText("Mark").onFirst().performTextInput("75")

        rule.onNodeWithText("Save Student").performScrollTo().performClick()
        rule.waitUntil(5_000) { added }

        val students = allStudents()
        assertEquals(1, students.size)
        assertEquals("Chikondi Banda", students[0].name)
        assertEquals("Form 1", students[0].className)
        assertEquals(2, subjectsOf(students[0]).size)
    }

    @Test
    fun addStudent_withEmptyName_showsError() {
        rule.setContent { AddStudentScreen(onBack = {}, onStudentAdded = {}) }

        rule.onNodeWithText("Select form").performClick()
        rule.onNodeWithText("Form 1").performClick()
        rule.onNodeWithText("Save Student").performScrollTo().performClick()

        rule.onNodeWithText("Please enter the student's full name.").assertExists()
        assertTrue(allStudents().isEmpty())
    }

    // =====================================================
    // MARKS   (SPA_MARKS_001)
    // =====================================================

    @Test
    fun addStudent_markAbove100_showsError() {
        rule.setContent { AddStudentScreen(onBack = {}, onStudentAdded = {}) }

        rule.onNodeWithText("Enter student name").performTextInput("Hope Chimombo")
        rule.onNodeWithText("Select form").performClick()
        rule.onNodeWithText("Form 1").performClick()
        rule.onNodeWithText("Subject").performClick()
        rule.onNodeWithText("Mathematics").performClick()
        rule.onAllNodesWithText("Mark").onFirst().performTextInput("101")
        rule.onNodeWithText("Save Student").performScrollTo().performClick()

        rule.onNodeWithText("The mark for Mathematics must be between 0 and 100.").assertExists()
        assertTrue(allStudents().isEmpty())
    }

    // =====================================================
    // EDIT A SUBJECT MARK   (SPA_SUBJ_001)
    // =====================================================

    @Test
    fun editSubjectMark_updatesAverage() {
        val student = seed("Chikondi Banda", "Form 1", listOf("Mathematics" to 85, "English" to 75))
        openPerformanceScreen(student)
        waitForText("Mathematics")

        // Edit the first subject row, whichever subject it is
        rule.onAllNodesWithContentDescription("Edit")[0].performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("95")
        rule.onNodeWithText("Save changes").performClick()

        // The new mark must be saved in the database
        rule.waitUntil(5_000) { subjectsOf(student).any { it.mark == 95 } }

        // The average on screen must match the saved marks
        val expected = String.format(
            java.util.Locale.getDefault(), "%.0f%%",
            subjectsOf(student).map { it.mark }.average()
        )
        waitForText(expected)
        rule.onNodeWithText(expected).assertExists()
    }
    // =====================================================
    // EDIT STUDENT   (SPA_EDITSTU_001, SPA_EDITSTU_002)
    // =====================================================

    @Test
    fun editStudent_changesNameAndForm() {
        val student = seed("Chikondi Banda", "Form 1", listOf("Mathematics" to 85))
        var updated: Student? = null
        openPerformanceScreen(student, onUpdated = { updated = it })
        waitForText("Mathematics")

        rule.onNodeWithText("Edit Student").performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("Chikondi Mbewe")
        rule.onNodeWithText("Form 1").performClick()
        rule.onNodeWithText("Form 2").performClick()
        rule.onNodeWithText("Save Changes").performClick()

        rule.waitUntil(5_000) { updated != null }
        assertEquals("Chikondi Mbewe", updated!!.name)
        assertEquals("Form 2", updated!!.className)
        assertEquals("Chikondi Mbewe", allStudents()[0].name)
    }

    @Test
    fun editStudent_emptyName_showsError() {
        val student = seed("Chikondi Banda", "Form 1", listOf("Mathematics" to 85))
        openPerformanceScreen(student)
        waitForText("Mathematics")

        rule.onNodeWithText("Edit Student").performScrollTo().performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("")
        rule.onNodeWithText("Save Changes").performClick()

        rule.onNodeWithText("Student name cannot be empty.").assertExists()
        assertEquals("Chikondi Banda", allStudents()[0].name)
    }

    // =====================================================
    // DELETE STUDENT   (SPA_DELSTU_001, SPA_DELSTU_002)
    // =====================================================

    @Test
    fun deleteStudent_confirm_removesStudent() {
        val student = seed("Chikondi Banda", "Form 1", listOf("Mathematics" to 85))
        var deleted = false
        openPerformanceScreen(student, onDeleted = { deleted = true })
        waitForText("Mathematics")

        rule.onNodeWithText("Delete Student").performScrollTo().performClick()
        rule.onNodeWithText("Delete").performClick()

        rule.waitUntil(5_000) { deleted }
        assertTrue(allStudents().isEmpty())
    }

    @Test
    fun deleteStudent_cancel_keepsStudent() {
        val student = seed("Chikondi Banda", "Form 1", listOf("Mathematics" to 85))
        var deleted = false
        openPerformanceScreen(student, onDeleted = { deleted = true })
        waitForText("Mathematics")

        rule.onNodeWithText("Delete Student").performScrollTo().performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()

        assertFalse(deleted)
        assertEquals(1, allStudents().size)
    }

    // =====================================================
    // AVERAGE   (SPA_CALC_001, SPA_CALC_002)
    // =====================================================

    @Test
    fun average_ofSeveralSubjects_isCorrect() {
        val student = seed(
            "Mercy Chirwa", "Form 1",
            listOf("Mathematics" to 90, "English" to 70, "Biology" to 80)
        )
        openPerformanceScreen(student)

        // (90 + 70 + 80) / 3 = 80
        waitForText("80%")
        rule.onNodeWithText("80%").assertExists()
    }

    @Test
    fun studentWithNoMarks_showsEmptyMessage() {
        val student = seed("Mphatso Phiri", "Form 2")
        openPerformanceScreen(student)

        waitForText("No marks recorded yet. Tap “Add Subjects” below.")
        rule.onNodeWithText("No marks recorded yet. Tap “Add Subjects” below.").assertExists()
    }

    // =====================================================
    // RANKING   (SPA_RANK_001)
    // =====================================================

    @Test
    fun ranking_listsStudentsFromHighestToLowestAverage() {
        seed("Grace Phiri", "Form 1", listOf("Mathematics" to 70))
        seed("Peter Banda", "Form 1", listOf("Mathematics" to 90))
        seed("Joseph Mwale", "Form 1", listOf("Mathematics" to 80))

        val rankings = runBlocking(Dispatchers.IO) { calculateClassRanking(allStudents(), dao) }
        rule.setContent {
            PerformanceRankingScreen(
                rankings = rankings,
                formCounts = mapOf("Form 1" to 3),
                formAverages = mapOf("Form 1" to 80.0),
                onBack = {},
                onStudentSelected = {},
                onFormSelected = {}
            )
        }

        fun top(name: String) = rule.onNodeWithText(name).fetchSemanticsNode().positionInRoot.y
        assertTrue(top("Peter Banda") < top("Joseph Mwale"))
        assertTrue(top("Joseph Mwale") < top("Grace Phiri"))
    }

    // =====================================================
    // SEARCH   (SPA_SRCH_001, SPA_SRCH_002)
    // =====================================================

    private fun openFormList() {
        seed("Chikondi Banda", "Form 1")
        seed("Peter Banda", "Form 1")
        seed("Grace Phiri", "Form 1")
        val students = allStudents()
        rule.setContent {
            ClassStudentsScreen(
                className = "Form 1",
                students = students,
                onBack = {},
                onStudentSelected = {}
            )
        }
        waitForText("Grace Phiri")
    }

    @Test
    fun search_byPartOfName_showsOnlyMatches() {
        openFormList()

        rule.onNode(hasSetTextAction()).performTextInput("Banda")

        rule.waitUntil(5_000) { rule.onAllNodesWithText("Grace Phiri").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Chikondi Banda").assertExists()
        rule.onNodeWithText("Peter Banda").assertExists()
        rule.onNodeWithText("Grace Phiri").assertDoesNotExist()
    }

    @Test
    fun search_noMatch_showsNoStudents() {
        openFormList()

        rule.onNode(hasSetTextAction()).performTextInput("Zzz")

        rule.waitUntil(5_000) { rule.onAllNodesWithText("Grace Phiri").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Chikondi Banda").assertDoesNotExist()
        rule.onNodeWithText("Peter Banda").assertDoesNotExist()
        rule.onNodeWithText("Grace Phiri").assertDoesNotExist()
    }
}