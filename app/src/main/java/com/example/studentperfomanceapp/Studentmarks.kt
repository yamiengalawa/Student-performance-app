package com.example.studentperfomanceapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_marks")
data class StudentMark(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: Int,
    val subject: String,
    val mark: Int
)