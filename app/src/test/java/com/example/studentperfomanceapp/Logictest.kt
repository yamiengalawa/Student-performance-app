package com.example.studentperfomanceapp

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LogicTest {

    // ---------- Grades ----------
    @Test fun grade_boundaries() {
        assertEquals("A", calculateGrade(80.0))
        assertEquals("A", calculateGrade(100.0))
        assertEquals("B", calculateGrade(79.9))
        assertEquals("B", calculateGrade(70.0))
        assertEquals("C", calculateGrade(60.0))
        assertEquals("D", calculateGrade(50.0))
        assertEquals("E", calculateGrade(49.9))
        assertEquals("E", calculateGrade(0.0))
    }

    @Test fun encouragement_matches_range() {
        assertTrue(getEncouragement(85.0).startsWith("Excellent"))
        assertTrue(getEncouragement(72.0).startsWith("Very good"))
        assertTrue(getEncouragement(61.0).startsWith("Good progress"))
        assertTrue(getEncouragement(55.0).startsWith("Keep improving"))
        assertTrue(getEncouragement(10.0).startsWith("Keep trying"))
    }

    // ---------- Position text ----------
    @Test fun position_text_suffixes() {
        assertEquals("1st", positionText(1))
        assertEquals("2nd", positionText(2))
        assertEquals("3rd", positionText(3))
        assertEquals("4th", positionText(4))
        assertEquals("11th", positionText(11))
        assertEquals("12th", positionText(12))
        assertEquals("13th", positionText(13))
        assertEquals("21st", positionText(21))
        assertEquals("112th", positionText(112))
    }

    // ---------- Validation ----------
    @Test fun validate_accepts_good_rows_and_skips_empty() {
        val (valid, error) = validateEntries(
            listOf(SubjectEntry("Mathematics", "80"), SubjectEntry("", "")),
            emptySet()
        )
        assertEquals("", error)
        assertEquals(listOf("Mathematics" to 80), valid)
    }

    @Test fun validate_rejects_mark_over_100() {
        val (_, error) = validateEntries(listOf(SubjectEntry("English", "101")), emptySet())
        assertTrue(error.contains("between 0 and 100"))
    }

    @Test fun validate_rejects_missing_subject() {
        val (_, error) = validateEntries(listOf(SubjectEntry("", "50")), emptySet())
        assertTrue(error.contains("choose a subject"))
    }

    @Test fun validate_rejects_missing_mark() {
        val (_, error) = validateEntries(listOf(SubjectEntry("Biology", "")), emptySet())
        assertTrue(error.contains("enter a mark"))
    }

    @Test fun validate_rejects_duplicates_in_same_list() {
        val (_, error) = validateEntries(
            listOf(SubjectEntry("Physics", "60"), SubjectEntry("physics", "70")), emptySet()
        )
        assertTrue(error.contains("already added"))
    }

    @Test fun validate_rejects_subject_already_taken() {
        val (_, error) = validateEntries(listOf(SubjectEntry("Chemistry", "60")), setOf("chemistry"))
        assertTrue(error.contains("already added"))
    }

    // ---------- Ranking ----------
    private fun subj(id: Int, studentId: Int, mark: Int) =
        Subject(id = id, studentId = studentId, subjectName = "S$id", mark = mark)

    @Test fun ranking_orders_students_by_average() = runTest {
        val a = Student(id = 1, name = "Alice", className = "Form 1")
        val b = Student(id = 2, name = "Bob", className = "Form 1")
        val c = Student(id = 3, name = "Chimwemwe", className = "Form 1")

        val dao = mockk<StudentDao>()
        every { dao.getSubjectsForStudent(1) } returns flowOf(listOf(subj(1, 1, 90), subj(2, 1, 70))) // avg 80
        every { dao.getSubjectsForStudent(2) } returns flowOf(listOf(subj(3, 2, 60)))                 // avg 60
        every { dao.getSubjectsForStudent(3) } returns flowOf(listOf(subj(4, 3, 95)))                 // avg 95

        val result = calculateClassRanking(listOf(a, b, c), dao)

        assertEquals(1, result.first { it.student.id == 3 }.position)
        assertEquals(2, result.first { it.student.id == 1 }.position)
        assertEquals(3, result.first { it.student.id == 2 }.position)
        assertEquals(80.0, result.first { it.student.id == 1 }.average, 0.001)
    }

    @Test fun ranking_ties_share_position() = runTest {
        val a = Student(id = 1, name = "A", className = "Form 2")
        val b = Student(id = 2, name = "B", className = "Form 2")
        val c = Student(id = 3, name = "C", className = "Form 2")
        val dao = mockk<StudentDao>()
        every { dao.getSubjectsForStudent(1) } returns flowOf(listOf(subj(1, 1, 80)))
        every { dao.getSubjectsForStudent(2) } returns flowOf(listOf(subj(2, 2, 80)))
        every { dao.getSubjectsForStudent(3) } returns flowOf(listOf(subj(3, 3, 50)))

        val result = calculateClassRanking(listOf(a, b, c), dao)

        assertEquals(1, result.first { it.student.id == 1 }.position)
        assertEquals(1, result.first { it.student.id == 2 }.position)
        assertEquals(3, result.first { it.student.id == 3 }.position)
    }

    @Test fun student_without_marks_has_no_position() = runTest {
        val a = Student(id = 1, name = "A", className = "Form 3")
        val dao = mockk<StudentDao>()
        every { dao.getSubjectsForStudent(1) } returns flowOf(emptyList())

        val result = calculateClassRanking(listOf(a), dao)

        assertNull(result[0].position)
        assertEquals(0, result[0].subjectCount)
    }
}