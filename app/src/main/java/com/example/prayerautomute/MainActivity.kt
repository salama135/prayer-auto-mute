package com.example.prayerautomute

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.example.prayerautomute.viewmodel.PrayerAutoMuteViewModel
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Handle permission denied
        }
    }
    
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted, but we can't directly check the result
        // We'll just proceed with scheduling alarms and Android will handle permission issues
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestNotificationPermission()
        checkDoNotDisturbPermission()

        setContent {
            PrayerAutoMuteApp()
        }
    }

    private fun checkDoNotDisturbPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // You can show a dialog here explaining why this permission is needed
                // For now, we'll just continue - the app will use vibrate mode instead
            }
        }
    }

    private fun requestDoNotDisturbPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Notifications"
            val descriptionText = "Notifications for background tasks"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("TASK_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general battery optimization settings
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(fallbackIntent)
            }
        }
    }
    
    // Function to request alarm permission on Android 13+
    fun requestAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+ (S)
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.parse("package:${context.packageName}")
            try {
                alarmPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                // Fallback if the specific intent fails
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                fallbackIntent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(fallbackIntent)
            }
        }
    }
}

@Composable
fun PrayerAutoMuteApp() {
    val context = LocalContext.current
    val viewModel: PrayerAutoMuteViewModel = viewModel()
    
    // Observe ViewModel states
    val isEnabled by viewModel.isEnabled
    val isMuted by viewModel.isMuted
    val alarmsScheduled by viewModel.alarmsScheduled
    val prayerTimes by viewModel.prayerTimes
    val nextPrayer by viewModel.nextPrayer
    val timeUntilNext by viewModel.timeUntilNext
    
    // Start time updates
    LaunchedEffect(key1 = true) {
        viewModel.startTimeUpdates()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Prayer Auto Mute",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Prayer times list
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Prayer Times",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                prayerTimes.forEachIndexed { index, prayer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = prayer.time,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (nextPrayer?.name == prayer.name) {
                            Text(
                                text = "Next ($timeUntilNext)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        
        // Enable/Disable Prayer Alarms
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Prayer Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { 
                        // Request alarm permissions if needed (Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isEnabled) {
                            // Get the MainActivity instance to access its launcher
                            val activity = context as? MainActivity
                            if (activity != null) {
                                activity.requestAlarmPermission(context)
                            }
                        }
                        viewModel.toggleEnabled(context) 
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Text(
                    text = if (isEnabled) "Alarms Enabled" else "Alarms Disabled",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (alarmsScheduled) {
                    Text(
                        text = "Alarms scheduled for all prayer times",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Test Mute Button
        Button(
            onClick = { viewModel.testMutePhone(context) },
            enabled = !isMuted,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Test Mute (1 min)")
        }
        
        if (isMuted) {
            Button(
                onClick = { viewModel.unmutePhone(context) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Unmute Now")
            }
        }

        // Add button to request Do Not Disturb permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Grant Do Not Disturb Permission")
                }

                Text(
                    text = "Grant permission for full mute functionality",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@SuppressLint("NewApi")
fun scheduleTaskChain(context: Context) {
    // Original functionality with improved constraints
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresCharging(false)
        .setRequiresDeviceIdle(false)
        .setRequiresBatteryNotLow(false) // Allow even on low battery
        .build()

    val taskARequest = OneTimeWorkRequestBuilder<MuteWorker>()
        .setInitialDelay(2, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.LINEAR, 30000L, TimeUnit.MILLISECONDS) // 30 seconds minimum
        .addTag("TASK_A")
        .build()

    val taskBRequest = OneTimeWorkRequestBuilder<UnmuteWorker>()
        .setInitialDelay(4, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.LINEAR, 30000L, TimeUnit.MILLISECONDS) // 30 seconds minimum
        .addTag("TASK_B")
        .build()

    WorkManager.getInstance(context).enqueue(taskARequest)
    WorkManager.getInstance(context).enqueue(taskBRequest)
}

@SuppressLint("NewApi")
fun scheduleDailyTasks(context: Context, muteHour: Int, muteMinute: Int, unmuteHour: Int, unmuteMinute: Int) {
    val workManager = WorkManager.getInstance(context)

    // Cancel any existing daily tasks
    workManager.cancelAllWorkByTag("DAILY_MUTE")
    workManager.cancelAllWorkByTag("DAILY_UNMUTE")

    // More lenient constraints for daily tasks
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresCharging(false)
        .setRequiresDeviceIdle(false)
        .setRequiresBatteryNotLow(false)
        .build()

    // Calculate initial delay for mute task
    val muteTime = LocalTime.of(muteHour, muteMinute)
    val muteDelay = calculateInitialDelay(muteTime)

    // Calculate initial delay for unmute task
    val unmuteTime = LocalTime.of(unmuteHour, unmuteMinute)
    val unmuteDelay = calculateInitialDelay(unmuteTime)

    // Schedule daily mute task with flexible repeat interval
    val dailyMuteRequest = PeriodicWorkRequestBuilder<MuteWorker>(24, TimeUnit.HOURS, 6, TimeUnit.HOURS)
        .setInitialDelay(muteDelay, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60000L, TimeUnit.MILLISECONDS) // 1 minute minimum
        .addTag("DAILY_MUTE")
        .build()

    // Schedule daily unmute task with flexible repeat interval
    val dailyUnmuteRequest = PeriodicWorkRequestBuilder<UnmuteWorker>(24, TimeUnit.HOURS, 6, TimeUnit.HOURS)
        .setInitialDelay(unmuteDelay, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60000L, TimeUnit.MILLISECONDS) // 1 minute minimum
        .addTag("DAILY_UNMUTE")
        .build()

    workManager.enqueue(dailyMuteRequest)
    workManager.enqueue(dailyUnmuteRequest)
}

@SuppressLint("NewApi")
fun calculateInitialDelay(targetTime: LocalTime): Long {
    val now = LocalDateTime.now()
    var targetDateTime = now.toLocalDate().atTime(targetTime)

    // If target time has already passed today, schedule for tomorrow
    if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
        targetDateTime = targetDateTime.plusDays(1)
    }

    return ChronoUnit.MINUTES.between(now, targetDateTime)
}

fun cancelDailyTasks(context: Context) {
    val workManager = WorkManager.getInstance(context)
    workManager.cancelAllWorkByTag("DAILY_MUTE")
    workManager.cancelAllWorkByTag("DAILY_UNMUTE")
}

fun cancelAllTasks(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag("TASK_A")
    WorkManager.getInstance(context).cancelAllWorkByTag("TASK_B")
    WorkManager.getInstance(context).cancelAllWorkByTag("DAILY_MUTE")
    WorkManager.getInstance(context).cancelAllWorkByTag("DAILY_UNMUTE")
}