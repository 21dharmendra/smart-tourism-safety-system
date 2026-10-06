package com.example.smarttourist;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AirQualityActivity extends AppCompatActivity {
    TextView aqiValue, aqiStatus, txtLocation;
    ProgressBar aqiProgress;
    Button btnBack;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    InputStream inputStream;
    Thread readThread;
    private static final int BT_PERMISSION_CODE = 101;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String DEVICE_NAME = "ESP32";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_quality);
        aqiValue = findViewById(R.id.aqiValue);
        aqiStatus = findViewById(R.id.aqiStatus);
        aqiProgress = findViewById(R.id.aqiProgress);
        txtLocation = findViewById(R.id.txtLocation);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        if (checkBluetoothPermission()) connectBluetooth();
    }
    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, BT_PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) connectBluetooth();
            else Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_LONG).show();
        }
    }
    private void connectBluetooth() {
        new Thread(() -> {
            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) { runOnUiThread(() -> Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()); return; }
                if (!bluetoothAdapter.isEnabled()) { runOnUiThread(() -> Toast.makeText(this, "Enable Bluetooth", Toast.LENGTH_LONG).show()); return; }
                BluetoothDevice esp32Device = null;
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) if (DEVICE_NAME.equals(device.getName())) { esp32Device = device; break; }
                if (esp32Device == null) { runOnUiThread(() -> Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_LONG).show()); return; }
                bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();
                runOnUiThread(() -> Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show());
                startReading();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Bluetooth error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                closeBluetooth();
            }
        }).start();
    }
    private void startReading() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            StringBuilder dataBuffer = new StringBuilder();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        dataBuffer.append(new String(buffer, 0, bytes));
                        int index;
                        while ((index = dataBuffer.indexOf("\n")) != -1) {
                            String line = dataBuffer.substring(0, index).trim();
                            dataBuffer.delete(0, index + 1);
                            updateAQIFromESP32(line);
                        }
                    }
                } catch (Exception e) { break; }
            }
        });
        readThread.start();
    }
    private void updateAQIFromESP32(String data) {
        try {
            if (!data.contains("AQI")) return;
            String[] parts = data.split(",");
            if (parts.length < 3) return;
            String lat = parts[0].split(":")[1];
            String lon = parts[1].split(":")[1];
            int aqi = Integer.parseInt(parts[2].split(":")[1]);
            runOnUiThread(() -> {
                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);
                new Thread(() -> {
                    String locationName = getLocationName(latitude, longitude);
                    runOnUiThread(() -> txtLocation.setText("📍 " + locationName + "\nLat: " + latitude + "\nLon: " + longitude));
                }).start();
                aqiValue.setText("AQI: " + aqi);
                aqiProgress.setProgress(aqi);
                if (aqi <= 50) aqiStatus.setText("Good");
                else if (aqi <= 100) aqiStatus.setText("Moderate");
                else if (aqi <= 200) aqiStatus.setText("Unhealthy");
                else aqiStatus.setText("Very Unhealthy");
            });
        } catch (Exception ignored) {}
    }
    private String getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                if (city == null) city = address.getSubAdminArea();
                String state = address.getAdminArea();
                String country = address.getCountryName();
                return city + ", " + state + ", " + country;
            }
        } catch (IOException ignored) {}
        return "Unknown Location";
    }
    private void closeBluetooth() {
        try { if (readThread != null) readThread.interrupt(); if (inputStream != null) inputStream.close(); if (bluetoothSocket != null) bluetoothSocket.close(); } catch (Exception ignored) {}
    }
    @Override protected void onDestroy() { super.onDestroy(); closeBluetooth(); }
}