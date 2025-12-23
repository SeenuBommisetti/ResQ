package com.seenubommisetti.resq.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.seenubommisetti.resq.data.model.ContactModel
import com.seenubommisetti.resq.location.SosService
import com.seenubommisetti.resq.ui.theme.ResQTheme

@Composable
fun ResQAppScreen(
    modifier: Modifier = Modifier,
    viewModel: SosViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val contacts by viewModel.contacts.collectAsState()

    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

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

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                val contact = retrieveContactDetails(context, contactUri)
                if (contact != null) {
                    viewModel.addContact(contact.name, contact.number)
                    Toast.makeText(context, "Added ${contact.name}", Toast.LENGTH_SHORT).show()
                }
            }
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
                    if (contacts.isEmpty()) {
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

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                }
                contactPickerLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Phone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick from Contacts")
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    onDelete = { viewModel.removeContact(contact) } // <--- ACTION TO VIEWMODEL
                )
            }
        }
    }
}

@Composable
fun ContactItem(contact: ContactModel, onDelete: () -> Unit) {
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = contact.number,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
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

fun retrieveContactDetails(context: Context, contactUri: Uri): ContactModel? {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val cursor = context.contentResolver.query(contactUri, projection, null, null, null)

    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
            val number = if (numberIndex >= 0) it.getString(numberIndex) else ""

            if (number.isNotBlank()) {
                return ContactModel(name, number)
            }
        }
    }
    return null
}

@Preview(showBackground = true)
@Composable
fun SosScreenPreview() {
    ResQTheme {
        ResQAppScreen()
    }
}