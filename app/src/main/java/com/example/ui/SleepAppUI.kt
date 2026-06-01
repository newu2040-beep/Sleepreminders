package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.SleepSchedule
import com.example.data.SleepSession
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.ThemePalettes
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAppNavigation(viewModel: SleepViewModel) {
    val context = LocalContext.current
    val isAlarmActive by viewModel.isAlarmSounding.collectAsState()
    val ringingSession by viewModel.ringingSession.collectAsState()

    // Screen states
    var currentTab by remember { mutableStateOf("dashboard") }

    // Request permissions launcher
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Reminder alerts will only work inside the app.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
    ) {
        Scaffold(
            bottomBar = {
                if (!isAlarmActive) {
                    SleepBottomBar(currentTab = currentTab, onTabSelect = { 
                        viewModel.stopPremadePreview()
                        currentTab = it 
                    })
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "tab_fade"
                ) { targetTab ->
                    when (targetTab) {
                        "dashboard" -> DashboardScreen(viewModel = viewModel, onGoToScheduler = { currentTab = "scheduler" })
                        "scheduler" -> SchedulerScreen(viewModel = viewModel)
                        "nature_sounds" -> AmbientSoundsScreen(viewModel = viewModel)
                        "analytics" -> AnalyticsScreen(viewModel = viewModel)
                        "settings" -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Full-screen Premium Ringing Overlay when an alarm triggers
        AnimatedVisibility(
            visible = isAlarmActive,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            RingingOverlayScreen(
                session = ringingSession,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun SleepBottomBar(currentTab: String, onTabSelect: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("app_navigation_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == "dashboard",
            onClick = { onTabSelect("dashboard") },
            icon = { Icon(if (currentTab == "dashboard") Icons.Filled.Timer else Icons.Outlined.Timer, contentDescription = "Dashboard") },
            label = { Text("Timer", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "scheduler",
            onClick = { onTabSelect("scheduler") },
            icon = { Icon(if (currentTab == "scheduler") Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth, contentDescription = "Schedules") },
            label = { Text("Schedule", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "nature_sounds",
            onClick = { onTabSelect("nature_sounds") },
            icon = { Icon(Icons.Default.MusicNote, contentDescription = "Ambience") },
            label = { Text("Sounds", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("app_nav_sounds_tab")
        )
        NavigationBarItem(
            selected = currentTab == "analytics",
            onClick = { onTabSelect("analytics") },
            icon = { Icon(if (currentTab == "analytics") Icons.Filled.BarChart else Icons.Outlined.BarChart, contentDescription = "History") },
            label = { Text("Insights", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelect("settings") },
            icon = { Icon(if (currentTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ---------------- DASHBOARD TAB ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: SleepViewModel, onGoToScheduler: () -> Unit) {
    val activeSession by viewModel.activeSession.collectAsState()
    val secondsLeft by viewModel.countdownSecondsLeft.collectAsState()
    val isDelay by viewModel.isDelayCountdown.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()

    val context = LocalContext.current
    var selectedPresetMinutes by remember { mutableStateOf(480) } // Default 8 hours
    var selectedDelayMinutes by remember { mutableStateOf(0) }

    var showPresetSheet by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }

    val stats = remember(allSessions) { viewModel.getSleepStats() }

    // PROFILE CUSTOMIZER ALERTDIALOG
    if (showProfileEditor) {
        val profileName by viewModel.profileName.collectAsState()
        val profileAge by viewModel.profileAge.collectAsState()
        val profileAddress by viewModel.profileAddress.collectAsState()
        val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
        val profileGender by viewModel.profileGender.collectAsState()
        val profileChronotype by viewModel.profileChronotype.collectAsState()

        var editName by remember { mutableStateOf(profileName) }
        var editAge by remember { mutableStateOf(profileAge) }
        var editAddress by remember { mutableStateOf(profileAddress) }
        var editGender by remember { mutableStateOf(profileGender) }
        var editChronotype by remember { mutableStateOf(profileChronotype) }
        var editPhotoUri by remember { mutableStateOf(profilePhotoUri) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    // Ignore or log
                }
                editPhotoUri = it.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showProfileEditor = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👤 Rest Profile Editor", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Customize sleep diagnostics profile & avatar.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Profile Photo selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!editPhotoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = Uri.parse(editPhotoUri),
                                    contentDescription = "Edit Avatar",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                val initials = if (editName.trim().length >= 2) {
                                    editName.trim().substring(0, 2).uppercase()
                                } else "RS"
                                Text(
                                    text = initials,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { photoPickerLauncher.launch(arrayOf("image/*")) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Import Photo", fontSize = 11.sp)
                            }
                            if (!editPhotoUri.isNullOrBlank()) {
                                TextButton(
                                    onClick = { editPhotoUri = null },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Clear Photo", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Profile Name") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_name")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it },
                            label = { Text("Age") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("profile_edit_age")
                        )

                        OutlinedTextField(
                            value = editGender,
                            onValueChange = { editGender = it },
                            label = { Text("Gender") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f).testTag("profile_edit_gender")
                        )
                    }

                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Sleep Sanctuary Address") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_address")
                    )

                    // Chronotype Selection Grid
                    Text(
                        text = "Sleep Chronotype Classification",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val chronotypes = listOf("Night Owl 🦉", "Early Bird 🌅", "Dolphin 🐬", "Bear 🐻")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chronotypes.forEach { type ->
                            val isSelected = editChronotype == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { editChronotype = type }
                                    .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = editName.ifBlank { "Active Rest Optimizer" },
                            age = editAge.ifBlank { "28" },
                            address = editAddress.ifBlank { "Sleepy Hollow, NY" },
                            photoUri = editPhotoUri,
                            gender = editGender.ifBlank { "Unspecified" },
                            chronotype = editChronotype
                        )
                        showProfileEditor = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditor = false }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Premium Header (Interactive Rest Profile trigger)
        item {
            val profileName by viewModel.profileName.collectAsState()
            val profileAge by viewModel.profileAge.collectAsState()
            val profileAddress by viewModel.profileAddress.collectAsState()
            val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
            val profileChronotype by viewModel.profileChronotype.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileEditor = true }
                    .padding(vertical = 8.dp)
                    .testTag("app_profile_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        hour in 0..11 -> "GOOD MORNING"
                        hour in 12..16 -> "GOOD AFTERNOON"
                        else -> "GOOD EVENING"
                    }
                    Text(
                        text = "$greeting • ${profileChronotype.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profileName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Age: $profileAge • $profileAddress",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profilePhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(profilePhotoUri),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        val initials = if (profileName.trim().length >= 2) {
                            profileName.trim().substring(0, 2).uppercase()
                        } else if (profileName.trim().isNotEmpty()) {
                            profileName.trim().take(1).uppercase()
                        } else {
                            "RS"
                        }
                        Text(
                            text = initials,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Timer Area
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_timer_card")
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (activeSession != null) {
                        val session = activeSession!!
                        val isPaused = session.status == "PAUSED"

                        // Active Session Countdown Visualizer Circle
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulse anim
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.96f,
                                targetValue = 1.04f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1800, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            // Glass background with pulse
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .scale(if (!isPaused) pulseScale else 1.0f)
                                    .drawBehind {
                                        drawCircle(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    ThemePalettes.LavenderNightDark.primary.copy(alpha = 0.12f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    }
                            )

                            val totalTargetSec = if (isDelay) {
                                session.delayMinutes * 60L
                            } else {
                                session.durationMinutes * 60L
                            }
                            val progressFactor = if (totalTargetSec > 0) {
                                (secondsLeft.toFloat() / totalTargetSec.toFloat()).coerceIn(0f, 1f)
                            } else 1f

                            val strokeColor = MaterialTheme.colorScheme.primary
                            val delayStrokeColor = MaterialTheme.colorScheme.tertiary
                            val neutralStroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

                            Canvas(modifier = Modifier.size(210.dp)) {
                                drawCircle(
                                    color = neutralStroke,
                                    radius = size.minDimension / 2f,
                                    style = Stroke(width = 12.dp.toPx())
                                )

                                drawArc(
                                    color = if (isDelay) delayStrokeColor else strokeColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progressFactor,
                                    useCenter = false,
                                    style = Stroke(
                                        width = 12.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isDelay) Icons.Default.HourglassEmpty else Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (isDelay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatSeconds(secondsLeft),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-1.5).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isDelay) "PREP WIND-DOWN" else if (isPaused) "PAUSED" else "RUNNING SESSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = if (isDelay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Countdown description Label
                        Text(
                            text = if (isDelay) {
                                "Prep yourself. Sleep timer triggers in: ${formatSeconds(secondsLeft)}"
                            } else {
                                "Planned duration: ${session.durationMinutes} min"
                            },
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("timer_controls")
                        ) {
                            if (session.status == "PAUSED") {
                                Button(
                                    onClick = { viewModel.resumeActiveSession() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.pauseActiveSession() },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }

                            Button(
                                onClick = { viewModel.cancelActiveSession() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("cancel_timer_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Cancel")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel")
                            }
                        }

                    } else {
                        // Empty/Setup State
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Night icon",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(68.dp)
                                .padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Sleep Timer Idle",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose a preset or setup custom countdown to start your rest timer.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Preset Configurations Slider
                        Button(
                            onClick = { showPresetSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("open_sleep_timer_setup")
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configure Sleep Session", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Presets Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Presets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showPresetSheet = true }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Power Nap
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.startSleepSession(20, 0)
                                Toast.makeText(context, "Started 20 Minutes Power Nap ⚡", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF4F378B), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Power Nap",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "20 Minutes",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Full Cycle
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.startSleepSession(90, 0)
                                Toast.makeText(context, "Started 90 Minutes Full Cycle 🌙", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF334D43), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Full Cycle",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "90 Minutes",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Streak and Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Column {
                            Text(
                                text = "Streak",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stats.streak} Days",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Average Hours Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌙",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Column {
                            Text(
                                text = "Quality",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (stats.totalSessions > 0) "88% Avg" else "--- Avg",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Smart Adaptive AI Sleep Companion
        item {
            val profileName by viewModel.profileName.collectAsState()
            val profileAge by viewModel.profileAge.collectAsState()
            val profileAddress by viewModel.profileAddress.collectAsState()
            val profileGender by viewModel.profileGender.collectAsState()
            val profileChronotype by viewModel.profileChronotype.collectAsState()

            val ageInt = profileAge.toIntOrNull() ?: 25
            val targetHours = when {
                ageInt < 18 -> 9.0
                ageInt in 18..64 -> 8.0
                else -> 7.5
            }

            val chronoEmoji = when {
                profileChronotype.contains("owl", ignoreCase = true) -> "🦉"
                profileChronotype.contains("bird", ignoreCase = true) -> "🌅"
                profileChronotype.contains("dolphin", ignoreCase = true) -> "🐬"
                else -> "🐻"
            }

            val chronoTitle = when {
                profileChronotype.contains("owl", ignoreCase = true) -> "Night Owl"
                profileChronotype.contains("bird", ignoreCase = true) -> "Early Bird"
                profileChronotype.contains("dolphin", ignoreCase = true) -> "Dolphin"
                else -> "The Bear"
            }

            val chronoInsight = when {
                profileChronotype.contains("owl", ignoreCase = true) -> {
                    "You are biologically tuned for late productivity. Avoid cognitive tasks after 10:00 PM. We have auto-adjusted soundscapes: Cosmic Thunderstorms ⛈️ or crackling woods matches your cerebral wind-down perfectly."
                }
                profileChronotype.contains("bird", ignoreCase = true) -> {
                    "Your peak biological clocks trigger massive early cortisol spikes. Wind-down suggested around 9:30 PM. Use Calming Chimes 🔔 to gently ease your bright awake state."
                }
                profileChronotype.contains("dolphin", ignoreCase = true) -> {
                    "Light & sensitive sleeper. Easily disturbed by surrounding noises. Steady Babbling Brooks 🌊 or White Noise 💨 are dynamically structured to insulate you against nocturnal arousals. Try a 20-min Power Nap ⚡ to reset cognitive exhaustion."
                }
                else -> {
                    "Directly synchronized with the earth's natural solar cycle. Ensure you target $targetHours hours of rest. Forest birds 🐦 or deep ocean waves 🌊 creates the perfect resonance for clean morning awakening."
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_companion_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = chronoEmoji, fontSize = 28.sp)
                            Column {
                                Text(
                                    text = "SMART CHRONO COMPANION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Insights for $profileName ($chronoTitle)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVE ADAPTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bio metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Diagnostic Target
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Rest Target", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$targetHours Hours",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Gender alignment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Tone Preference", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (profileGender.trim().isNotEmpty()) profileGender else "Warm Harmonic",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = chronoInsight,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sanctuary Advisor
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏡", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "SANCTUARY ENVIRONMENT ADVISOR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Physical readings simulated for: $profileAddress",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column {
                                    Text("ROOM TEMP", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text("19.4°C (Ideal)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column {
                                    Text("HUMIDITY", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text("45% (Perfect)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column {
                                    Text("AIR QUALITY", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text("12 AQI (Pure)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Canvas Sleep stats chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Sleep Tracker",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (stats.totalSessions == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Set and run timers to populate weekly rest stats.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Drawing premium Canvas-based statistics bars
                        val barColor = MaterialTheme.colorScheme.primary
                        val daysOfWeekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val spacing = size.width / 7
                            val maxMinutes = stats.weeklyMinutes.maxOrNull()?.coerceAtLeast(480) ?: 480
                            
                            stats.weeklyMinutes.forEachIndexed { index, minutes ->
                                val x = index * spacing + (spacing / 2)
                                val fraction = minutes.toFloat() / maxMinutes.toFloat()
                                val barHeight = size.height * 0.8f * fraction
                                val yStart = size.height - 15.dp.toPx()
                                val yEnd = yStart - barHeight

                                // Draw bar
                                drawRoundRect(
                                    color = barColor.copy(alpha = if (minutes > 0) 1f else 0.15f),
                                    topLeft = Offset(x - 8.dp.toPx(), yEnd),
                                    size = Size(16.dp.toPx(), barHeight.coerceAtLeast(4.dp.toPx())),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            daysOfWeekLabels.forEach { label ->
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // QUALITY SLEEP WISDOM CARD
        item {
            var selectedTipTab by remember { mutableStateOf("before") } // "before", "after", "tips"
            
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quality_sleep_tips_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 QUALITY SLEEP & NUTRITION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Expert guidance on daily routines & nutrition to optimize sleep quality and physical morning recovery.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Tab Selectors
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("before", "Before Sleep 🥑", MaterialTheme.colorScheme.primary),
                            Triple("after", "After Sleep 🥞", MaterialTheme.colorScheme.tertiary),
                            Triple("tips", "Hygiene Tips 🌙", MaterialTheme.colorScheme.secondary)
                        ).forEach { (tabId, label, activeTabColor) ->
                            val isSelected = selectedTipTab == tabId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeTabColor.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { selectedTipTab = tabId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) activeTabColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Content depending on selected tab
                    AnimatedContent(
                        targetState = selectedTipTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                        },
                        label = "tips_tab_animation"
                    ) { tab ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (tab) {
                                "before" -> {
                                    TipItem(
                                        emoji = "🌰",
                                        title = "Almonds & Walnuts",
                                        desc = "Rich in melatonin and magnesium, which reduce muscle tension and trigger sleepiness."
                                    )
                                    TipItem(
                                        emoji = "🍒",
                                        title = "Tart Cherry Juice & Kiwis",
                                        desc = "Natural catalysts containing tryptophan, serotonin and anthocyanins to prolong restful REM states."
                                    )
                                    TipItem(
                                        emoji = "🍵",
                                        title = "Chamomile or Valerian Tea",
                                        desc = "Apigenin antioxidants bind to GABA receptors to tranquilize the nervous system prior to rest."
                                    )
                                    TipItem(
                                        emoji = "⚠️",
                                        title = "Steer Clear",
                                        desc = "Do not eat heavy, sugary or ultra-spicy meals within 3 hours of sleep to avoid severe digestive arousal."
                                    )
                                }
                                "after" -> {
                                    TipItem(
                                        emoji = "🚰",
                                        title = "Immediate Hydration First",
                                        desc = "Drink 300-500ml of water right after lifting your head to rehydrate cells after 8 hours of dry breathing."
                                    )
                                    TipItem(
                                        emoji = "🍳",
                                        title = "Balanced Protein-Rich Breakfast",
                                        desc = "Include eggs, spinach, oats, or Greek yogurt. Protein stabilizes cortisol levels and maintains stable alertness."
                                    )
                                    TipItem(
                                        emoji = "☀️",
                                        title = "10 Mins First Sunlight",
                                        desc = "View open morning sunlight to set of metabolic timers and prompt melatonin production clock for next night."
                                    )
                                    TipItem(
                                        emoji = "🔇",
                                        title = "Zero Phone in First 15 Mins",
                                        desc = "Prevent dopamine spikes and artificial stress indicators. Breathe, stretch or journal to begin your routine calmly."
                                    )
                                }
                                "tips" -> {
                                    TipItem(
                                        emoji = "❄️",
                                        title = "Cool and Pitch Dark Rooms",
                                        desc = "Your core body temperature must drop by 2-3°F. Maintain cold air (65-68°F) and absolute pitch-dark conditions."
                                    )
                                    TipItem(
                                        emoji = "⏰",
                                        title = "Unyielding Regular Hours",
                                        desc = "Maintain strict wake-up times (even on weekends). Consistency encodes deep subconscious circadian triggers."
                                    )
                                    TipItem(
                                        emoji = "📵",
                                        title = "Shed Blue Screens Pre-Sleep",
                                        desc = "Put away screens 45 minutes before sleep. Blue photon exposure tricks your brain into seeing bright solar noon."
                                    )
                                    TipItem(
                                        emoji = "🧘",
                                        title = "GABA Wind-Down Routine",
                                        desc = "Incorporate 4-7-8 deep breathing exercises or a gentle stretching session to switch into parasympathetic mode."
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Call to action schedule settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoToScheduler() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Looking for bedtime alarms?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Configure recurring schedules for healthy sleep.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Preset Selection Custom Modal Dialog Sheet
    if (showPresetSheet) {
        AlertDialog(
            onDismissRequest = { showPresetSheet = false },
            title = {
                Text(
                    "Setup Sleep Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Configure both your sleep duration and countdown delay from preset configurations or custom selectors.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // 1. Durations presets picker
                    Text("1. Select Sleep Duration:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    val durations = listOf(
                        "20m Nap" to 20,
                        "30m Nap" to 30,
                        "60m Nap" to 60,
                        "90m Cycle" to 90,
                        "6 Hours" to 360,
                        "7 Hours" to 420,
                        "8 Hours" to 480
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durations.forEach { (label, mins) ->
                            FilterChip(
                                selected = selectedPresetMinutes == mins,
                                onClick = { selectedPresetMinutes = mins },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // 2. Start timer countdown delay
                    Text("2. Delay Startup Countdown:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    val delays = listOf(
                        "Immediate" to 0,
                        "5 Mins" to 5,
                        "10 Mins" to 10,
                        "30 Mins" to 30
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        delays.forEach { (label, mins) ->
                            FilterChip(
                                selected = selectedDelayMinutes == mins,
                                onClick = { selectedDelayMinutes = mins },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Custom input fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = if (selectedPresetMinutes <= 0) "" else selectedPresetMinutes.toString(),
                            onValueChange = { selectedPresetMinutes = it.toIntOrNull() ?: 0 },
                            label = { Text("Custom Duration (m)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = selectedDelayMinutes.toString(),
                            onValueChange = { selectedDelayMinutes = it.toIntOrNull() ?: 0 },
                            label = { Text("Custom Delay (m)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPresetMinutes <= 0) {
                            Toast.makeText(context, "Please select/type a sleep duration", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.startSleepSession(selectedPresetMinutes, selectedDelayMinutes)
                            showPresetSheet = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Bedtime Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetSheet = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ---------------- ALARMS SCHEDULER TAB ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchedulerScreen(viewModel: SleepViewModel) {
    val schedules by viewModel.allSchedules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var labelInput by remember { mutableStateOf("Bedtime") }
    var hourInput by remember { mutableStateOf(22) }
    var minuteInput by remember { mutableStateOf(30) }
    val daysSelection = remember { mutableStateListOf("Mon", "Tue", "Wed", "Thu", "Fri") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_schedule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "My Bedtime Alarms",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your device will alert you with persistent alerts at these times to prepare you for healthy cycles.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (schedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AlarmOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No active sleep schedules.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            "Tap + to configure one now.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(schedules) { schedule ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (schedule.active) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = formatTime(schedule.hour, schedule.minute),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (schedule.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                        )
                                        Text(
                                            text = schedule.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = schedule.active,
                                            onCheckedChange = { viewModel.toggleSchedule(schedule) }
                                        )
                                        IconButton(onClick = { viewModel.deleteSchedule(schedule) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete alarm",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Days list chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                    val alarmDays = schedule.daysOfWeek.split(",")
                                    daysList.forEach { dayName ->
                                        val matches = alarmDays.contains(dayName)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (matches && schedule.active) MaterialTheme.colorScheme.primary.copy(0.3f)
                                                    else MaterialTheme.colorScheme.onSurface.copy(0.06f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = dayName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (matches && schedule.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Bedtime Schedule", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Alarm Label") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Input Hour / Minute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = hourInput.toString(),
                            onValueChange = { hourInput = (it.toIntOrNull() ?: 0).coerceIn(0, 23) },
                            label = { Text("Hour (0-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minuteInput.toString(),
                            onValueChange = { minuteInput = (it.toIntOrNull() ?: 0).coerceIn(0, 59) },
                            label = { Text("Minute (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Days selector
                    Text("Select Days:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysList.forEach { day ->
                            val isSelected = daysSelection.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        daysSelection.remove(day)
                                    } else {
                                        daysSelection.add(day)
                                    }
                                },
                                label = { Text(day, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (daysSelection.isEmpty()) {
                            Toast.makeText(viewModel.getApplication(), "Select at least one day", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addSchedule(labelInput, hourInput, minuteInput, daysSelection)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------- HISTORY & ANALYTICS TAB ----------------
@Composable
fun AnalyticsScreen(viewModel: SleepViewModel) {
    val allSessions by viewModel.allSessions.collectAsState()
    val stats = remember(allSessions) { viewModel.getSleepStats() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sleep Habits & Insights",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Track your long term consistency, recover sleep debt, and align with natural sleep science schedules below.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }

        // Aggregate Score cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Consistency", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text("${stats.consistencyScore}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Completed vs set", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sleep Debt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text(String.format("%.1f hrs", stats.sleepDebtHours), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Text("Relative to 8h goal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }
        }

        // ---------------- COGNITIVE REPORTS & BILLING EXPORT CENTER ----------------
        item {
            val context = LocalContext.current
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep_report_export_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🖨️ REPORTS & SOCIAL EXPORTS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate printable executive reports or a playful sleep invoice outlining your restful sync credits. Instantly share to social platforms, chats, or cloud storage.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action 1: Standard TXT Report
                        Button(
                            onClick = {
                                try {
                                    val file = SleepReportExporter.generateTextReport(context, stats, allSessions)
                                    SleepReportExporter.shareGeneratedReport(context, file, "text/plain", "Sleep Cycle Assessment Report")
                                    Toast.makeText(context, "Text Report Generated & Shared! 📝", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generating report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Description, contentDescription = "TXT Report", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("TXT Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action 2: Playful Cognitive Rest Invoice
                        Button(
                            onClick = {
                                try {
                                    val file = SleepReportExporter.generateInvoiceReport(context, stats, allSessions)
                                    SleepReportExporter.shareGeneratedReport(context, file, "text/plain", "Cognitive Sleep Performance Invoice")
                                    Toast.makeText(context, "Playful Invoice Statement Shared! 🧾", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generating invoice: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Receipt, contentDescription = "Rest Invoice", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Rest Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action 3: Professional PDF Document
                        Button(
                            onClick = {
                                try {
                                    val file = SleepReportExporter.generatePdfReport(context, stats, allSessions)
                                    SleepReportExporter.shareGeneratedReport(context, file, "application/pdf", "Executive Circadian Performance Report")
                                    Toast.makeText(context, "PDF Executive Report Shared! 📄", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Report", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("PDF Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Smart Bedtime Suggestions / Calculator Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Smart Sleep Calculator", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Humans sleep in 90-minute cycle increments. To wake up fully refreshed, aim for these natural windows based on when you fall asleep:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val suggestions = listOf(
                        "4.5 Hours" to "Power Sleep (3 cycles). Ideal for busy nights.",
                        "6.0 Hours" to "Essential Sleep (4 cycles). Solid recovery.",
                        "7.5 Hours" to "Optimal Sleep (5 cycles). Zero morning grogginess!"
                    )

                    suggestions.forEach { (label, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        }
                    }
                }
            }
        }

        // Simple List of Logs
        item {
            Text(
                text = "Sleep Log History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (allSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No historic logs. Complete your first session to see entries here.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(allSessions) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        when (log.status) {
                                            "COMPLETED" -> MaterialTheme.colorScheme.primary.copy(0.12f)
                                            "CANCELLED" -> MaterialTheme.colorScheme.error.copy(0.12f)
                                            else -> MaterialTheme.colorScheme.tertiary.copy(0.12f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.status) {
                                        "COMPLETED" -> Icons.Default.Check
                                        "CANCELLED" -> Icons.Default.Close
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = when (log.status) {
                                        "COMPLETED" -> MaterialTheme.colorScheme.primary
                                        "CANCELLED" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Slept: ${log.durationMinutes} minutes (delay: ${log.delayMinutes}m)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteSessionById(log.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete log", modifier = Modifier.size(18.dp), tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- ALARMS & BACKUP SETTINGS TAB ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SleepViewModel) {
    val context = LocalContext.current
    val clip = LocalClipboardManager.current

    val selectedThemePreset by viewModel.themePreset.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val soundId by viewModel.selectedSoundId.collectAsState()
    val customText by viewModel.customTtsText.collectAsState()
    val playingPremadePreviewId by viewModel.playingPremadePreviewId.collectAsState()

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Application Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Personalize colors, voices, backup records, and export history.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }

        // Custom Themes Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. Visual Themes & Tone",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Mode Enabled", fontSize = 13.sp)
                        Switch(checked = isDark, onCheckedChange = { viewModel.setDarkTheme(it) })
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Active Pastel Themes Palette:", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppThemePreset.values().forEach { preset ->
                            val isSelected = selectedThemePreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectTheme(preset) },
                                label = { Text(preset.displayName, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Custom Sound Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "2. Customized Wake-Up Audio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select synthesized ambient vibes or premium voice narrations.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    val soundsList = listOf(
                        "Soft Chime Alarm" to "sound_soft_alarm",
                        "Steady Soft Rain" to "sound_rain",
                        "Rolling Ocean Waves" to "sound_ocean",
                        "Intermittent Chirps" to "sound_birds",
                        "Gentle Wake Narrator (TTS)" to "voice_gentle",
                        "Calm Deep Guide (TTS)" to "voice_narrator",
                        "Female Wake Call (TTS)" to "voice_female",
                        "Male Routine Call (TTS)" to "voice_male",
                        "Rise & Conqer (TTS)" to "voice_motivational",
                        "Custom Imported Audio 🎵" to "custom_audio"
                    )

                    soundsList.forEach { (label, sId) ->
                        val isPreviewing = playingPremadePreviewId == sId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectSound(sId) }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isPreviewing) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            CircleShape
                                        )
                                        .clickable {
                                            if (isPreviewing) {
                                                viewModel.stopPremadePreview()
                                            } else {
                                                viewModel.playPremadePreview(sId)
                                            }
                                        }
                                        .testTag("premade_preview_btn_$sId"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview $label",
                                        tint = if (isPreviewing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = label, 
                                    fontSize = 13.sp, 
                                    fontWeight = if (soundId == sId) FontWeight.Bold else FontWeight.Normal,
                                    color = if (soundId == sId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            RadioButton(
                                selected = soundId == sId, 
                                onClick = { viewModel.selectSound(sId) },
                                modifier = Modifier.testTag("premade_radio_$sId")
                            )
                        }
                    }

                    // Storage and Import Custom Audio
                    val customAudioUriString by viewModel.customAudioUri.collectAsState()

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Device Storage Audio Integration",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Permit access to import a custom offline .mp3 or .wav song from your phone's storage to use as your sleep session alarm.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    var hasStoragePermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val storagePermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasStoragePermission = isGranted
                        if (isGranted) {
                            Toast.makeText(context, "Storage access granted! You can now import audio files.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Storage access denied. App will fallback to built-in tones.", Toast.LENGTH_LONG).show()
                        }
                    }

                    val audioPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let {
                            try {
                                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                            } catch (e: Exception) {
                                // Some platforms might not support taking persistable permission, ignore or log
                            }
                            viewModel.setCustomAudioUri(it.toString())
                            viewModel.selectSound("custom_audio")
                            Toast.makeText(context, "Custom audio file imported successfully! 🎶", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (!hasStoragePermission) {
                        Button(
                            onClick = { storagePermissionLauncher.launch(storagePermission) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Storage Access & Import", fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import Audio File", fontSize = 12.sp)
                            }

                            if (customAudioUriString != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.setCustomAudioUri(null)
                                        if (soundId == "custom_audio") {
                                            viewModel.selectSound("sound_soft_alarm")
                                        }
                                        Toast.makeText(context, "Custom audio cleared.", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (customAudioUriString != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val displayName = try {
                                        val resolvedName = Uri.parse(customAudioUriString).path?.substringAfterLast("/")
                                        if (resolvedName.isNullOrBlank()) "imported_audio.mp3" else resolvedName
                                    } catch (e: Exception) {
                                        "imported_audio.mp3"
                                    }
                                    Text(
                                        text = "Active: $displayName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }

                            // TRIM & PREVIEW SLIDERS PANEL
                            val customStartMs by viewModel.customAudioStartMs.collectAsState()
                            val customEndMs by viewModel.customAudioEndMs.collectAsState()
                            val customDurationMs by viewModel.customAudioDurationMs.collectAsState()
                            var isPlayingPreview by remember { mutableStateOf(false) }

                            DisposableEffect(Unit) {
                                onDispose {
                                    viewModel.stopPreview()
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("custom_audio_trim_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "✂️ TRIM & PREVIEW AUDIO",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "Configure start and end wake timestamps",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isPlayingPreview) {
                                                    viewModel.stopPreview()
                                                    isPlayingPreview = false
                                                } else {
                                                    viewModel.playPreview(customAudioUriString!!, customStartMs, customEndMs)
                                                    isPlayingPreview = true
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).testTag("custom_audio_preview_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingPreview) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircle,
                                                contentDescription = "Preview Audio",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Local Helper Format Time function
                                    val formatTime = remember {
                                        { ms: Long ->
                                            val totalSecs = ms / 1000
                                            val mins = totalSecs / 60
                                            val secs = totalSecs % 60
                                            String.format("%02d:%02d", mins, secs)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Trim Start: ${formatTime(customStartMs)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Max: ${formatTime(customDurationMs)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    // Start Trim Slider
                                    Slider(
                                        value = customStartMs.toFloat(),
                                        onValueChange = { newVal ->
                                            val maxValAllowed = customEndMs - 1000L
                                            val adjVal = newVal.toLong().coerceAtMost(maxValAllowed.coerceAtLeast(0L))
                                            viewModel.updateCustomAudioTrim(adjVal, customEndMs)
                                            if (isPlayingPreview) {
                                                viewModel.playPreview(customAudioUriString!!, adjVal, customEndMs)
                                            }
                                        },
                                        valueRange = 0f..customDurationMs.toFloat().coerceAtLeast(1000f),
                                        modifier = Modifier.fillMaxWidth().testTag("custom_audio_start_slider")
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Trim End: ${formatTime(customEndMs)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val activeLen = (customEndMs - customStartMs).coerceAtLeast(0L)
                                        Text(
                                            text = "Trimmed Loop: ${formatTime(activeLen)}s",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // End Trim Slider
                                    Slider(
                                        value = customEndMs.toFloat(),
                                        onValueChange = { newVal ->
                                            val minValAllowed = customStartMs + 1000L
                                            val adjVal = newVal.toLong().coerceIn(minValAllowed..customDurationMs)
                                            viewModel.updateCustomAudioTrim(customStartMs, adjVal)
                                            if (isPlayingPreview) {
                                                viewModel.playPreview(customAudioUriString!!, customStartMs, adjVal)
                                            }
                                        },
                                        valueRange = 0f..customDurationMs.toFloat().coerceAtLeast(1000f),
                                        modifier = Modifier.fillMaxWidth().testTag("custom_audio_end_slider")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text input for personalized speech
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { viewModel.updateCustomTts(it) },
                        label = { Text("Custom Text-to-Speech Message", fontSize = 12.sp) },
                        placeholder = { Text("E.g., Good morning champion! Your planned 8 hours sleep cycle finished.") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }
            }
        }

        // Backup and Restore Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "3. Database Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Warden your files offline. Export to JSON or Restore history easily.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val jsonStr = viewModel.backupData()
                                clip.setText(AnnotatedString(jsonStr))
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.wipeDatabase()
                            Toast.makeText(context, "Database logs wiped!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All Log History", fontSize = 11.sp)
                    }
                }
            }
        }

        // Developer Credits Footer (Mandated)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Made with ❤️ by Rahul Shah",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Version 1.0.0 (Offline First Stable)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Import JSON Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the JSON string backup previously copied to your clipboard:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        label = { Text("JSON Data") },
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreData(restoreInput) { success ->
                            if (success) {
                                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                                showRestoreDialog = false
                            } else {
                                Toast.makeText(context, "Parsing error: Invalid JSON structure", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Restore Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------- RINGING SCREEN ----------------
@Composable
fun RingingOverlayScreen(
    session: SleepSession?,
    viewModel: SleepViewModel
) {
    val cycleMins = session?.durationMinutes ?: 480
    val nowFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03001C)) // Pure space theme ambient background for ringing
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing orbital circles
        val infiniteTransition = rememberInfiniteTransition(label = "ring_glow")
        val ringFactor by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_glow_factor"
        )

        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(ringFactor)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFFB3B3).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = Color(0xFFD4ADFC),
                modifier = Modifier
                    .size(82.dp)
                    .padding(bottom = 12.dp)
            )

            Text(
                text = "Rise and Shine!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Alarm trigger: $nowFormatted",
                fontSize = 18.sp,
                color = Color(0xFFAFA0FF),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Information Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.08f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Congratulations!\nYou successfully completed your scheduled $cycleMins-minute rest cycle.",
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Large dismiss / snooze Pill buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.stopAlarmSound() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DCD91)),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                        .testTag("dismiss_alarm_button"),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dismiss", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        // Triggers snooze through viewmodel, acts like 5 min timer
                        val now = System.currentTimeMillis()
                        viewModel.startSleepSession(5, 0)
                        viewModel.stopAlarmSound()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                        .testTag("snooze_alarm_button"),
                    border = BorderStroke(2.dp, Color.White),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Snooze, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snooze (5 Mins)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- FORMATTING HELPERS ----------------
fun formatSeconds(total: Long): String {
    val hrs = total / 3600
    val mins = (total % 3600) / 60
    val secs = total % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val adjustedHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%02d:%02d %s", adjustedHour, minute, amPm)
}

@Composable
fun TipItem(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

// ---------------- AMBIENT NATURE SOUNDS SCREEN ----------------

@Composable
fun SoundVisualizer(isPlaying: Boolean) {
    if (!isPlaying) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Silent",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_pulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(70.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulse2)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(54.dp)
                .scale(pulse1)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Playing",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AmbientSoundsScreen(viewModel: SleepViewModel) {
    val playingAmbientId by viewModel.playingAmbientId.collectAsState()
    val ambientRemainingSec by viewModel.ambientTimeRemainingSec.collectAsState()
    val ambientDuration by viewModel.ambientDurationMinutes.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "NATURE AMBIENT SOUNDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sleep Ambience Maker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Listen to curated synthetic soundtracks with custom timers to help you unwind and rest.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active Player Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ambient_active_player_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (playingAmbientId != null) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (playingAmbientId != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
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
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        SoundVisualizer(isPlaying = playingAmbientId != null)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (playingAmbientId != null) {
                                val soundName = when (playingAmbientId) {
                                    "sound_rain" -> "Rain Storm 🌧️"
                                    "sound_ocean" -> "Ocean Waves 🌊"
                                    "sound_birds" -> "Forest Birds 🐦"
                                    "sound_white_noise" -> "Steady White Noise 💨"
                                    "sound_soft_alarm" -> "Calming Chimes 🔔"
                                    "sound_wind" -> "Gentle Breezes 🍃"
                                    "sound_thunder" -> "Cosmic Thunderstorm ⛈️"
                                    "sound_stream" -> "Babbling Brook 🌊"
                                    "sound_frogs" -> "Summer Frogs 🐸"
                                    "sound_campfire" -> "Crackling Campfire 🔥"
                                    "sound_custom_mix" -> "Custom Sound Mix 🎛️"
                                    else -> "Nature Sound"
                                }
                                Text(
                                    text = soundName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val remainingText = if (ambientRemainingSec > 0) {
                                    val mins = ambientRemainingSec / 60
                                    val secs = ambientRemainingSec % 60
                                    String.format("%02d:%02d remaining", mins, secs)
                                } else {
                                    "Playing continuously (Infinite)"
                                }
                                Text(
                                    text = remainingText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            } else {
                                Text(
                                    text = "Ready to Play",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pick a sound and duration below",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (playingAmbientId != null) {
                        FilledIconButton(
                            onClick = { viewModel.stopAmbientSound() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("ambient_stop_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Ambient Sound",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Custom Duration Selection Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("ambient_timer_duration_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⏱️ SELECT SLEEP TIMER DURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val durations = listOf(
                        5 to "5 Min",
                        15 to "15 Min",
                        30 to "30 Min",
                        60 to "60 Min",
                        -1 to "Infinite ♾️"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durations.forEach { (mins, displayText) ->
                            val isSelected = ambientDuration == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable {
                                        viewModel.updateAmbientDuration(mins)
                                        // If already playing, restart with new duration
                                        val playingId = playingAmbientId
                                        if (playingId != null) {
                                            viewModel.playAmbientSound(playingId)
                                        }
                                    }
                                    .border(
                                        1.dp, 
                                        if (isSelected) Color.Transparent 
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom mixer card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ambient_custom_mixer_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎛️ CUSTOM SOUND EFFECTS MIXER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EXPERIMENTAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Design your perfect sleep soundscapes in real-time. Slide to blend individual physical waves.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val windVal by viewModel.customWindVol.collectAsState()
                    val rainVal by viewModel.customRainVol.collectAsState()
                    val streamVal by viewModel.customStreamVol.collectAsState()
                    val campfireVal by viewModel.customCampfireVol.collectAsState()
                    val chimesVal by viewModel.customChimesVol.collectAsState()

                    CustomMixSliderRow(label = "Gentle Wind 🍃", value = windVal, onValueChange = viewModel::updateCustomWindVol)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomMixSliderRow(label = "Forest Rain 🌧️", value = rainVal, onValueChange = viewModel::updateCustomRainVol)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomMixSliderRow(label = "Flowing Stream 🌊", value = streamVal, onValueChange = viewModel::updateCustomStreamVol)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomMixSliderRow(label = "Campfire Crackle 🔥", value = campfireVal, onValueChange = viewModel::updateCustomCampfireVol)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomMixSliderRow(label = "Peaceful Chimes 🔔", value = chimesVal, onValueChange = viewModel::updateCustomChimesVol)

                    Spacer(modifier = Modifier.height(14.dp))

                    val isPlayingCustom = playingAmbientId == "sound_custom_mix"
                    Button(
                        onClick = {
                            if (isPlayingCustom) {
                                viewModel.stopAmbientSound()
                            } else {
                                viewModel.playAmbientSound("sound_custom_mix")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("play_custom_mix_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingCustom) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingCustom) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlayingCustom) "STOP CUSTOM AMBIENT MIX" else "START CUSTOM AMBIENT MIX",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Curated Sound List Header
        item {
            Text(
                text = "🎵 CURATED NATURE SOUNDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        val natureSounds = listOf(
            NatureSoundItem("sound_rain", "Rain Storm 🌧️", "curated_rain_storm", "High density, filtered drop sounds forming a steady relaxing downpour."),
            NatureSoundItem("sound_ocean", "Ocean Waves 🌊", "curated_ocean_waves", "Periodic slow volume-modulated low-frequency rolling ocean swell."),
            NatureSoundItem("sound_birds", "Forest Birds 🐦", "curated_forest_birds", "Pleasant intermittent frequency-swept birds chirping on a soft field."),
            NatureSoundItem("sound_wind", "Gentle Breezes 🍃", "curated_gentle_wind", "A soft, whistling wind blowing across leaves and rolling grass."),
            NatureSoundItem("sound_thunder", "Cosmic Thunderstorm ⛈️", "curated_cosmic_thunder", "Soothing distant rumbling thunder cracks with light rain showers."),
            NatureSoundItem("sound_stream", "Babbling Brook 🌊", "curated_babbling_stream", "The rich, rhythmic sound of sweet clear water rolling over smooth stones."),
            NatureSoundItem("sound_frogs", "Summer Frogs 🐸", "curated_summer_frogs", "Intermittent twilight tree frogs chirping softly in a slow-paced rhythmic cadence."),
            NatureSoundItem("sound_campfire", "Crackling Campfire 🔥", "curated_wood_fire", "A comforting wood fire with sparks crackling and deep glowing warmth."),
            NatureSoundItem("sound_soft_alarm", "Calming Chimes 🔔", "curated_harmony_chimes", "Evolving five-note melodic chiming bells in a gentle relaxing rhythm."),
            NatureSoundItem("sound_white_noise", "Steady White Noise 💨", "curated_white_noise", "Constant, smooth static isolation frequency to mask background echo.")
        )

        items(natureSounds) { sound ->
            val isPlayingThis = playingAmbientId == sound.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sound_card_${sound.tag}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPlayingThis) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else 
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.61f)
                ),
                border = BorderStroke(
                    width = if (isPlayingThis) 2.dp else 1.dp,
                    color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sound.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sound.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isPlayingThis) {
                                viewModel.stopAmbientSound()
                            } else {
                                viewModel.playAmbientSound(sound.id)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingThis) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = if (isPlayingThis) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("play_btn_${sound.tag}")
                    ) {
                        Text(
                            text = if (isPlayingThis) "STOP" else "PLAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class NatureSoundItem(
    val id: String,
    val name: String,
    val tag: String,
    val description: String
)

@Composable
fun CustomMixSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "${value.toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..100f,
                modifier = Modifier.height(24.dp)
            )
        }
    }
}
