package com.example.prayerautomute

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.prayerautomute.ui.theme.PrayerAutoMuteTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prayerautomute.viewmodel.PrayerAutoMuteViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            PrayerAutoMuteTheme {
                PrayerAutoMuteApp()
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerAutoMuteApp(
    viewModel: PrayerAutoMuteViewModel = viewModel()
) {
    val location by viewModel.location
    val prayerTimes by viewModel.prayerTimes
    val currentTime by viewModel.currentTime
    val nextPrayer by viewModel.nextPrayer
    val timeUntilNext by viewModel.timeUntilNext
    val isEnabled by viewModel.isEnabled
    val isMuted by viewModel.isMuted
    val muteEndTime by viewModel.muteEndTime

    var showLocationDialog by remember { mutableStateOf(false) }
    var showTimeEditDialog by remember { mutableStateOf(false) }
    var editingPrayerIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.startTimeUpdates()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFBBDEFB)
                    )
                )
            )
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1976D2)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Prayer Mute",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-mute during prayer times",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Status Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-mute",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isMuted) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Phone is muted",
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                muteEndTime?.let {
                                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    Text(
                                        text = "Will unmute at ${sdf.format(it)}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                            TextButton(onClick = { viewModel.unmutePhone() }) {
                                Text("Unmute")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = sdf.format(Date()),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Location Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { showLocationDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${location.city}, ${location.country}",
                        fontSize = 14.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Next Prayer Card
        nextPrayer?.let { next ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Next Prayer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E8)
                        )
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
                                    text = next.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = next.time,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "in",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = timeUntilNext,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Prayer Times Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Prayer Times",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                editingPrayerIndex = 0
                                showTimeEditDialog = true
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(prayerTimes.withIndex().toList()) { (index, prayer) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    editingPrayerIndex = index
                                    showTimeEditDialog = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFAFAFA)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prayer.name,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = prayer.time,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Test Button
        Button(
            onClick = { viewModel.mutePhone() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color.Black
            )
        ) {
            Text("Test Mute (20 minutes)")
        }
    }

    // Dialogs
    if (showLocationDialog) {
        var city by remember { mutableStateOf(location.city) }
        var country by remember { mutableStateOf(location.country) }

        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Edit Location") },
            text = {
                Column {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateLocation(city, country)
                        showLocationDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimeEditDialog) {
        var time by remember { mutableStateOf(prayerTimes[editingPrayerIndex].time) }

        AlertDialog(
            onDismissRequest = { showTimeEditDialog = false },
            title = { Text("Edit ${prayerTimes[editingPrayerIndex].name} Time") },
            text = {
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (HH:MM)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("12:30") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePrayerTime(editingPrayerIndex, time)
                        showTimeEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}