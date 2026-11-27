package com.example.studentperfomanceapp

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Navy = Color(0xFF0B3558)
private val Teal = Color(0xFF0E8F83)
private val Gold = Color(0xFFF5B301)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    students: List<Student>,
    dao: StudentDao,
    onBack: () -> Unit
) {
    var selectedForm by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var rankings by remember { mutableStateOf<List<StudentRanking>>(emptyList()) }
    var selectedStudentForReport by remember { mutableStateOf<StudentRanking?>(null) }
    val context = LocalContext.current

    LaunchedEffect(students) {
        rankings = calculateClassRanking(students, dao)
    }

    val forms = listOf("All", "Form 1", "Form 2", "Form 3", "Form 4")

    val filteredRankings = rankings.filter { r ->
        (selectedForm == "All" || r.student.className == selectedForm) &&
                (searchQuery.isBlank() || r.student.name.contains(searchQuery, ignoreCase = true))
    }

    val totalStudents = filteredRankings.size
    val classAvg = if (filteredRankings.isNotEmpty()) {
        filteredRankings.map { it.average }.filter { it > 0 }.let { avgs ->
            if (avgs.isNotEmpty()) avgs.average() else 0.0
        }
    } else 0.0

    val passCount = filteredRankings.count { it.average >= 50.0 }
    val passRate = if (totalStudents > 0) (passCount * 100.0 / totalStudents) else 0.0

    ScreenFrame(
        header = {
            AppHeader(
                title = "Academic Reports",
                subtitle = "Comprehensive student performance reports",
                onIconClick = onBack
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportSummaryCard(
                    title = "Students",
                    value = totalStudents.toString(),
                    icon = Icons.Rounded.Groups,
                    color = Teal,
                    modifier = Modifier.weight(1f)
                )
                ReportSummaryCard(
                    title = "Class Avg",
                    value = if (classAvg > 0) String.format("%.1f%%", classAvg) else "—",
                    icon = Icons.Rounded.Analytics,
                    color = Navy,
                    modifier = Modifier.weight(1f)
                )
                ReportSummaryCard(
                    title = "Pass Rate",
                    value = if (totalStudents > 0) String.format("%.0f%%", passRate) else "—",
                    icon = Icons.Rounded.CheckCircle,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search student by name...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Form Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                forms.forEach { form ->
                    FilterChip(
                        selected = selectedForm == form,
                        onClick = { selectedForm = form },
                        label = { Text(form, fontSize = 12.sp) }
                    )
                }
            }

            // Export All Button Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reports (${filteredRankings.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Generated academic summary report for $totalStudents students",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export PDF",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Export Report", fontSize = 12.sp)
                }
            }

            // Student Report List
            if (filteredRankings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No students match '$searchQuery'" else "No students found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRankings) { ranking ->
                        StudentReportCard(
                            ranking = ranking,
                            onClick = { selectedStudentForReport = ranking }
                        )
                    }
                }
            }
        }
    }

    // Report Detail Dialog
    selectedStudentForReport?.let { ranking ->
        StudentReportDetailDialog(
            ranking = ranking,
            dao = dao,
            onDismiss = { selectedStudentForReport = null }
        )
    }
}

@Composable
private fun ReportSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StudentReportCard(
    ranking: StudentRanking,
    onClick: () -> Unit
) {
    val student = ranking.student
    val grade = calculateGrade(ranking.average)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Teal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Teal,
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${student.className} • ${ranking.subjectCount} Subject(s)" +
                                (ranking.position?.let { " • Rank: ${positionText(it)}" } ?: ""),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (grade) {
                        "A" -> Color(0xFF2E7D32)
                        "B" -> Teal
                        "C" -> Gold
                        "D" -> Color(0xFFE65100)
                        else -> Color(0xFFC62828)
                    }.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (ranking.subjectCount > 0) String.format("%.1f%% ($grade)", ranking.average) else "No Marks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (grade) {
                            "A" -> Color(0xFF2E7D32)
                            "B" -> Teal
                            "C" -> Gold
                            "D" -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentReportDetailDialog(
    ranking: StudentRanking,
    dao: StudentDao,
    onDismiss: () -> Unit
) {
    val student = ranking.student
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(student.id) {
        scope.launch {
            subjects = dao.getSubjectsForStudent(student.id).first()
        }
    }

    val overallGrade = calculateGrade(ranking.average)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Official Academic Report",
                    fontSize = 14.sp,
                    color = Teal,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = student.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${student.className} • Rank: ${ranking.position?.let { positionText(it) } ?: "N/A"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Box
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Overall Avg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.1f%%", ranking.average), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Teal)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Grade", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(overallGrade, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Subjects", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(subjects.size.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = getEncouragement(ranking.average),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text(
                    text = "Subject Marks & Grades",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (subjects.isEmpty()) {
                    Text(
                        text = "No subjects registered for this student.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    subjects.forEach { subject ->
                        val subGrade = calculateGrade(subject.mark.toDouble())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(subject.subjectName, fontSize = 14.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${subject.mark}%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Teal.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = subGrade,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Teal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "Report Card for ${student.name} saved/printed!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            ) {
                Icon(Icons.Rounded.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Print Report Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
