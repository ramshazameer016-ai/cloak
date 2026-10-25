package com.example.ui.tracker

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.demo.DemoConfigManager
import com.example.core.security.PinStorage
import com.example.core.security.secretShieldHold
import com.example.core.sos.SosController
import com.example.core.sos.SosState
import com.example.data.local.entity.PeriodEntry
import com.example.data.repository.ContactRepository
import com.example.data.repository.PeriodRepository
import com.example.ui.fakesleep.FakeSleepOverlay
import com.example.ui.shield.ShieldDashboardScreen
import com.example.ui.theme.CycleActiveRed
import com.example.ui.theme.CyclePredictedPink
import com.example.ui.wellbeing.DigitalWellbeingDashboardScreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class TrackerTab(val label: String) {
    CALENDAR("Calendar"),
    LOG("Log"),
    TIPS("Tips"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerMainScreen(
    pinStorage: PinStorage,
    sosController: SosController,
    contactRepository: ContactRepository,
    periodRepository: PeriodRepository,
    onLockApp: () -> Unit,
    onResetPins: () -> Unit,
    demoConfigManager: DemoConfigManager? = null,
    onNavigateToShield: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(TrackerTab.CALENDAR) }

    val sosState by sosController.state.collectAsState()
    val isSosActive = sosState is SosState.Active

    var showFakeCall by remember { mutableStateOf(false) }
    var fakeCallerName by remember { mutableStateOf("Mom") }
    var showShield by remember { mutableStateOf(false) }
    var showDigitalWellbeing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(44.dp)
                                .secretShieldHold(
                                    context = context,
                                    isSosActive = isSosActive,
                                    onActivated = { showShield = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Petal Health",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Petal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    actions = {
                        if (demoConfigManager?.isDemoModeEnabled == true) {
                            IconButton(
                                onClick = {
                                    onNavigateToShield?.invoke() ?: run {
                                        showShield = true
                                    }
                                },
                                modifier = Modifier.testTag(
                                    "demo_shield_appbar_btn"
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Digital Shield",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = onLockApp,
                            modifier = Modifier.testTag("lock_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock App",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    TrackerTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            TrackerTab.CALENDAR ->
                                Icons.Default.CalendarMonth

                            TrackerTab.LOG ->
                                Icons.Default.EditCalendar

                            TrackerTab.TIPS ->
                                Icons.Default.Lightbulb

                            TrackerTab.SETTINGS ->
                                Icons.Default.Settings
                        }

                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(
                                "tab_${tab.name.lowercase()}"
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    TrackerTab.CALENDAR -> {
                        CalendarTabView(periodRepository)
                    }

                    TrackerTab.LOG -> {
                        LogTabView(periodRepository)
                    }

                    TrackerTab.TIPS -> {
                        TipsTabView()
                    }

                    TrackerTab.SETTINGS -> {
                        SettingsTabView(
                            contactRepository = contactRepository,
                            periodRepository = periodRepository,
                            pinStorage = pinStorage,
                            sosState = sosState,
                            sosController = sosController,
                            onLaunchFakeCall = { caller ->
                                fakeCallerName = caller
                                showFakeCall = true
                            },
                            onResetPins = onResetPins,
                            demoConfigManager = demoConfigManager,
                            onOpenShield = {
                                onNavigateToShield?.invoke() ?: run {
                                    showShield = true
                                }
                            },
                            onOpenDigitalWellbeing = {
                                showDigitalWellbeing = true
                            }
                        )
                    }
                }
            }
        }

        // Existing simulated call screen.
        if (showFakeCall) {
            FakeCallScreen(
                callerName = fakeCallerName,
                onDismiss = { showFakeCall = false }
            )
        }

        // Existing Fake-Sleep functionality.
        FakeSleepOverlay(
            isSosActive = isSosActive,
            pinStorage = pinStorage,
            sosController = sosController,
            inactivityThresholdMs = 15_000L,
            onNormalPinUnlocked = { },
            onDuressPinUnlocked = { }
        )

        // Existing Digital Shield dashboard.
        if (showShield && !isSosActive) {
            ShieldDashboardScreen(
                contactRepository = contactRepository,
                onCloseShield = { showShield = false }
            )
        }

        // New, separate Digital Wellbeing dashboard.
        if (showDigitalWellbeing) {
            DigitalWellbeingDashboardScreen(
                onBack = {
                    showDigitalWellbeing = false
                }
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CALENDAR
// -------------------------------------------------------------

@Composable
private fun CalendarTabView(periodRepository: PeriodRepository) {
    val scope = rememberCoroutineScope()

    var currentMonth by remember {
        mutableStateOf(YearMonth.now())
    }

    val entries by periodRepository.allEntriesFlow.collectAsState(
        initial = emptyList()
    )

    var predictedPeriod by remember {
        mutableStateOf<Pair<LocalDate, LocalDate>?>(null)
    }

    LaunchedEffect(entries) {
        predictedPeriod = periodRepository.predictNextPeriod()
    }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month"
                )
            }

            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var dayCounter = 1

        for (row in 0..5) {
            if (dayCounter > daysInMonth) break

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0..6) {
                    if (
                        (row == 0 && col < firstDayOfWeek) ||
                        dayCounter > daysInMonth
                    ) {
                        Box(modifier = Modifier.size(40.dp))
                    } else {
                        val date = currentMonth.atDay(dayCounter)
                        val dateStr = date.format(
                            PeriodRepository.DATE_FORMATTER
                        )

                        val entry = entries.find {
                            it.dateString == dateStr
                        }

                        val isPeriod = entry?.isPeriodDay == true

                        val hasSymptoms =
                            entry?.symptoms?.isNotBlank() == true ||
                                entry?.mood?.isNotBlank() == true

                        val isPredicted = predictedPeriod?.let {
                                (start, end) ->
                            !date.isBefore(start) &&
                                !date.isAfter(end)
                        } ?: false

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPeriod -> CycleActiveRed
                                        isPredicted -> CyclePredictedPink
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    scope.launch {
                                        periodRepository.togglePeriodDay(date)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$dayCounter",
                                    fontSize = 13.sp,
                                    fontWeight = if (
                                        isPeriod || isPredicted
                                    ) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    color = when {
                                        isPeriod -> Color.White
                                        isPredicted ->
                                            MaterialTheme.colorScheme.primary

                                        else ->
                                            MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                if (hasSymptoms) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPeriod) {
                                                    Color.White
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        dayCounter++
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Legend & Predictions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(CycleActiveRed)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Period Days (Logged)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(CyclePredictedPink)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Predicted Next Period (Default 28-day cycle)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        "Logged Symptoms / Mood",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap any date to toggle period flow or log symptoms in the 'Log' tab.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: LOG
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTabView(periodRepository: PeriodRepository) {
    val scope = rememberCoroutineScope()

    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }

    var flowIntensity by remember {
        mutableStateOf("Medium")
    }

    val selectedSymptoms = remember {
        mutableStateOf(setOf<String>())
    }

    var selectedMood by remember {
        mutableStateOf("Calm")
    }

    var notes by remember {
        mutableStateOf("")
    }

    var saveConfirmed by remember {
        mutableStateOf(false)
    }

    val dateStr = selectedDate.format(
        PeriodRepository.DATE_FORMATTER
    )

    LaunchedEffect(selectedDate) {
        val existing = periodRepository.getEntry(selectedDate)

        if (existing != null) {
            flowIntensity = existing.flowIntensity.ifBlank { "None" }

            selectedSymptoms.value = existing.symptoms
                .split(",")
                .filter { it.isNotBlank() }
                .toSet()

            selectedMood = existing.mood.ifBlank { "Calm" }
            notes = existing.notes
        } else {
            flowIntensity = "None"
            selectedSymptoms.value = emptySet()
            selectedMood = "Calm"
            notes = ""
        }

        saveConfirmed = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Daily Wellness Log: $dateStr",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Flow Intensity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("None", "Light", "Medium", "Heavy").forEach { level ->
                        FilterChip(
                            selected = flowIntensity == level,
                            onClick = { flowIntensity = level },
                            label = { Text(level) }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Symptoms",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val symptomList = listOf(
                    "Cramps",
                    "Headache",
                    "Bloating",
                    "Fatigue",
                    "Tender Breasts",
                    "Acne",
                    "Backache",
                    "Cravings"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(symptomList) { symptom ->
                        val isSelected =
                            selectedSymptoms.value.contains(symptom)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val current =
                                    selectedSymptoms.value.toMutableSet()

                                if (isSelected) {
                                    current.remove(symptom)
                                } else {
                                    current.add(symptom)
                                }

                                selectedSymptoms.value = current
                            },
                            label = { Text(symptom) }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Mood",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val moodList = listOf(
                    "Happy",
                    "Calm",
                    "Sensitive",
                    "Irritable",
                    "Exhausted",
                    "Energetic"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moodList) { mood ->
                        FilterChip(
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            label = { Text(mood) }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Personal Journal & Notes") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Button(
            onClick = {
                scope.launch {
                    val entry = PeriodEntry(
                        dateString = dateStr,
                        isPeriodDay = flowIntensity != "None",
                        flowIntensity = flowIntensity,
                        symptoms = selectedSymptoms.value.joinToString(","),
                        mood = selectedMood,
                        notes = notes
                    )

                    periodRepository.saveEntry(entry)
                    saveConfirmed = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                if (saveConfirmed) "Saved ✓"
                else "Save Wellness Log"
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 3: TIPS
// -------------------------------------------------------------

@Composable
private fun TipsTabView() {
    val tips = listOf(
        "1. Hydration & Prostaglandins" to
            "Drinking 2.5L of warm water daily helps flush excess sodium and decreases uterine prostaglandin cramping.",

        "2. Magnesium for Cramp Relief" to
            "Magnesium glycinate (300mg) relaxes uterine smooth muscles and diminishes period pain naturally.",

        "3. Iron Replenishment during Flow" to
            "Consume dark leafy greens, lentils, and lean proteins with vitamin C to maximize dietary non-heme iron absorption.",

        "4. Sleep Architecture in Luteal Phase" to
            "Elevated progesterone can raise resting body temperature by 0.5°C; keep your bedroom at 18°C for optimal deep REM sleep.",

        "5. Cycle-Synched Fitness" to
            "Engage in strength and HIIT workouts during follicular phase when estrogen peaks; switch to gentle yoga during menstruation.",

        "6. Fiber for Estrogen Balance" to
            "Cruciferous vegetables (broccoli, cabbage) provide indole-3-carbinol which supports the liver in metabolizing circulating estrogen.",

        "7. Heat Therapy vs Pain Mediators" to
            "Applying a heating pad (40°C) over the lower pelvic area increases local microvascular blood flow as effectively as ibuprofen.",

        "8. Caffeine's Effect on Breast Tissue" to
            "Reducing coffee during the week prior to menstruation may help some people with cyclic breast tenderness.",

        "9. Vitamin B6 & Neurotransmitters" to
            "Vitamin B6 supports neurotransmitter synthesis. Avoid high-dose supplements unless a clinician recommends them.",

        "10. Follicular Phase Energy Ascent" to
            "Energy can vary throughout the cycle; track your own pattern rather than assuming every cycle will feel the same.",

        "11. Ovulation Identification" to
            "Clear, stretchy cervical mucus can occur near ovulation. Basal body temperature rises after ovulation and confirms it retrospectively.",

        "12. Anti-Inflammatory Nutrition" to
            "Include sources of omega-3 fats such as walnuts, flaxseed, chia seeds, and oily fish as part of a balanced diet.",

        "13. Chamomile & Ginger Teas" to
            "Ginger tea may help some people with nausea. Check with a clinician if symptoms are persistent or severe.",

        "14. Consistent Hormonal Tracking" to
            "Recording your cycle over several months can help identify patterns and provide useful information for a healthcare professional.",

        "15. Restorative Menstrual Recovery" to
            "Prioritize rest, gentle movement, hydration, and comfort during menstruation, adjusting activity to how you feel."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Evidence-Based Cycle Health",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Curated wellness guidance for every cycle phase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    top = 2.dp,
                    bottom = 8.dp
                )
            )
        }

        items(tips) { (title, content) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: SETTINGS
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsTabView(
    contactRepository: ContactRepository,
    periodRepository: PeriodRepository,
    pinStorage: PinStorage,
    sosState: SosState,
    sosController: SosController,
    onLaunchFakeCall: (String) -> Unit,
    onResetPins: () -> Unit,
    demoConfigManager: DemoConfigManager? = null,
    onOpenShield: () -> Unit = {},
    onOpenDigitalWellbeing: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val contacts by contactRepository.contactsFlow.collectAsState(
        initial = emptyList()
    )

    var cycleLength by remember {
        mutableIntStateOf(periodRepository.getAverageCycleLength())
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var nameInput by remember {
        mutableStateOf("")
    }

    var phoneInput by remember {
        mutableStateOf("")
    }

    var contactError by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Disguise",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {
                        onLaunchFakeCall("Mom")
                    },
                    onLongClick = {
                        onLaunchFakeCall("Police")
                    }
                )
                .testTag("call_gynecologist_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Call Gynecologist",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Tap for 'Mom' • Long press for 'Police' simulated call.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                            alpha = 0.8f
                        )
                    )
                }
            }
        }

        // Support Circle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Support Circle (${contacts.size}/3)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (contacts.size < 3) {
                        OutlinedButton(
                            onClick = {
                                nameInput = ""
                                phoneInput = ""
                                contactError = null
                                showAddDialog = true
                            },
                            modifier = Modifier.testTag(
                                "settings_add_contact_button"
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text("Add")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (contacts.isEmpty()) {
                    Text(
                        text = "No emergency contacts set. Configure up to 3 trusted contacts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    contacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    contact.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Text(
                                    contact.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        contactRepository.deleteContact(
                                            contact.id
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Average cycle length
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Average Cycle Length: $cycleLength days",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Slider(
                    value = cycleLength.toFloat(),
                    onValueChange = {
                        cycleLength = it.toInt()
                        periodRepository.setAverageCycleLength(cycleLength)
                    },
                    valueRange = 21f..35f,
                    steps = 13
                )

                Text(
                    "Standard default is 28 days for cycle prediction.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Security reset
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.4f
                )
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Security Reset",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        "Reset access PINs & onboarding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        pinStorage.clearAll()
                        onResetPins()
                    }
                ) {
                    Text("Reset PINs")
                }
            }
        }

        // New Digital Wellbeing entry point.
        // Available even when Demo Mode is disabled.
        OutlinedButton(
            onClick = onOpenDigitalWellbeing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_digital_wellbeing_btn")
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Open Digital Wellbeing")
        }

        // Existing demo-mode settings and Digital Shield entry point.
        if (demoConfigManager != null) {
            val isDemoEnabled by demoConfigManager
                .isDemoModeEnabledFlow.collectAsState()

            val delaySeconds by demoConfigManager
                .transitionDelaySecondsFlow.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("demo_mode_settings_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Hackathon Demo Mode",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Text(
                                "When enabled, entering the duress PIN displays a temporary visible SOS status screen before Fake-Sleep.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isDemoEnabled,
                            onCheckedChange = {
                                demoConfigManager.setDemoModeEnabled(it)
                            },
                            modifier = Modifier.testTag("demo_mode_switch")
                        )
                    }

                    if (isDemoEnabled) {
                        Column {
                            Text(
                                "Auto-transition to Fake-Sleep delay: $delaySeconds seconds",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = delaySeconds.toFloat(),
                                onValueChange = {
                                    demoConfigManager.setTransitionDelaySeconds(
                                        it.toInt()
                                    )
                                },
                                valueRange = 3f..20f,
                                steps = 16
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onOpenShield,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_shield_dashboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Open Digital Shield Dashboard")
                    }
                }
            }
        }

        // Existing Guardian Protection status.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("guardian_status_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (sosState is SosState.Active) {
                    MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.25f
                    )
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (sosState is SosState.Active) {
                                Icons.Default.NotificationsActive
                            } else {
                                Icons.Default.Security
                            },
                            contentDescription = null,
                            tint = if (sosState is SosState.Active) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Guardian Protection",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (sosState is SosState.Active) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                    ) {
                        Text(
                            text = if (sosState is SosState.Active) {
                                "ACTIVE"
                            } else {
                                "ARMED"
                            },
                            color = if (sosState is SosState.Active) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (val current = sosState) {
                    is SosState.Idle -> {
                        Text(
                            text = "Guardian is standing by in background. Ready for Duress PIN or Shake gesture. Emergency SMS and audio capture are armed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is SosState.Active -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Trigger Source: ${current.triggerSource}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )

                            Text(
                                text = "Status: ${current.statusMessage}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (current.lastMapsUrl != null) {
                                Text(
                                    text = "Live Location: ${current.lastMapsUrl}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "Updates Dispatched: ${current.updateCount} (~2 min cadence)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    sosController.stopSos()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stop_sos_settings_button")
                            ) {
                                Text("Stop Active SOS Session")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Support Circle contact dialog.
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
            },
            title = {
                Text("Add Support Circle Contact")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                        },
                        label = {
                            Text("Contact Name")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                        },
                        label = {
                            Text("Phone Number")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    contactError?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneInput.isBlank()) {
                            contactError = "Phone number is required."
                            return@Button
                        }

                        scope.launch {
                            val result = contactRepository.addContact(
                                nameInput,
                                phoneInput
                            )

                            result
                                .onSuccess {
                                    showAddDialog = false
                                }
                                .onFailure {
                                    contactError = it.message
                                }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}