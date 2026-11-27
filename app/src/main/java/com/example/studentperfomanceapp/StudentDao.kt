package com.example.studentperfomanceapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    // ==============================
    // STUDENTS
    // ==============================

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)


    // ==============================
    // SUBJECTS
    // ==============================

    @Query(
        "SELECT * FROM subjects " +
                "WHERE studentId = :studentId " +
                "ORDER BY subjectName ASC"
    )
    fun getSubjectsForStudent(studentId: Int): Flow<List<Subject>>

    @Insert
    suspend fun insertSubject(subject: Subject)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)
}