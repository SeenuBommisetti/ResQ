package com.seenubommisetti.resq.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.seenubommisetti.resq.location.SosService
import com.seenubommisetti.resq.ui.theme.ResQTheme

@Composable
fun ResQAppScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    var contactNumber by remember { mutableStateOf("") }
    var storedContacts by remember { mutableStateOf(loadContacts(context)) }


    val permissionsToRequest = mutableListOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            toggleSosService(context, !SosService.isRunning)
        } else {
            Toast.makeText(context, "Permissions required for SOS", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SOS EMERGENCY", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        Spacer(modifier = Modifier.height(20.dp))


        Button(
            onClick = {
                if (SosService.isRunning) {
                    toggleSosService(context, false)
                } else {
                    if (storedContacts.isEmpty()) {
                        Toast.makeText(context, "Add contacts first!", Toast.LENGTH_SHORT).show()
                    } else {
                        if (hasPermissions(context, permissionsToRequest)) {
                            toggleSosService(context, true)
                        } else {
                            permissionLauncher.launch(permissionsToRequest)
                        }
                    }
                }
            },
            modifier = Modifier.size(200.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (SosService.isRunning) Color.Gray else Color.Red
            )
        ) {
            Text(
                text = if (SosService.isRunning) "STOP" else "SOS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(if (SosService.isRunning) "Sending location every 10 seconds..." else "Tap to start sharing")

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))


        Text("Trusted Contacts", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = contactNumber,
                onValueChange = { contactNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (contactNumber.isNotBlank() && contactNumber.length == 10 && contactNumber[0] in '6'..'9') {
                    val newSet = storedContacts + contactNumber
                    saveContacts(context, newSet)
                    storedContacts = newSet
                    contactNumber = ""
                } else {
                    Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Add")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            items(storedContacts.toList()) { number ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(number, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val newSet = storedContacts - number
                            saveContacts(context, newSet)
                            storedContacts = newSet
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}


fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun toggleSosService(context: Context, shouldStart: Boolean) {
    val intent = Intent(context, SosService::class.java).apply {
        action = if (shouldStart) SosService.ACTION_START else SosService.ACTION_STOP
    }
    if (shouldStart) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        Toast.makeText(context, "SOS Started!", Toast.LENGTH_SHORT).show()
    } else {
        context.startService(intent)
        Toast.makeText(context, "SOS Stopped", Toast.LENGTH_SHORT).show()
    }
}

fun saveContacts(context: Context, contacts: Set<String>) {
    context.getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        .edit {
            putStringSet("contacts", contacts)
        }
}

fun loadContacts(context: Context): Set<String> {
    return context.getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        .getStringSet("contacts", emptySet()) ?: emptySet()
}

@Preview(showBackground = true)
@Composable
fun SosScreenPreview() {
    ResQTheme {
        ResQAppScreen()
    }
}