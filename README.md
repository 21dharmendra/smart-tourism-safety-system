# 🚨 Smart Tourist Safety System

An IoT-based tourist safety system designed to provide **emergency SOS communication, GPS location sharing, and air-quality monitoring**, including support for low/no-network environments.

## 📌 Project Overview

The system combines an **ESP32**, **SIM800L GSM module**, **NEO-6M GPS**, **MQ135 air-quality sensor**, Bluetooth communication, and an Android application.

The Android app communicates with the ESP32 over Bluetooth. The ESP32 collects sensor/GPS information and can use the SIM800L module for emergency SMS/calling.

## ✨ Features

- 🆘 Emergency SOS
- 📡 Bluetooth communication between Android and ESP32
- 📍 GPS location tracking
- 📞 GSM calling and SMS using SIM800L
- 🌫️ Air-quality monitoring (AQI)
- 📱 Android application
- 🌐 Designed for low/no-network tourist areas

## 🔧 Hardware

- ESP32-WROOM-32
- MQ135 Air Quality Sensor
- NEO-6M GPS Module
- SIM800L GSM Module
- GSM Antenna
- DHT22
- Battery / power supply components

## 💻 Software & Technologies

- Android Studio
- Java
- XML
- Arduino IDE
- ESP32
- Bluetooth Serial (SPP)
- TinyGPSPlus
- GSM AT Commands

## 📂 Repository Structure

```
smart-tourism-safety-system/
├── Android-App/       # Android Studio source code
├── Smart_tourist.ino  # ESP32 firmware
├── STA.apk            # Android APK
└── README.md
```

## 📱 Android Application

The Android application contains:

- Splash screen
- Main dashboard
- Air Quality screen
- Emergency SOS screen
- Bluetooth communication with ESP32
- GPS/location handling

### Android source code

The complete source code is available in the **Android-App** directory.

## 🔄 Working Flow

```
Android App
     │
     │ Bluetooth
     ▼
   ESP32
   ┌──┼─────────────┐
   │  │             │
   ▼  ▼             ▼
 MQ135 GPS        SIM800L
   │  │             │
   ▼  ▼             ▼
 AQI Location   SMS / Call
```

## 🆘 SOS Workflow

1. User opens the Emergency SOS screen.
2. User enters the emergency contact number.
3. The Android app obtains the location when available.
4. SOS data is sent to the ESP32 through Bluetooth.
5. ESP32 communicates with the SIM800L module.
6. The GSM module sends the emergency message/call.

## 🌫️ Air Quality Workflow

1. MQ135 provides an air-quality reading to ESP32.
2. ESP32 processes the reading.
3. ESP32 sends AQI and GPS data through Bluetooth.
4. Android displays the AQI, status, latitude, longitude, and location name.

## 📥 APK

The compiled Android application is available as **STA.apk** in the repository.

> The APK is provided for demonstration/testing. The Android source code is also included so the project can be inspected and developed further.

## 🚀 Future Scope

- LoRa-based communication
- Cloud monitoring
- AI-based emergency detection
- Live location tracking
- Multi-language support

## 👨‍💻 Author

**Dharmesh Goswami**

BSc IT Final Year Project
