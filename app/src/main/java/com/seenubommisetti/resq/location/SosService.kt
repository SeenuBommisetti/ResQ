package com.seenubommisetti.resq.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.seenubommisetti.resq.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SosService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val UPDATE_INTERVAL = 10000L

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "sos_location_channel"
        var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSharing()
            ACTION_STOP -> stopSharing()
        }
        return START_STICKY
    }

    private fun startSharing() {
        if (isRunning) return
        isRunning = true

        startForeground(1, createNotification())

        serviceScope.launch {
            while (isRunning) {
                try {
                    val location = getLastLocation()
                    if (location != null) {
                        sendSosMessages(location)
                    } else {
                        Log.e("SOS", "Location was null")
                    }
                } catch (e: Exception) {
                    Log.e("SOS", "Error in loop: ${e.message}")
                }

                delay(UPDATE_INTERVAL)
            }
        }
    }

    private fun stopSharing() {
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun getLastLocation(): Location? {

        return try {
            val task = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )

            val result = CompletableDeferred<Location?>()
            task.addOnSuccessListener { result.complete(it) }
            task.addOnFailureListener { result.complete(null) }
            result.await()
        } catch (e: SecurityException) {
            null
        }
    }

    private fun sendSosMessages(location: Location) {
        val prefs = getSharedPreferences("sos_prefs", Context.MODE_PRIVATE)
        val contacts = prefs.getStringSet("contacts", emptySet()) ?: emptySet()
        val mapLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val message = "SOS! I need help. My current location is: $mapLink. Sent automatically via ResQ App."

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        for (number in contacts) {
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
                Log.d("SOS", "SMS sent to $number")
            } catch (e: Exception) {
                Log.e("SOS", "Failed to send SMS to $number: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SOS Location Sharing",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, SosService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ResQ SOS is Active")
            .setContentText("Sharing location with contacts every 10 seconds...")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .addAction(android.R.drawable.ic_delete, "STOP SHARING", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isRunning = false
    }
}