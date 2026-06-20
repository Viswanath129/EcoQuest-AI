package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChatMessage
import com.example.data.Quest
import com.example.data.UserStats
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Navigation Tabs ---
enum class EcoTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    QUESTS("Quests", Icons.Default.Star),
    LEADERBOARD("Leaders", Icons.Default.Leaderboard),
    COACH("AI Coach", Icons.AutoMirrored.Filled.Chat),
    PROFILE("Profile", Icons.Default.Person)
}

// --- Main App Scaffolding ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoQuestApp(
    viewModel: EcoViewModel,
    modifier: Modifier = Modifier
) {
    val userStats by viewModel.userStats.collectAsState()
    val quests by viewModel.quests.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isGeneratingQuests by viewModel.isGeneratingQuests.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
    val showCelebration by viewModel.showCelebration.collectAsState()
    val recentQuest by viewModel.recentCelebrationQuest.collectAsState()
    val isApiKeyWarning by viewModel.apiKeyWarning.collectAsState()

    var currentTab by remember { mutableStateOf(EcoTab.DASHBOARD) }

    // Seed initial database stats and messages
    LaunchedEffect(Unit) {
        viewModel.seedInitialDataIfNeeded()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (userStats != null && !userStats!!.onboarded) {
            // Force Onboarding Flow on fresh launch
            OnboardingScreen(
                onComplete = { name, country, transport, food, electricity, shopping ->
                    viewModel.completeOnboarding(name, country, transport, food, electricity, shopping)
                    currentTab = EcoTab.DASHBOARD
                }
            )
        } else {
            // Main Scaffold with Bottom Navigation
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_navigation")
                    ) {
                        EcoTab.values().forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else EcoTextMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else EcoTextMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        EcoTab.DASHBOARD -> DashboardScreen(
                            stats = userStats,
                            viewModel = viewModel
                        )
                        EcoTab.QUESTS -> QuestsScreen(
                            quests = quests,
                            isGenerating = isGeneratingQuests,
                            viewModel = viewModel,
                            isApiKeyWarning = isApiKeyWarning
                        )
                        EcoTab.LEADERBOARD -> LeaderboardScreen(
                            stats = userStats
                        )
                        EcoTab.COACH -> CoachScreen(
                            chatHistory = chatHistory,
                            isSending = isSendingMessage,
                            viewModel = viewModel,
                            isWarning = isApiKeyWarning
                        )
                        EcoTab.PROFILE -> ProfileScreen(
                            stats = userStats,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Animated Confetti Celebration Overlay
        if (showCelebration && recentQuest != null) {
            CelebrationDialog(
                quest = recentQuest!!,
                onDismiss = { viewModel.dismissCelebration() }
            )
        }
    }
}

// --- DashBoard / Home Screen ---
@Composable
fun DashboardScreen(
    stats: UserStats?,
    viewModel: EcoViewModel
) {
    val scrollState = rememberScrollState()
    val resolvedStats = stats ?: UserStats()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Hero Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome Back! 👋",
                    fontSize = 16.sp,
                    color = EcoTextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "EcoQuest AI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Daily Streak Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(EcoWarning.copy(alpha = 0.15f))
                    .border(1.dp, EcoWarning.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = EcoWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${resolvedStats.streakDays} Day Streak",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = EcoWarning
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Level & XP Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LEVEL ${resolvedStats.level}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = viewModel.getLevelName(resolvedStats.level),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Level Badge Icon (Custom design)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Badge",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // XP Progress Bar
                val progress = resolvedStats.xp.toFloat() / resolvedStats.xpToNextLevel.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${resolvedStats.xp} XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Next level: ${resolvedStats.xpToNextLevel} XP",
                        fontSize = 12.sp,
                        color = EcoTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated Carbon scoring gauge card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Carbon Score",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Assessment from transportation, eating, and electricity",
                    fontSize = 13.sp,
                    color = EcoTextMuted,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Custom circular gauge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            val scoreSweep = (resolvedStats.carbonScore.toFloat() / 100f) * 270f
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(EcoDanger, EcoWarning, EcoPrimary)
                                ),
                                startAngle = 135f,
                                sweepAngle = scoreSweep,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${resolvedStats.carbonScore}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "out of 100",
                                fontSize = 10.sp,
                                color = EcoTextMuted
                            )
                        }
                    }

                    // Stat numbers column
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text("Annual Emissions", fontSize = 11.sp, color = EcoTextMuted)
                            Text(
                                text = String.format("%.1f Tons CO₂", resolvedStats.annualEmissions),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoDanger
                            )
                        }
                        Column {
                            Text("Impact Category", fontSize = 11.sp, color = EcoTextMuted)
                            val catColor = when (resolvedStats.impactCategory) {
                                "Low" -> EcoPrimary
                                "Moderate" -> EcoWarning
                                else -> EcoDanger
                            }
                            Text(
                                text = resolvedStats.impactCategory,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = catColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Comparisons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Analysis",
                        tint = EcoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your daily habits put you in the top 33% of green users!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Total Stat Highlights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "CO2",
                        tint = EcoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%.1f kg", resolvedStats.co2SavedCumulative),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Total CO₂ Saved", fontSize = 11.sp, color = EcoTextMuted)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "Tasks Completed",
                        tint = EcoAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${resolvedStats.missionsCompleted}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Missions Mastered", fontSize = 11.sp, color = EcoTextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Projections Futures Simulator Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Future Carbon Simulator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "See your ecological accumulation over time",
                    fontSize = 12.sp,
                    color = EcoTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var sliderValue by remember { mutableStateOf(5f) } // default 5 years

                // Dynamically projected calculations
                val annual = resolvedStats.annualEmissions
                val projectedTons = annual * sliderValue
                // 1 tree absorbs about 22kg CO2, corresponding to 0.022 Ton offset annually
                val treesNeeded = (projectedTons / 0.022).toInt()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Timeline:",
                            fontSize = 13.sp,
                            color = EcoTextMuted
                        )
                        Text(
                            text = "${sliderValue.toInt()} Years Projection",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Projected Emissions", fontSize = 10.sp, color = EcoTextMuted)
                            Text(
                                text = String.format("%.1f Tons CO₂", projectedTons),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoDanger
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Trees Required to Offset", fontSize = 10.sp, color = EcoTextMuted)
                            Text(
                                text = "$treesNeeded Trees",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontally Scrollable Achievement Badges
        Text(
            text = "Achievement Badges",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            val achievements = listOf(
                BadgeItem("🌱", "Eco Starter", "Complete your profile check", true),
                BadgeItem("⚡", "Volt Buster", "Master 3 standby energy quests", resolvedStats.missionsCompleted >= 1),
                BadgeItem("🥗", "Meat Optimizer", "Record 2 vegetarian meals", resolvedStats.missionsCompleted >= 2),
                BadgeItem("🚲", "Cycle Champ", "Complete a transit quest", resolvedStats.missionsCompleted >= 3),
                BadgeItem("👑", "Earth Guardian", "Maintain a 5 day streak", resolvedStats.streakDays >= 5)
            )

            items(achievements) { badge ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (badge.unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (badge.unlocked) EcoPrimary.copy(alpha = 0.3f) else Color.Transparent
                    ),
                    modifier = Modifier.width(135.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = badge.emoji,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = badge.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = badge.desc,
                            fontSize = 10.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 11.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (badge.unlocked) "ACTIVED" else "LOCKED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badge.unlocked) EcoPrimary else EcoTextMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (badge.unlocked) EcoPrimary.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

data class BadgeItem(val emoji: String, val title: String, val desc: String, val unlocked: Boolean)

// --- Daily Quests Page ---
@Composable
fun QuestsScreen(
    quests: List<Quest>,
    isGenerating: Boolean,
    viewModel: EcoViewModel,
    isApiKeyWarning: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Quests",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Complete missions to earn XP and save CO₂",
                    fontSize = 13.sp,
                    color = EcoTextMuted
                )
            }

            // Generate Button
            Button(
                onClick = { viewModel.generateMissions() },
                enabled = !isGenerating,
                modifier = Modifier.testTag("renew_quests_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Regenerate", fontSize = 12.sp)
            }
        }

        // Gemini Warning / Simulation banner
        if (isApiKeyWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = EcoWarning.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, EcoWarning.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = "Simulated",
                        tint = EcoWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Running under offline assessment engine. Supply your GEMINI_API_KEY in the Secrets panel to activate live AI generation!",
                        fontSize = 11.sp,
                        color = EcoTextDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (quests.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EnergySavingsLeaf,
                        contentDescription = "Empty leaf",
                        tint = Color.LightGray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No quests currently generated.", color = EcoTextMuted)
                    Text("Click 'AI Regenerate' above to create custom missions!", fontSize = 12.sp, color = EcoTextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quests) { quest ->
                    QuestCard(
                        quest = quest,
                        onCompleteClick = { viewModel.completeQuest(quest) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestCard(
    quest: Quest,
    onCompleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quest_card_${quest.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quest.completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (quest.completed) EcoPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = quest.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (quest.completed) EcoPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = quest.description,
                        fontSize = 14.sp,
                        color = EcoTextMuted,
                        lineHeight = 18.sp
                    )
                }

                // Status Action indicator
                if (quest.completed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = EcoPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Button(
                        onClick = onCompleteClick,
                        modifier = Modifier.testTag("complete_quest_btn_${quest.id}"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                    ) {
                        Text("Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specs Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // XP Score chip
                Text(
                    text = "+${quest.xp} XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EcoSecondary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // CO2 saved chip
                Text(
                    text = "saved ${quest.co2Saved}kg CO₂",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoSuccess,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EcoSuccess.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Difficulty chip
                val diffColor = when (quest.difficulty) {
                    "Easy" -> EcoPrimary
                    "Medium" -> EcoWarning
                    else -> EcoDanger
                }
                Text(
                    text = quest.difficulty,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = diffColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(diffColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Toggle Detail Expander
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand details",
                        tint = EcoTextMuted
                    )
                }
            }

            // Expanded Science description block
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Science Context:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = quest.reason,
                        fontSize = 12.sp,
                        color = EcoTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// --- Leaderboard Screen ---
@Composable
fun LeaderboardScreen(stats: UserStats?) {
    var globalTabSelected by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "National College Leaders",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Weekly rankings of top students saving emissions",
                    fontSize = 12.sp,
                    color = EcoTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tab selectors
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp)
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val idleColor = Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (globalTabSelected) activeColor else idleColor)
                            .clickable { globalTabSelected = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Global Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (globalTabSelected) Color.White else EcoTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!globalTabSelected) activeColor else idleColor)
                            .clickable { globalTabSelected = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Friends Circle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!globalTabSelected) Color.White else EcoTextMuted
                        )
                    }
                }
            }
        }

        // Leader rows
        val mockLeaders = if (globalTabSelected) {
            listOf(
                LeaderRow(1, "🌱 GreenDean99", "San Jose State", 955, 14.8, 8, true),
                LeaderRow(2, "⚡ VoltVanguard", "UC Berkeley", 890, 12.4, 5, false),
                LeaderRow(3, "🚴 CarbonCrusher", "Stanford University", 845, 11.2, 4, false),
                LeaderRow(4, "🥗 VeggieBite", "CSU Long Beach", 790, 9.8, 6, false),
                LeaderRow(5, "You (Rookie)", "This Campus", stats?.xp ?: 0, stats?.co2SavedCumulative ?: 0.0, stats?.streakDays ?: 1, true, isCurrentUser = true),
                LeaderRow(6, "🎒 DormPedestrian", "UT Austin", 680, 8.2, 3, false),
                LeaderRow(7, "📱 EnergyWise", "CSU Fullerton", 610, 7.5, 2, false),
                LeaderRow(8, "🥤 ZeroSingleUse", "San Diego State", 540, 6.8, 4, false),
                LeaderRow(9, "🚂 RailRoamer", "Arizona State", 490, 5.9, 3, false),
                LeaderRow(10, "🏡 EcoNester", "Oregon State", 410, 4.3, 1, false)
            )
        } else {
            listOf(
                LeaderRow(1, "🚴 CarbonCrusher", "Stanford University", 845, 11.2, 4, true),
                LeaderRow(2, "You (Rookie)", "This Campus", stats?.xp ?: 0, stats?.co2SavedCumulative ?: 0.0, stats?.streakDays ?: 1, true, isCurrentUser = true),
                LeaderRow(3, "🎒 DormPedestrian", "UT Austin", 680, 8.2, 3, false),
                LeaderRow(4, "🥤 ZeroSingleUse", "San Diego State", 540, 6.8, 4, false)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mockLeaders) { row ->
                val bg = if (row.isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                val border = if (row.isCurrentUser) BorderStroke(1.2.dp, EcoPrimary) else null

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = border
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank position or Trophy
                        Box(
                            modifier = Modifier.width(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (row.rank == 1) {
                                Text("👑", fontSize = 20.sp)
                            } else {
                                Text(
                                    text = "${row.rank}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (row.rank <= 3) MaterialTheme.colorScheme.primary else EcoTextMuted
                                )
                            }
                        }

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (row.isCurrentUser) EcoPrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = row.name.take(2).uppercase(),
                                color = if (row.isCurrentUser) Color.White else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = row.college,
                                fontSize = 11.sp,
                                color = EcoTextMuted
                            )
                        }

                        // Score
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${row.xp} XP",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format("%.1f kg Saved", row.co2Saved),
                                fontSize = 11.sp,
                                color = EcoSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LeaderRow(
    val rank: Int,
    val name: String,
    val college: String,
    val xp: Int,
    val co2Saved: Double,
    val streak: Int,
    val hasCrown: Boolean,
    val isCurrentUser: Boolean = false
)

// --- AI Sustainability Coach Screen ---
@Composable
fun CoachScreen(
    chatHistory: List<ChatMessage>,
    isSending: Boolean,
    viewModel: EcoViewModel,
    isWarning: Boolean
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScrollState() // Plain scroll state for Chat bubble list column

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to bottom of chat history when it updates
    LaunchedEffect(chatHistory.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EcoPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🍃", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Coach GreenLeaf",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "AI Sustainability Advisor",
                    fontSize = 12.sp,
                    color = EcoSuccess,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Live Mode vs Simulation Mode Warnings
        if (isWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(containerColor = EcoWarning.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, EcoWarning.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Offline Coach Simulation. Real Gemini responses unlock with a valid GEMINI_API_KEY in secure Secrets.",
                    fontSize = 11.sp,
                    color = EcoTextDark,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Messages Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (chatHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chat is empty. Start typing to get suggestions!", color = EcoTextMuted)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chatHistory.forEach { chat ->
                        val isUser = chat.role == "user"
                        ChatBubble(
                            text = chat.content,
                            isUser = isUser
                        )
                    }

                    if (isSending) {
                        TypingBubble()
                    }
                }
            }
        }

        // Suggested Chips
        val suggestedPrompts = listOf(
            "How can students reduce emissions?",
            "Eco food habits",
            "Unplug vampire loads"
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestedPrompts) { chipText ->
                Card(
                    modifier = Modifier.clickable {
                        viewModel.sendCoachMessage(chipText)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = chipText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Field Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask GreenLeaf standard tips...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.trim().isNotEmpty()) {
                            viewModel.sendCoachMessage(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        }
                    }),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (textInput.trim().isNotEmpty()) {
                            viewModel.sendCoachMessage(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("chat_send_button"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleBg = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
        val textColor = if (isUser) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        val border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleBg),
            border = border
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
fun TypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val transition = rememberInfiniteTransition()
                val dotAlpha1 by transition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 0),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val dotAlpha2 by transition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 200),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val dotAlpha3 by transition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 400),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EcoPrimary.copy(alpha = dotAlpha1)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EcoPrimary.copy(alpha = dotAlpha2)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EcoPrimary.copy(alpha = dotAlpha3)))
            }
        }
    }
}

// --- Profile & Core Actions Page ---
@Composable
fun ProfileScreen(
    stats: UserStats?,
    viewModel: EcoViewModel
) {
    val scrollState = rememberScrollState()
    var showResetWarning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Header
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(EcoPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("🌍", fontSize = 54.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Planet Champion",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "EcoQuest Member since 2026",
            fontSize = 12.sp,
            color = EcoTextMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recalibrate Carbon Profile button
        Button(
            onClick = {
                viewModel.reassessFootprint()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.SettingsSuggest, contentDescription = "Recalibrate")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reassess Carbon Footprint")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reset App button
        OutlinedButton(
            onClick = { showResetWarning = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EcoDanger),
            border = BorderStroke(1.2.dp, EcoDanger)
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = "Reset")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear App and Reset Data")
        }

        // Reset dialog confirmation
        if (showResetWarning) {
            AlertDialog(
                onDismissRequest = { showResetWarning = false },
                title = { Text("Reset EcoQuest AI?", fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently clear your level, accumulated XP, streak counters, completed carbon calculations, and chat history. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetAllData()
                        showResetWarning = false
                    }) {
                        Text("Reset Permanently", color = EcoDanger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetWarning = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Static App Details Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("About EcoQuest AI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A national climate hackathon prototype designed using modern Material 3 and Google DeepMind's Gemini-3.5-flash text models. Streamlined to inspire and simplify green accountability cycles.",
                    fontSize = 12.sp,
                    color = EcoTextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// --- Onboarding & Smart Multi-Step Carbon Assessment Page ---
@Composable
fun OnboardingScreen(
    onComplete: (String, String, String, String, String, String) -> Unit
) {
    var step by remember { mutableStateOf(1) }

    // User preferences
    var name by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf("Walking/Biking primary") }
    var food by remember { mutableStateOf("Moderate Meat / Veg") }
    var electricity by remember { mutableStateOf("Moderate home use") }
    var shopping by remember { mutableStateOf("Occasional buyer") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("onboarding_wizard_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details
                Text(
                    text = "ECOQUEST AI",
                    fontWeight = FontWeight.ExtraBold,
                    color = EcoPrimary,
                    fontSize = 15.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Step $step of 6",
                    fontSize = 13.sp,
                    color = EcoTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Multi Step progress indicator
                LinearProgressIndicator(
                    progress = { step.toFloat() / 6f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EcoPrimary,
                    trackColor = EcoPrimary.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Steppers rendering
                when (step) {
                    1 -> {
                        Text("Welcome, Champion! 🌍", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            text = "EcoQuest transforms carbon auditing into a satisfying gamified experience. Let's start with your label!",
                            fontSize = 13.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("What name should we call you?") },
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_field"),
                            maxLines = 1,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EcoPrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }

                    2 -> {
                        Text("Commuting Habits 🚗", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "How do you get to work, college, or run errands typically?",
                            fontSize = 13.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        val transportOptions = listOf(
                            "Gasoline Car Solo",
                            "Electric / Hybrid",
                            "Public Transit",
                            "Walking/Biking primary"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            transportOptions.forEach { opt ->
                                val active = transport == opt
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) EcoPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (active) EcoPrimary else Color.LightGray.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { transport = opt }
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) EcoSecondary else EcoTextDark
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        Text("Food Preferences 🍲", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "Your dining plays a massive role in global methane and crop emissions.",
                            fontSize = 13.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        val foodOptions = listOf(
                            "Heavy Meat Consumer",
                            "Moderate Meat / Veg",
                            "Vegetarian",
                            "Strict Vegan"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            foodOptions.forEach { opt ->
                                val active = food == opt
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) EcoPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (active) EcoPrimary else Color.LightGray.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { food = opt }
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) EcoSecondary else EcoTextDark
                                    )
                                }
                            }
                        }
                    }

                    4 -> {
                        Text("Grid Power Usage 🔌", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "How active are high-density electrical systems in your home?",
                            fontSize = 13.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        val electricityOptions = listOf(
                            "High electrical use",
                            "Moderate home use",
                            "Eco conscious / off grid"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            electricityOptions.forEach { opt ->
                                val active = electricity == opt
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) EcoPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (active) EcoPrimary else Color.LightGray.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { electricity = opt }
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) EcoSecondary else EcoTextDark
                                    )
                                }
                            }
                        }
                    }

                    5 -> {
                        Text("Shopping Habits 🛍️", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "How often do you acquire brand-new physical or fashion items?",
                            fontSize = 13.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        val shoppingOptions = listOf(
                            "Frequent shopper",
                            "Occasional buyer",
                            "Vintage / Thrift collector"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            shoppingOptions.forEach { opt ->
                                val active = shopping == opt
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) EcoPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (active) EcoPrimary else Color.LightGray.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { shopping = opt }
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) EcoSecondary else EcoTextDark
                                    )
                                }
                            }
                        }
                    }

                    6 -> {
                        Text("Ready for AI Carbon Check!", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "We are ready to compile your profile data, compute your baseline score, and configure your personalized daily quests. Let's go!",
                            fontSize = 13.sp,
                            color = EcoTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = EcoBackground)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Name: ${name.ifEmpty { "Champion" }}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Transit: $transport", fontSize = 12.sp)
                                Text("Dietary: $food", fontSize = 12.sp)
                                Text("Household grid: $electricity", fontSize = 12.sp)
                                Text("Shopping: $shopping", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
                        TextButton(
                            onClick = { step-- },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Back", color = EcoTextMuted)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 6) {
                                if (step == 1 && name.trim().isEmpty()) {
                                    name = "Eco Warrior"
                                }
                                step++
                            } else {
                                onComplete(name, "USA", transport, food, electricity, shopping)
                            }
                        },
                        modifier = Modifier.testTag("onboarding_continue_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary)
                    ) {
                        Text(
                            text = if (step == 6) "Analyze & Start" else "Continue",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- Confetti particle structures inside Dialog ---
@Composable
fun CelebrationDialog(
    quest: Quest,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val transition = rememberInfiniteTransition()

        // Create random particles in state
        val particles = remember {
            List(35) {
                ConfettiParticle(
                    color = listOf(EcoPrimary, EcoSecondary, EcoAccent, EcoSuccess, EcoWarning).random(),
                    x = Random.nextFloat(),
                    y = -Random.nextFloat() * 100f - 20f,
                    speed = Random.nextFloat() * 6f + 3f,
                    size = Random.nextInt(8, 20).toFloat(),
                    angle = Random.nextFloat() * 360f,
                    rotSpeed = Random.nextFloat() * 4f - 2f
                )
            }
        }

        // Particle animator updating states
        var tickTrigger by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30)
                particles.forEach { p ->
                    p.y += p.speed
                    p.angle += p.rotSpeed
                }
                tickTrigger++
            }
        }

        Box(
            modifier = Modifier
                .size(320.dp, 360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw direct custom canvas confetti drops
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dummy = tickTrigger // force recompose
                particles.forEach { p ->
                    drawRect(
                        color = p.color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = p.x * size.width,
                            y = (p.y) % size.height
                        ),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size)
                    )
                }
            }

            // Central congratulations visual banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🏆", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Mission Complete!",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = EcoSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = quest.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EcoTextDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EcoPrimary.copy(alpha = 0.12f))
                    ) {
                        Text(
                            "+${quest.xp} XP",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = EcoPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = EcoSuccess.copy(alpha = 0.12f))
                    ) {
                        Text(
                            "-${quest.co2Saved}kg CO₂",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = EcoSuccess,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EcoPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("celebration_dismiss_btn")
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

class ConfettiParticle(
    val color: Color,
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    var angle: Float,
    val rotSpeed: Float
)
