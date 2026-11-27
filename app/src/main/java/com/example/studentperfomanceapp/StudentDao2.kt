package com.example.studentperfomanceapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentMarkDao {

    @Insert
    suspend fun insertMark(studentMark: StudentMark)

    @Query("SELECT * FROM student_marks WHERE studentId = :studentId ORDER BY subject ASC")
    fun getMarksForStudent(studentId: Int): Flow<List<StudentMark>>

    // Gets all marks from all students.
    // We use this for calculating class rankings.
    @Query("SELECT * FROM student_marks")
    fun getAllMarks(): Flow<List<StudentMark>>

    @Delete
    suspend fun deleteMark(studentMark: StudentMark)
}