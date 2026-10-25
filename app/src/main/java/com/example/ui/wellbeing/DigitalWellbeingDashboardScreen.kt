
package com.example.ui.wellbeing

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val WellbeingBackground = Color(0xFF141923)
private val WellbeingCard = Color(0xFF1E2638)
private val WellbeingAccent = Color(0xFF80CBC4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalWellbeingDashboardScreen(
    onBack: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var breakReminderEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, remainingSeconds) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds -= 1
        } else if (remainingSeconds == 0) {
            isTimerRunning = false
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = WellbeingBackground
    ) {
        Scaffold(
            containerColor = WellbeingBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Digital Wellbeing",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Petal",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF1B2232)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Make space for yourself.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Build healthier digital habits with focused work, regular breaks and mindful rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )

                // Screen-time overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WellbeingCard
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = WellbeingAccent,
                            modifier = Modifier.size(36.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp)
                        ) {
                            Text(
                                text = "Today's Screen Time",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Not connected",
                                color = WellbeingAccent,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Device usage data has not been connected.",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Focus timer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WellbeingCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = WellbeingAccent,
                            modifier = Modifier.size(30.dp)
                        )

                        Text(
                            text = "Focus Session",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = timerText,
                            color = WellbeingAccent,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = when {
                                remainingSeconds == 0 -> "Session complete!"
                                isTimerRunning -> "Stay focused. One task at a time."
                                remainingSeconds == 25 * 60 -> "Ready for a 25-minute session?"
                                else -> "Timer paused"
                            },
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (remainingSeconds == 0) {
                                        remainingSeconds = 25 * 60
                                    }
                                    isTimerRunning = !isTimerRunning
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WellbeingAccent,
                                    contentColor = Color(0xFF102020)
                                )
                            ) {
                                Text(
                                    when {
                                        remainingSeconds == 0 -> "Restart"
                                        isTimerRunning -> "Pause"
                                        else -> "Start"
                                    }
                                )
                            }

                            Button(
                                onClick = {
                                    isTimerRunning = false
                                    remainingSeconds = 25 * 60
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF354158),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }

                // Break reminder preference
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WellbeingCard
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = WellbeingAccent,
                            modifier = Modifier.size(28.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "Break Reminder",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Take a short break from your screen.",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = breakReminderEnabled,
                            onCheckedChange = {
                                breakReminderEnabled = it
                            }
                        )
                    }
                }

                // Mindfulness tip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF203633)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = WellbeingAccent,
                            modifier = Modifier.size(28.dp)
                        )

                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = "A small mindful pause",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Relax your shoulders, look away from the screen and take a few comfortable, slow breaths.",
                                color = Color(0xFFD5E8E4),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
