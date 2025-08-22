package com.example.prayerautomute

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.prayerautomute.ui.theme.PrayerAutoMuteTheme
import com.example.prayerautomute.util.MuteStatusManager
import com.example.prayerautomute.viewmodel.PrayerAutoMuteViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerAutoMuteViewModel by viewModels()
    private lateinit var muteStatusManager: MuteStatusManager

    // Permission launchers
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handle result if needed */ }

    private val requestDndPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Handle result if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        muteStatusManager = MuteStatusManager.getInstance(this)
        checkAndRequestPermissions()

        setContent {
            PrayerAutoMuteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PrayerAutoMuteScreen(
                        viewModel = viewModel,
                        muteStatusManager = muteStatusManager
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check Do Not Disturb permission
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            !notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            requestDndPermission.launch(intent)
        }
    }
}

@Composable
fun PrayerAutoMuteScreen(
    viewModel: PrayerAutoMuteViewModel,
    muteStatusManager: MuteStatusManager
) {
    val context = LocalContext.current
    val currentTime by viewModel.currentTime
    val nextPrayer by viewModel.nextPrayer
    val timeUntilNext by viewModel.timeUntilNext
    val isEnabled by viewModel.isEnabled
    val isMuted by viewModel.isMuted
    val location by viewModel.location

    // Start time updates and status monitoring
    LaunchedEffect(Unit) {
        viewModel.startTimeUpdates()
    }

    // Monitor mute status from SharedPreferences
    LaunchedEffect(Unit) {
        while (true) {
            val currentlyMuted = muteStatusManager.isMuted
            val endTime = muteStatusManager.muteEndTime

            // Check if mute time has expired
            if (currentlyMuted && endTime > 0 && System.currentTimeMillis() > endTime) {
                muteStatusManager.clearMuteStatus()
                viewModel.updateMuteStatus(false)
            } else {
                viewModel.updateMuteStatus(currentlyMuted)
            }

            delay(1000) // Check every second
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current time display
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Time",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Location display
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${location.city}, ${location.country}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Next prayer display
        nextPrayer?.let { prayer ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Next Prayer",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${prayer.name} - ${prayer.time}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "In $timeUntilNext",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Status indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Auto Mute")
                    Text(if (isEnabled) "ON" else "OFF")
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMuted)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Phone Status")
                    Text(if (isMuted) "MUTED" else "NORMAL")
                }
            }
        }

        // Countdown timer when muted
        if (isMuted) {
            val endTime = muteStatusManager.muteEndTime
            if (endTime > 0) {
                val remainingTime = (endTime - System.currentTimeMillis()) / 1000
                if (remainingTime > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Time Remaining")
                            Text(
                                text = "${remainingTime}s",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }

        // Control buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleEnabled() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEnabled) "Disable Auto Mute" else "Enable Auto Mute")
            }

            Button(
                onClick = { viewModel.testMutePhone(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMuted
            ) {
                Text("Test Mute (1 minute)")
            }

            if (isMuted) {
                Button(
                    onClick = { viewModel.unmutePhone(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unmute Now")
                }
            }
        }
    }
}