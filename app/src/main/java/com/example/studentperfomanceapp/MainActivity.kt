package com.example.studentperfomanceapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val APP_PREFERENCES = "student_performance_preferences"

// ---------- Palette taken from the mockup ----------
private val Navy = Color(0xFF0B3558)
private val NavyDeep = Color(0xFF082A47)
private val Teal = Color(0xFF0E8F83)
private val Gold = Color(0xFFF5B301)
private val Silver = Color(0xFFB8C2CC)
private val Bronze = Color(0xFFCD7F32)

private val FORMS = listOf("Form 1", "Form 2", "Form 3", "Form 4")

private val COMMON_SUBJECTS = listOf(
    "Mathematics", "English", "Chichewa", "Biology", "Physics", "Chemistry",
    "Geography", "History", "Computer Studies", "Life Skills", "Bible Knowledge",
    "Agriculture", "Business Studies", "Social Studies", "Additional Mathematics",
    "French", "Home Economics"
)

private val TeacherLightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAF8),
    onPrimaryContainer = Color(0xFF102D4A),
    secondary = Color(0xFF147D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F1EB),
    onSecondaryContainer = Color(0xFF104B44),
    tertiary = Color(0xFF9A6500),
    background = Color(0xFFF3F7FB),
    onBackground = Color(0xFF182638),
    surface = Color.White,
    onSurface = Color(0xFF182638),
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF536477),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

private val TeacherDarkColors = darkColorScheme(
    primary = Color(0xFF4FC3B5),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF294968),
    onPrimaryContainer = Color(0xFFE0EDFB),
    secondary = Color(0xFF8DD5C8),
    onSecondary = Color(0xFF073B35),
    secondaryContainer = Color(0xFF174B45),
    onSecondaryContainer = Color(0xFFC8F3EB),
    tertiary = Color(0xFFFFC96E),
    background = Color(0xFF101820),
    onBackground = Color(0xFFE6EDF4),
    surface = Color(0xFF18232D),
    onSurface = Color(0xFFE6EDF4),
    surfaceVariant = Color(0xFF293743),
    onSurfaceVariant = Color(0xFFBCC9D5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudentPerformanceApp() }
    }
}

// ==========================================================
// ACTIVITY LOG STORAGE
// ==========================================================

data class ActivityLogEntry(val timestamp: Long, val message: String)

private fun loadActivityLogs(context: Context): List<ActivityLogEntry> {
    val stored = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .getString("activity_log", "") ?: ""
    if (stored.isBlank()) return emptyList()
    return stored.split("\n").mapNotNull { record ->
        try {
            val divider = record.indexOf('|')
            if (divider <= 0) return@mapNotNull null
            val time = record.substring(0, divider).toLongOrNull() ?: return@mapNotNull null
            val message = String(Base64.decode(record.substring(divider + 1), Base64.DEFAULT), Charsets.UTF_8)
            ActivityLogEntry(time, message)
        } catch (_: Exception) {
            null
        }
    }.sortedByDescending { it.timestamp }.take(100)
}

private fun saveActivityLogs(context: Context, logs: List<ActivityLogEntry>) {
    val stored = logs.take(100).joinToString("\n") { entry ->
        val encoded = Base64.encodeToString(entry.message.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        "${entry.timestamp}|$encoded"
    }
    context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        .edit().putString("activity_log", stored).apply()
}

private fun formatLogTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}


// SMALL SHARED UI PIECES

private fun formAccent(form: String): Color = when (form.substringAfter("Form ").toIntOrNull()) {
    1 -> Color(0xFF2F80ED); 2 -> Color(0xFF079B83); 3 -> Color(0xFF8256D0); else -> Color(0xFFF29924)
}

private fun gradeColor(grade: String): Color = when (grade) {
    "A" -> Color(0xFF1B9E4B)
    "B" -> Color(0xFF0E8F83)
    "C" -> Color(0xFFE69500)
    "D" -> Color(0xFFE06B00)
    "E" -> Color(0xFFD32F2F)
    else -> Color.Gray
}

private fun percent(value: Double) = String.format(Locale.getDefault(), "%.0f%%", value)

/** Navy header + rounded content sheet, like every screen in the mockup. */
@Composable
fun ScreenFrame(header: @Composable () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy)) {
        header()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(MaterialTheme.colorScheme.background),
            content = content
        )
    }
}

@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Rounded.ArrowBack,
    iconDescription: String = "Back",
    onIconClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onIconClick) { Icon(icon, iconDescription, tint = Color.White) }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
        }
        actions()
    }
}

@Composable
private fun Avatar(name: String, size: Dp) {
    val palette = listOf(Color(0xFF2F80ED), Color(0xFF079B83), Color(0xFF8256D0), Color(0xFFF29924), Color(0xFFE5566D))
    val color = palette[abs(name.hashCode()) % palette.size]
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S",
            color = color, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
private fun RankBadge(position: Int?) {
    val color = when (position) { 1 -> Gold; 2 -> Silver; 3 -> Bronze; else -> null }
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape)
            .background(color ?: MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            position?.toString() ?: "–", fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (color != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f))
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val top = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val step = size.width / (values.size - 1)
        fun y(v: Float) = size.height - (v / top) * size.height * 0.85f - size.height * 0.05f
        val line = Path().apply {
            values.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(step * i, y(v)) }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.14f))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun DropdownField(
    value: String,
    placeholder: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    isDisabled: (String) -> Boolean = { false },
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(
                value.ifEmpty { placeholder },
                modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, textAlign = TextAlign.Start,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val disabled = isDisabled(option)
                DropdownMenuItem(
                    text = { Text(if (disabled) "$option ✓" else option) },
                    enabled = !disabled,
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

// ==========================================================
// SUBJECT ENTRY ROWS (used by Add Student and the subject dialog)
// ==========================================================

internal data class SubjectEntry(val subjectName: String = "", val mark: String = "")

@Composable
private fun SubjectRows(entries: List<SubjectEntry>, taken: Set<String>, onChange: (List<SubjectEntry>) -> Unit) {
    entries.forEachIndexed { index, entry ->
        val usedElsewhere = entries.filterIndexed { i, _ -> i != index }
            .map { it.subjectName.trim().lowercase() }.toSet()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DropdownField(
                value = entry.subjectName,
                placeholder = "Subject",
                options = COMMON_SUBJECTS,
                modifier = Modifier.weight(1f),
                isDisabled = { option -> option.lowercase() in taken || option.lowercase() in usedElsewhere },
                onSelect = { picked ->
                    onChange(entries.toMutableList().also { list -> list[index] = entry.copy(subjectName = picked) })
                }
            )
            OutlinedTextField(
                value = entry.mark,
                onValueChange = { text ->
                    if (text.all { ch -> ch.isDigit() } && text.length <= 3) {
                        onChange(entries.toMutableList().also { list -> list[index] = entry.copy(mark = text) })
                    }
                },
                modifier = Modifier.width(84.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Mark") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            IconButton(onClick = {
                onChange(if (entries.size > 1) entries.filterIndexed { i, _ -> i != index } else listOf(SubjectEntry()))
            }) {
                Icon(Icons.Rounded.DeleteOutline, "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Returns valid (subject, mark) pairs, or an error message. Completely empty rows are skipped. */
internal fun validateEntries(entries: List<SubjectEntry>, taken: Set<String>): Pair<List<Pair<String, Int>>, String> {
    val valid = mutableListOf<Pair<String, Int>>()
    for ((index, entry) in entries.withIndex()) {
        val name = entry.subjectName.trim()
        if (name.isEmpty() && entry.mark.isEmpty()) continue
        val mark = entry.mark.toIntOrNull()
        val problem = when {
            name.isEmpty() -> "Please choose a subject for row ${index + 1}."
            mark == null -> "Please enter a mark for $name."
            mark !in 0..100 -> "The mark for $name must be between 0 and 100."
            name.lowercase() in taken || valid.any { it.first.lowercase() == name.lowercase() } ->
                "$name is already added."
            else -> null
        }
        if (problem != null) return Pair(emptyList(), problem)
        valid.add(name to (mark ?: 0))
    }
    return Pair(valid, "")
}

// ==========================================================
// ROOT: SPLASH + DRAWER + BOTTOM BAR + NAVIGATION
// ==========================================================

@Composable
fun StudentPerformanceApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE) }
    val dao = remember { AppDatabase.getDatabase(context).studentDao() }
    val students by dao.getAllStudents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showSplash by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(preferences.getBoolean("dark_mode", false)) }
    var activityLogs by remember { mutableStateOf(loadActivityLogs(context)) }
    var dataRevision by remember { mutableIntStateOf(0) }
    var overall by remember { mutableStateOf<List<StudentRanking>>(emptyList()) }
    var currentScreen by remember { mutableStateOf("dashboard") }
    var selectedClass by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    LaunchedEffect(Unit) { delay(1600); showSplash = false }

    LaunchedEffect(students, dataRevision) { overall = calculateClassRanking(students, dao) }

    fun recordActivity(message: String) {
        val updated = (listOf(ActivityLogEntry(System.currentTimeMillis(), message)) + activityLogs).take(100)
        activityLogs = updated
        saveActivityLogs(context, updated)
        dataRevision += 1
    }

    fun setThemeMode(isDark: Boolean) {
        darkMode = isDark
        preferences.edit().putBoolean("dark_mode", isDark).apply()
    }

    fun go(screen: String) {
        currentScreen = screen
        scope.launch { drawerState.close() }
    }

    val parentScreen = mapOf(
        "classes" to "dashboard", "ranking" to "dashboard", "add_student" to "dashboard",
        "activity_log" to "dashboard", "class_students" to "classes", "student_performance" to "class_students",
        "reports" to "dashboard", "about" to "dashboard"
    )
    BackHandler(enabled = !showSplash && currentScreen != "dashboard") {
        currentScreen = parentScreen[currentScreen] ?: "dashboard"
    }

    val formCounts = FORMS.associateWith { form -> students.count { it.className.equals(form, true) } }
    val ranked = overall.filter { it.subjectCount > 0 }
    val classAverage = if (ranked.isEmpty()) 0.0 else ranked.map { it.average }.average()
    val formAverages = FORMS.associateWith { form ->
        val list = ranked.filter { it.student.className.equals(form, true) }
        if (list.isEmpty()) 0.0 else list.map { it.average }.average()
    }

    // Keep the bottom navigation visible on every screen after the splash.
    val selectedTab = when (currentScreen) {
        "dashboard", "activity_log", "classes", "class_students", "ranking", "student_performance" ->
            if (currentScreen == "dashboard" || currentScreen == "activity_log") "home" else "performance"
        "add_student" -> "add"
        else -> "home"
    }
    val showBottomBar = true

    MaterialTheme(colorScheme = if (darkMode) TeacherDarkColors else TeacherLightColors) {
        if (showSplash) {
            SplashScreen()
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Navy).padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Rounded.School, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Student Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Version 1.0", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val drawerColors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Teal.copy(alpha = 0.14f),
                            selectedIconColor = Teal,
                            selectedTextColor = Teal
                        )
                        listOf(
                            Triple("Home", Icons.Rounded.Home, "dashboard"),
                            Triple("Performance", Icons.Rounded.BarChart, "ranking"),
                            Triple("Add Student", Icons.Rounded.AddCircleOutline, "add_student"),
                            Triple("Classes (Forms)", Icons.Rounded.Groups, "classes"),
                            Triple("Activity Log", Icons.Rounded.History, "activity_log"),
                            Triple("Academic Reports", Icons.Rounded.Description, "reports")
                        ).forEach { (label, icon, screen) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                icon = { Icon(icon, null) },
                                selected = currentScreen == screen,
                                onClick = { go(screen) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                colors = drawerColors
                            )
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (darkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(14.dp))
                            Text(if (darkMode) "Dark Mode" else "Light Mode", modifier = Modifier.weight(1f))
                            Switch(checked = darkMode, onCheckedChange = { setThemeMode(it) })
                        }
                        NavigationDrawerItem(
                            label = { Text("About App") },
                            icon = { Icon(Icons.Rounded.Info, null) },
                            selected = false,
                            onClick = { go("about") },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        NavigationDrawerItem(
                            label = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                            icon = { Icon(Icons.Rounded.Logout, null, tint = MaterialTheme.colorScheme.error) },
                            selected = false,
                            onClick = { (context as? Activity)?.finish() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                            )
                        )
                    }
                }
            ) {
                Scaffold(
                    containerColor = Navy,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = Navy, contentColor = Color.White) {
                                val itemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color.White.copy(alpha = 0.65f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.65f),
                                    indicatorColor = Teal
                                )
                                NavigationBarItem(
                                    selected = selectedTab == "home", onClick = { currentScreen = "dashboard" },
                                    icon = { Icon(Icons.Rounded.Home, "Home") }, label = { Text("Home") }, colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = selectedTab == "performance", onClick = { currentScreen = "ranking" },
                                    icon = { Icon(Icons.Rounded.BarChart, "Performance") }, label = { Text("Performance") }, colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = selectedTab == "add", onClick = { currentScreen = "add_student" },
                                    icon = { Icon(Icons.Rounded.AddCircleOutline, "Add Student") }, label = { Text("Add Student") }, colors = itemColors
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            "dashboard" -> DashboardScreen(
                                studentCount = students.size,
                                formCounts = formCounts,
                                formAverages = formAverages,
                                classAverage = classAverage,
                                darkMode = darkMode,
                                onMenu = { scope.launch { drawerState.open() } },
                                onDarkModeChange = { setThemeMode(it) },
                                onViewActivity = { currentScreen = "activity_log" },
                                onFormSelected = { selectedClass = it; currentScreen = "class_students" },
                                onViewPerformance = { currentScreen = "ranking" }
                            )

                            "activity_log" -> ActivityLogScreen(activityLogs) { currentScreen = "dashboard" }

                            "reports" -> ReportsScreen(students = students, dao = dao, onBack = { currentScreen = "dashboard" })

                            "about" -> AboutAppScreen(onBack = { currentScreen = "dashboard" })

                            "add_student" -> AddStudentScreen(
                                onBack = { currentScreen = "dashboard" },
                                onStudentAdded = { currentScreen = "dashboard" },
                                onLog = { recordActivity(it) }
                            )

                            "classes" -> ClassesScreen(
                                formCounts = formCounts,
                                onBack = { currentScreen = "dashboard" },
                                onClassSelected = { selectedClass = it; currentScreen = "class_students" }
                            )

                            "ranking" -> PerformanceRankingScreen(
                                rankings = overall,
                                formCounts = formCounts,
                                formAverages = formAverages,
                                onBack = { currentScreen = "dashboard" },
                                onStudentSelected = { student ->
                                    selectedStudent = student
                                    selectedClass = student.className
                                    currentScreen = "student_performance"
                                },
                                onFormSelected = { selectedClass = it; currentScreen = "class_students" }
                            )

                            "class_students" -> ClassStudentsScreen(
                                className = selectedClass,
                                students = students.filter { it.className.equals(selectedClass, true) },
                                rankingRefreshKey = dataRevision,
                                onBack = { currentScreen = "classes" },
                                onStudentSelected = { selectedStudent = it; currentScreen = "student_performance" }
                            )

                            "student_performance" -> selectedStudent?.let { student ->
                                StudentPerformanceScreen(
                                    student = student,
                                    onBack = { currentScreen = "class_students" },
                                    onStudentUpdated = { updated ->
                                        selectedStudent = updated
                                        selectedClass = updated.className
                                    },
                                    onStudentDeleted = { currentScreen = "class_students" },
                                    onLog = { recordActivity(it) }
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

// ==========================================================
// ABOUT APP: STEP-BY-STEP TEACHER GUIDE (ENGLISH / CHICHEWA)
// ==========================================================

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    var language by remember { mutableStateOf("English") }
    val isChichewa = language == "Chichewa"

    val steps = if (isChichewa) {
        listOf(
            Triple("1. Tsegulani pulogalamuyi", "Mukalowa, mudzaona tsamba la Home. Likuonetsa chiwerengero cha ophunzira ndi makalasi a Form 1 mpaka Form 4.", "Gwiritsani ntchito menyu ya pansi kapena menyu ya m'mbali kuti mupite kumasamba ena."),
            Triple("2. Onjezani wophunzira", "Dinani Add Student. Lembani dzina lonse la wophunzirayo, sankhani Form yake, kenako dinani Save Student.", "Onetsetsani kuti dzina ndi Form ndi zolondola musanasunge."),
            Triple("3. Ikani maphunziro ndi ma mark", "Pa Add Student, sankhani phunziro lililonse ndipo lembani mark yake. Mark iyenera kukhala pakati pa 0 ndi 100. Mungawonjezere maphunziro angapo.", "Mukhoza kuwonjezera kapena kusintha maphunziro ndi ma mark pa tsamba la wophunzirayo."),
            Triple("4. Onani momwe ophunzira akuchitira", "Dinani Performance. Sankhani By Average kuti muone ophunzira motsatira average yawo, kapena By Class kuti muone Form 1 mpaka Form 4.", "Dinani pa dzina la wophunzira kuti mutsegule zambiri za momwe akuchitira."),
            Triple("5. Onani kalasi", "Sankhani Form kuti muone ophunzira a m'kalasimo. Dinani pa wophunzira kuti muone maphunziro, ma mark, average, grade ndi malo ake.", "Mukhoza kusintha kapena kuchotsa mark pa tsamba la wophunzirayo."),
            Triple("6. Pangani malipoti", "Tsegulani Academic Reports. Lembani dzina la sukulu, sankhani Individual learner, Whole school kapena Both reports, kenako sankhani wophunzira ngati pakufunika.", "Dinani Save PDF Report kuti musunge PDF. Dinani Export Selected Report to Excel (CSV) kuti mupange fayilo yotsegulidwa mu Excel."),
            Triple("7. Onani mbiri ya ntchito", "Tsegulani Activity Log kuti muone zochitika monga kuwonjezera wophunzira kapena kusintha ma mark.", "Gwiritsani ntchito Dark Mode kapena Light Mode mu menyu kuti musinthe maonekedwe a pulogalamuyi.")
        )
    } else {
        listOf(
            Triple("1. Open the application", "When the app opens, you will see the Home page. It shows the total number of learners and the classes from Form 1 to Form 4.", "Use the bottom navigation bar or the side menu to move between pages."),
            Triple("2. Add a learner", "Tap Add Student. Enter the learner's full name, select the correct Form, then tap Save Student.", "Check that the learner's name and Form are correct before saving."),
            Triple("3. Enter subjects and marks", "On Add Student, choose each subject and enter its mark. A mark must be between 0 and 100. You can add several subjects.", "You can also add, edit or delete subjects and marks from the learner's performance page."),
            Triple("4. View learner performance", "Tap Performance. Choose By Average to see learners ordered by their average, or By Class to view Form 1, Form 2, Form 3 and Form 4.", "Tap a learner's name to open their detailed performance page."),
            Triple("5. View a class", "Choose a Form to see the learners in that class. Tap a learner to see subjects, marks, average, grade and position.", "You can edit or delete a mark from the learner's page."),
            Triple("6. Generate academic reports", "Open Academic Reports. Enter the school name, choose Individual learner, Whole school or Both reports, and select a learner when required.", "Tap Save PDF Report to create a PDF. Tap Export Selected Report to Excel (CSV) to create a file that can be opened in Microsoft Excel."),
            Triple("7. Check activity and settings", "Open Activity Log to review actions such as adding learners or changing marks.", "Use Dark Mode or Light Mode in the side menu to change the app's appearance.")
        )
    }

    ScreenFrame(
        header = {
            AppHeader(
                title = if (isChichewa) "Za Pulogalamuyi" else "About the App",
                subtitle = if (isChichewa) "Malangizo a aphunzitsi" else "Step-by-step teacher guide",
                onIconClick = onBack
            )
        }
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.School, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Student Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text("Better Records • Better Decisions • Brighter Futures", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (isChichewa) "Pulogalamuyi imathandiza aphunzitsi kusunga ma mark, kuona momwe ophunzira akuchitira ndi kupanga malipoti."
                        else "This app helps teachers record learner marks, monitor performance and prepare academic reports.",
                        color = Color.White, fontSize = 13.sp
                    )
                }
            }

            Text(if (isChichewa) "SANKHANI CHINENERO" else "CHOOSE INSTRUCTIONS LANGUAGE",
                fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)) {
                listOf("English", "Chichewa").forEach { option ->
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                        .background(if (language == option) Teal else Color.Transparent)
                        .clickable { language = option }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(option, fontWeight = FontWeight.Bold,
                            color = if (language == option) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text(if (isChichewa) "MMENE MPHUNZITSI ANGAGWIRITSIRE NTCHITO PULOGALAMUYI" else "HOW A TEACHER USES THE APP",
                fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

            steps.forEachIndexed { index, step ->
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(step.first, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(step.second, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(7.dp))
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Rounded.Lightbulb, null, tint = Teal, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(step.third, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Teal.copy(alpha = 0.10f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(if (isChichewa) "ZINDIKIRANI" else "IMPORTANT NOTE", fontWeight = FontWeight.Bold, color = Teal)
                    Text(
                        if (isChichewa) "Pulogalamuyi imawerengera average kuchokera ku maphunziro ndi ma mark amene mwasunga. Onetsetsani kuti ma mark onse ndi olondola musanapange lipoti."
                        else "The app calculates each learner's average using the subjects and marks currently saved for that learner. Check that all marks are correct before generating reports.",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================================
// SPLASH
// ==========================================================

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF0B4A6F), NavyDeep))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Rounded.School, null, tint = Color.White, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(18.dp))
            Text("Student\nPerformance", fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold,
                color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text("Better Records  •  Better Decisions\nBrighter Futures", fontSize = 13.sp, lineHeight = 20.sp,
                color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(56.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Transparent, border = BorderStroke(1.dp, Teal)) {
                Row(Modifier.padding(horizontal = 28.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Teal)
                    Spacer(Modifier.width(10.dp))
                    Text("Loading...", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================================
// HOME / DASHBOARD
// ==========================================================

@Composable
fun DashboardScreen(
    studentCount: Int,
    formCounts: Map<String, Int>,
    formAverages: Map<String, Double>,
    classAverage: Double,
    darkMode: Boolean,
    onMenu: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onViewActivity: () -> Unit,
    onFormSelected: (String) -> Unit,
    onViewPerformance: () -> Unit
) {
    val greeting = remember { greetingForNow() }

    ScreenFrame(
        header = {
            AppHeader(
                title = "Student Performance",
                subtitle = "Track  •  Monitor  •  Improve",
                icon = Icons.Rounded.Menu,
                iconDescription = "Menu",
                onIconClick = onMenu
            ) {
                IconButton(onClick = onViewActivity) { Icon(Icons.Rounded.History, "Recent activity", tint = Color.White) }
                IconButton(onClick = { onDarkModeChange(!darkMode) }) {
                    Icon(if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "Change theme", tint = Color.White)
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // Greeting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("$greeting,", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Teacher", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Keep up the great work!", fontSize = 12.sp, color = Teal)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Total students
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Teal, Color(0xFF0B6E8A))))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Groups, null, tint = Color.White, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Total Students", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        Text(studentCount.toString(), fontSize = 34.sp, lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.BarChart, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            FORMS.chunked(2).forEachIndexed { rowIndex, rowForms ->
                if (rowIndex > 0) Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowForms.forEach { form ->
                        FormCountCard(form, formCounts[form] ?: 0, Modifier.weight(1f)) { onFormSelected(form) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Performance overview
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onViewPerformance() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BarChart, null, tint = Teal)
                        Spacer(Modifier.width(10.dp))
                        Text("Performance Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Class Average", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (classAverage > 0) percent(classAverage) else "—",
                                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text("Form 1–4 averages", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Sparkline(
                                values = FORMS.map { (formAverages[it] ?: 0.0).toFloat() },
                                color = Teal,
                                modifier = Modifier.width(120.dp).height(56.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormCountCard(form: String, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = formAccent(form)
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Groups, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(form, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$count Students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ==========================================================
// ACTIVITY LOG
// ==========================================================

@Composable
private fun ActivityLogRow(entry: ActivityLogEntry) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) { Text("✓", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.message, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(formatLogTime(entry.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityLogScreen(logs: List<ActivityLogEntry>, onBack: () -> Unit) {
    ScreenFrame(header = { AppHeader("Activity Log", "Student and mark changes on this device", onIconClick = onBack) }) {
        if (logs.isEmpty()) {
            Text(
                "No activity recorded yet. Add a student or update marks to begin the log.",
                modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { "${it.timestamp}-${it.message}" }) { entry ->
                    Card(
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) { ActivityLogRow(entry) }
                }
            }
        }
    }
}

// ==========================================================
// CLASSES (FORMS)
// ==========================================================

@Composable
fun ClassesScreen(formCounts: Map<String, Int>, onBack: () -> Unit, onClassSelected: (String) -> Unit) {
    ScreenFrame(header = { AppHeader("Classes (Forms)", onIconClick = onBack) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            FORMS.forEach { form ->
                val accent = formAccent(form)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onClassSelected(form) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Groups, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(form, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${formCounts[form] ?: 0} Students", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, "Open $form", tint = accent, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Teal.copy(alpha = 0.12f))
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.School, null, tint = Teal, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("“Education is the key\nto a better tomorrow.”", fontStyle = FontStyle.Italic, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==========================================================
// FORM STUDENTS (ranked list)
// ==========================================================

@Composable
fun ClassStudentsScreen(
    className: String,
    students: List<Student>,
    rankingRefreshKey: Int = 0,
    onBack: () -> Unit,
    onStudentSelected: (Student) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).studentDao() }
    var rankings by remember { mutableStateOf<List<StudentRanking>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(students, rankingRefreshKey) { rankings = calculateClassRanking(students, dao) }

    val ranked = rankings.filter { it.subjectCount > 0 }
    val classAverage = if (ranked.isEmpty()) 0.0 else ranked.map { it.average }.average()
    val bestGrade = if (ranked.isEmpty()) "—" else calculateGrade(ranked.maxOf { it.average })

    val rows = (if (rankings.isNotEmpty()) rankings else students.map { StudentRanking(it, 0.0, null, 0) })
        .sortedWith(compareBy<StudentRanking> { it.position ?: Int.MAX_VALUE }.thenBy { it.student.name.lowercase() })
        .filter { query.isBlank() || it.student.name.contains(query.trim(), ignoreCase = true) }

    ScreenFrame(header = { AppHeader("$className - Students", onIconClick = onBack) }) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.Groups, students.size.toString(), "Total Students", Teal, Modifier.weight(1f))
                StatTile(Icons.Rounded.BarChart, if (ranked.isEmpty()) "—" else percent(classAverage), "Class Average", Color(0xFF2F80ED), Modifier.weight(1f))
                StatTile(Icons.Rounded.Star, bestGrade, "Best Grade", Color(0xFFF29924), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text("Search students...") }
            )
            Spacer(Modifier.height(12.dp))

            if (students.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No students yet", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Use Add Student in the bottom menu to get started.", textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(rows, key = { it.student.id }) { row ->
                        StudentRankRow(
                            student = row.student, position = row.position, average = row.average,
                            subjectCount = row.subjectCount, showClass = false
                        ) { onStudentSelected(row.student) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentRankRow(
    student: Student,
    position: Int?,
    average: Double,
    subjectCount: Int,
    showClass: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(if (subjectCount > 0) position else null)
            Spacer(Modifier.width(10.dp))
            Avatar(student.name, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subjectCount > 0) {
                    Row {
                        Text("Average: ${percent(average)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (showClass) {
                            Spacer(Modifier.width(10.dp))
                            val grade = calculateGrade(average)
                            Text("Grade: $grade", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = gradeColor(grade))
                        }
                    }
                    Text(
                        if (showClass) student.className else "$subjectCount subjects",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("No marks recorded yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (position == 1 && subjectCount > 0) Text("👑", fontSize = 18.sp)
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================================
// PERFORMANCE: BY AVERAGE / BY CLASS
// ==========================================================

@Composable
fun PerformanceRankingScreen(
    rankings: List<StudentRanking>,
    formCounts: Map<String, Int>,
    formAverages: Map<String, Double>,
    onBack: () -> Unit,
    onStudentSelected: (Student) -> Unit,
    onFormSelected: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val ranked = rankings.filter { it.subjectCount > 0 }
        .sortedWith(compareBy<StudentRanking> { it.position ?: Int.MAX_VALUE }.thenBy { it.student.name.lowercase() })

    ScreenFrame(header = { AppHeader("Student Performance", onIconClick = onBack) }) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                listOf("By Average", "By Class").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (tab == i) Teal else Color.Transparent)
                            .clickable { tab = i }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold,
                            color = if (tab == i) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (tab == 0) {
                if (ranked.isEmpty()) {
                    Text("No marks recorded yet. Open a student and add subjects to see rankings.",
                        modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ranked, key = { it.student.id }) { row ->
                            StudentRankRow(row.student, row.position, row.average, row.subjectCount, showClass = true) {
                                onStudentSelected(row.student)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(FORMS) { form ->
                        val accent = formAccent(form)
                        val avg = formAverages[form] ?: 0.0
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onFormSelected(form) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Groups, null, tint = Color.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(form, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${formCounts[form] ?: 0} Students  •  Average: ${if (avg > 0) percent(avg) else "—"}",
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// ADD STUDENT
// ==========================================================

@Composable
fun AddStudentScreen(onBack: () -> Unit, onStudentAdded: () -> Unit, onLog: (String) -> Unit = {}) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).studentDao() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf(SubjectEntry())) }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    ScreenFrame(header = { AppHeader("Add Student", onIconClick = onBack) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Full Name *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = "" },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Enter student name") }
            )
            Spacer(Modifier.height(14.dp))
            Text("Form *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            DropdownField(selectedClass, "Select form", FORMS, Modifier.fillMaxWidth()) { selectedClass = it; error = "" }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add Subjects & Marks", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(
                    onClick = { entries = entries + SubjectEntry(); error = "" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Subject", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            SubjectRows(entries, emptySet()) { entries = it; error = "" }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { name = ""; selectedClass = ""; entries = listOf(SubjectEntry()); error = "" },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Teal)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Teal)
                    Spacer(Modifier.width(6.dp))
                    Text("Clear", color = Teal)
                }
                Button(
                    onClick = {
                        val cleanName = name.trim().replace(Regex("\\s+"), " ")
                        val (subjectList, problem) = validateEntries(entries, emptySet())
                        when {
                            cleanName.isEmpty() -> error = "Please enter the student's full name."
                            selectedClass.isEmpty() -> error = "Please select the form."
                            problem.isNotEmpty() -> error = problem
                            else -> {
                                saving = true
                                scope.launch {
                                    try {
                                        val duplicate = dao.getAllStudents().first().any {
                                            it.className.equals(selectedClass, true) && it.name.trim().equals(cleanName, true)
                                        }
                                        if (duplicate) {
                                            error = "$cleanName is already in $selectedClass."
                                            saving = false
                                        } else {
                                            dao.insertStudent(Student(name = cleanName, className = selectedClass))
                                            if (subjectList.isNotEmpty()) {
                                                val saved = dao.getAllStudents().first()
                                                    .filter { it.name == cleanName && it.className == selectedClass }
                                                    .maxByOrNull { it.id }
                                                saved?.let { s ->
                                                    subjectList.forEach { item ->
                                                        dao.insertSubject(Subject(studentId = s.id, subjectName = item.first, mark = item.second))
                                                    }
                                                }
                                            }
                                            onLog(
                                                "Added student $cleanName to $selectedClass" +
                                                        if (subjectList.isNotEmpty()) " with ${subjectList.size} subject(s)" else ""
                                            )
                                            onStudentAdded()
                                        }
                                    } catch (e: Exception) {
                                        error = "Could not save the student. Please try again."
                                        saving = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) {
                    Icon(Icons.Rounded.Save, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (saving) "Saving..." else "Save Student", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================================
// INDIVIDUAL STUDENT PERFORMANCE
// ==========================================================

@Composable
fun StudentPerformanceScreen(
    student: Student,
    onBack: () -> Unit,
    onStudentUpdated: (Student) -> Unit,
    onStudentDeleted: () -> Unit,
    onLog: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).studentDao() }
    val scope = rememberCoroutineScope()
    val subjects by dao.getSubjectsForStudent(student.id).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    var showEditStudent by remember { mutableStateOf(false) }
    var showDeleteStudent by remember { mutableStateOf(false) }
    var classRankings by remember { mutableStateOf<List<StudentRanking>>(emptyList()) }

    val average = if (subjects.isNotEmpty()) subjects.map { it.mark }.average() else 0.0
    val grade = if (subjects.isEmpty()) "—" else calculateGrade(average)

    LaunchedEffect(student.id, student.className, subjects) {
        val classmates = dao.getAllStudents().first().filter { it.className.equals(student.className, true) }
        classRankings = calculateClassRanking(classmates, dao)
    }
    val position = classRankings.firstOrNull { it.student.id == student.id }?.position

    ScreenFrame(header = { AppHeader("Student Performance", onIconClick = onBack) }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(student.name, 72.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(student.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (subjects.isNotEmpty() && position != null) {
                            Spacer(Modifier.height(4.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFE8A3)) {
                                Text("👑 Position #$position", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A4200))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (subjects.isEmpty()) student.className
                            else "${student.className}  •  Average: ${percent(average)}  •  Grade: $grade",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.MenuBook, subjects.size.toString(), "Subjects", Teal, Modifier.weight(1f))
                StatTile(Icons.Rounded.Percent, if (subjects.isEmpty()) "—" else percent(average), "Average", Teal, Modifier.weight(1f))
                StatTile(Icons.Rounded.Star, grade, "Grade", Color(0xFFF29924), Modifier.weight(1f))
            }
            if (subjects.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(getEncouragement(average), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(18.dp))
            Text("Subject Marks", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Subject", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Mark", Modifier.width(46.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Grade", Modifier.width(44.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(68.dp))
                    }
                    if (subjects.isEmpty()) {
                        Text("No marks recorded yet. Tap “Add Subjects” below.", modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    subjects.forEachIndexed { index, subject ->
                        val g = calculateGrade(subject.mark.toDouble())
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(subject.subjectName, Modifier.weight(1f), fontSize = 13.sp)
                            Text("${subject.mark}", Modifier.width(46.dp), fontSize = 13.sp)
                            Text(g, Modifier.width(44.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = gradeColor(g))
                            IconButton(onClick = { subjectToEdit = subject }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.Edit, "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { subjectToDelete = subject }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (index < subjects.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) { Text("+ Add Subjects", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showEditStudent = true },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Edit Student") }
                OutlinedButton(
                    onClick = { showDeleteStudent = true },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Delete Student", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showAddDialog) {
        SubjectDialog(
            title = "Add subjects", buttonText = "Save all", existingSubjects = subjects, editingSubject = null,
            onDismiss = { showAddDialog = false },
            onSaveMultiple = { list ->
                scope.launch {
                    list.forEach { dao.insertSubject(Subject(studentId = student.id, subjectName = it.first, mark = it.second)) }
                    onLog("Added ${list.size} subject(s) for ${student.name}: ${list.joinToString { it.first }}")
                    showAddDialog = false
                }
            }
        )
    }

    subjectToEdit?.let { subject ->
        SubjectDialog(
            title = "Update subject", buttonText = "Save changes", existingSubjects = subjects, editingSubject = subject,
            onDismiss = { subjectToEdit = null },
            onSaveMultiple = { list ->
                scope.launch {
                    list.firstOrNull()?.let { updated ->
                        dao.updateSubject(subject.copy(subjectName = updated.first, mark = updated.second))
                        onLog("Updated ${student.name}'s ${subject.subjectName} mark to ${updated.second}%")
                    }
                    subjectToEdit = null
                }
            }
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete subject?") },
            text = { Text("Delete ${subject.subjectName} and its saved mark for ${student.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.deleteSubject(subject)
                        onLog("Deleted ${subject.subjectName} mark for ${student.name}")
                        subjectToDelete = null
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { subjectToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showDeleteStudent) {
        AlertDialog(
            onDismissRequest = { showDeleteStudent = false },
            title = { Text("Delete student?") },
            text = { Text("Delete ${student.name} and remove the student from ${student.className}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.deleteStudent(student)
                        onLog("Deleted student ${student.name} from ${student.className}")
                        showDeleteStudent = false
                        onStudentDeleted()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteStudent = false }) { Text("Cancel") } }
        )
    }

    if (showEditStudent) {
        EditStudentDialog(
            student = student,
            onDismiss = { showEditStudent = false },
            onSave = { newName, newClass ->
                scope.launch {
                    val updated = student.copy(name = newName, className = newClass)
                    dao.updateStudent(updated)
                    onLog("Updated student record: ${student.name} (${student.className}) to $newName ($newClass)")
                    onStudentUpdated(updated)
                    showEditStudent = false
                }
            }
        )
    }
}

// ==========================================================
// DIALOGS
// ==========================================================

@Composable
fun SubjectDialog(
    title: String,
    buttonText: String,
    existingSubjects: List<Subject>,
    editingSubject: Subject?,
    onDismiss: () -> Unit,
    onSaveMultiple: (List<Pair<String, Int>>) -> Unit
) {
    val taken = existingSubjects
        .filter { editingSubject == null || it.id != editingSubject.id }
        .map { it.subjectName.trim().lowercase() }.toSet()
    var entries by remember {
        mutableStateOf(
            if (editingSubject != null) listOf(SubjectEntry(editingSubject.subjectName, editingSubject.mark.toString()))
            else listOf(SubjectEntry())
        )
    }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (editingSubject == null) "Enter several subjects and marks before saving." else "Update this subject and its mark.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                )
                Spacer(Modifier.height(10.dp))
                SubjectRows(entries, taken) { entries = it; error = "" }
                if (editingSubject == null) {
                    OutlinedButton(
                        onClick = { entries = entries + SubjectEntry(); error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("+ Add Another Subject", color = Teal, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(6.dp))
                Text("Marks must be between 0 and 100.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val (valid, problem) = validateEntries(entries, taken)
                when {
                    problem.isNotEmpty() -> error = problem
                    valid.isEmpty() -> error = "Please add at least one subject."
                    else -> onSaveMultiple(valid)
                }
            }) { Text(buttonText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditStudentDialog(student: Student, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember(student.id) { mutableStateOf(student.name) }
    var selectedClass by remember(student.id) { mutableStateOf(student.className) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text("Student Name") }
                )
                Spacer(Modifier.height(12.dp))
                Text("Form", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                DropdownField(selectedClass, "Select form", FORMS, Modifier.fillMaxWidth()) { selectedClass = it }
                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().isEmpty()) error = "Student name cannot be empty." else onSave(name.trim(), selectedClass)
            }) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ==========================================================
// GRADES, ENCOURAGEMENT, POSITIONS AND RANKING
// ==========================================================

data class StudentRanking(
    val student: Student,
    val average: Double,
    val position: Int?,
    val subjectCount: Int
)

fun calculateGrade(average: Double): String = when {
    average >= 80 -> "A"
    average >= 70 -> "B"
    average >= 60 -> "C"
    average >= 50 -> "D"
    else -> "E"
}

fun getEncouragement(average: Double): String = when {
    average >= 80 -> "Excellent work! Keep it up."
    average >= 70 -> "Very good work. Keep working hard."
    average >= 60 -> "Good progress. You can do even better."
    average >= 50 -> "Keep improving. Your hard work will pay off."
    else -> "Keep trying. Do not give up; you can improve."
}

fun positionText(position: Int): String = when {
    position % 100 in 11..13 -> "${position}th"
    position % 10 == 1 -> "${position}st"
    position % 10 == 2 -> "${position}nd"
    position % 10 == 3 -> "${position}rd"
    else -> "${position}th"
}

suspend fun calculateClassRanking(students: List<Student>, dao: StudentDao): List<StudentRanking> {
    val raw = mutableListOf<Triple<Student, Double, Int>>()
    for (student in students) {
        val saved = dao.getSubjectsForStudent(student.id).first()
        val average = if (saved.isNotEmpty()) saved.map { it.mark }.average() else 0.0
        raw.add(Triple(student, average, saved.size))
    }

    val ranked = raw.filter { it.third > 0 }

    return raw.map { (student, average, subjectCount) ->
        val position = if (subjectCount == 0) null else ranked.count { it.second > average } + 1
        StudentRanking(student, average, position, subjectCount)
    }
}
