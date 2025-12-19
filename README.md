# ResQ - Personal Safety & SOS App 🚨

**ResQ** is a native Android application designed to ensure personal safety by broadcasting real-time location updates to trusted contacts during emergencies. Built using **Kotlin** and **Jetpack Compose**, it leverages Android's **Foreground Services** to function reliably even when the app is minimized or the screen is locked.

## 📱 Features

* **One-Tap SOS:** Instantly activates emergency mode with a single button.
* **Real-Time Location Tracking:** Fetches high-accuracy GPS coordinates using `FusedLocationProviderClient`.
* **Automated SMS Alerts:** Sends SMS messages containing a dynamic Google Maps link to trusted contacts every 10 seconds.
* **Background Execution:** Uses a **Foreground Service** with a persistent notification to ensure the OS does not kill the process during an emergency.
* **Contact Integration:** Seamlessly integrates with the device's phonebook using `ActivityResultLauncher` to pick trusted contacts without manual entry.
* **Local Persistence:** Stores trusted contacts securely using `SharedPreferences`.

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Concurrency:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
* **Architecture:** MVVM patterns & State Hoisting
* **Android Components:**
    * `Service` (Foreground)
    * `ContentResolver` (For Contacts)
    * `SmsManager`


## 🚀 Installation
Clone the repository:

Bash

git clone [https://github.com/SeenuBommisetti/ResQ.git](https://github.com/SeenuBommisetti/ResQ.git)

* Open the project in Android Studio.

* Sync Gradle files.

* Run on an emulator or physical device.

* Note: SMS functionality requires a physical device with an active SIM card.

**🔐 Permissions Required**
The app requests the following runtime permissions:

* ACCESS_FINE_LOCATION: To get precise GPS coordinates.
* SEND_SMS: To send emergency alerts automatically.
* POST_NOTIFICATIONS: To show the persistent foreground service notification (Android 13+).

---

**Developed by [Seenu Bommisetti](github.com/SeenuBommisetti)**
