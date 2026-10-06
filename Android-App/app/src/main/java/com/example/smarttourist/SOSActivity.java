package com.example.smarttourist;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class SOSActivity extends AppCompatActivity {
    Button btnSOS;
    EditText phoneInput;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket socket;
    OutputStream outputStream;
    private static final UUID UUID_BT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String DEVICE_NAME = "ESP32";
    LocationManager locationManager;
    boolean locationReceived = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);
        btnSOS = findViewById(R.id.btnSendSOS);
        phoneInput = findViewById(R.id.phoneInput);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (bluetoothAdapter == null) { Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show(); finish(); return; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        connectBluetooth();
        btnSOS.setOnClickListener(v -> {
            String number = phoneInput.getText().toString().trim();
            if (number.isEmpty()) { Toast.makeText(this, "Enter phone number", Toast.LENGTH_SHORT).show(); return; }
            if (number.length() < 10) { Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show(); return; }
            getLocationAndSendSOS(number);
        });
    }
    private void connectBluetooth() {
        new Thread(() -> {
            try {
                if (!bluetoothAdapter.isEnabled()) { runOnUiThread(() -> Toast.makeText(this, "Turn ON Bluetooth", Toast.LENGTH_SHORT).show()); return; }
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                BluetoothDevice device = null;
                for (BluetoothDevice d : pairedDevices) if (DEVICE_NAME.equals(d.getName())) { device = d; break; }
                if (device == null) { runOnUiThread(() -> Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_SHORT).show()); return; }
                socket = device.createRfcommSocketToServiceRecord(UUID_BT);
                socket.connect();
                outputStream = socket.getOutputStream();
                runOnUiThread(() -> Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show());
            } catch (Exception e) { runOnUiThread(() -> Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show()); }
        }).start();
    }
    private void getLocationAndSendSOS(String number) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();
        locationReceived = false;
        LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (!locationReceived) {
                    locationReceived = true;
                    sendSOSWithLocation(number, location.getLatitude(), location.getLongitude());
                    locationManager.removeUpdates(this);
                }
            }
        };
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        new Handler().postDelayed(() -> { if (!locationReceived) { Toast.makeText(this, "Using fallback (no GPS)", Toast.LENGTH_SHORT).show(); sendSOSWithoutLocation(number); } }, 5000);
    }
    private void sendSOSWithLocation(String number, double lat, double lon) {
        try {
            if (outputStream != null) { String msg = "SOS," + number + "," + lat + "," + lon + "\n"; outputStream.write(msg.getBytes()); Toast.makeText(this, "SOS Sent with Location", Toast.LENGTH_SHORT).show(); }
            else Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Send Failed", Toast.LENGTH_SHORT).show(); }
    }
    private void sendSOSWithoutLocation(String number) {
        try {
            if (outputStream != null) { String msg = "SOS," + number + ",0,0\n"; outputStream.write((msg + "\n").getBytes()); Toast.makeText(this, "SOS Sent (No Location)", Toast.LENGTH_SHORT).show(); }
            else Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "Send Failed", Toast.LENGTH_SHORT).show(); }
    }
    @Override protected void onDestroy() { super.onDestroy(); try { if (socket != null) socket.close(); } catch (IOException ignored) {} }
}