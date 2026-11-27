package com.example.studentperfomanceapp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subject(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val studentId: Int,

    val subjectName: String,

    val mark: Int
)