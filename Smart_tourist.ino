#include <BluetoothSerial.h>
#include <TinyGPSPlus.h>
#include <HardwareSerial.h>

BluetoothSerial SerialBT;
TinyGPSPlus gps;

// UART setup
HardwareSerial gpsSerial(1);   // GPS
HardwareSerial sim800(2);      // SIM800L

#define MQ135_PIN 34

String received = "";

// ================= SETUP =================
void setup() {
  Serial.begin(115200);

  // Bluetooth
  SerialBT.begin("ESP32");
  Serial.println("Bluetooth Started...");

  // GPS
  gpsSerial.begin(9600, SERIAL_8N1, 4, 5);

  // SIM800L
  sim800.begin(9600, SERIAL_8N1, 16, 17);

  delay(3000);

  // SIM init
  sim800.println("AT");
  delay(1000);
  sim800.println("AT+CMGF=1");
  delay(1000);
  sim800.println("AT+CSCS=\"GSM\"");
  delay(1000);

  Serial.println("System Ready...");
}

// ================= LOOP =================
void loop() {

  // 🔥 PRIORITY 1: READ BLUETOOTH (VERY IMPORTANT)
  while (SerialBT.available()) {
    char c = SerialBT.read();

    if (c == '\n') {

      received.trim();
      Serial.println("Received: " + received);

      if (received.startsWith("SOS")) {

        int comma1 = received.indexOf(',');
        int comma2 = received.indexOf(',', comma1 + 1);
        int comma3 = received.indexOf(',', comma2 + 1);

        if (comma1 > 0 && comma2 > 0 && comma3 > 0) {

          String number = received.substring(comma1 + 1, comma2);
          String lat = received.substring(comma2 + 1, comma3);
          String lon = received.substring(comma3 + 1);

          Serial.println("Number: " + number);
          Serial.println("Lat: " + lat);
          Serial.println("Lon: " + lon);

          sendSMS(number, lat, lon);
        }
      }

      received = "";

    } else {
      received += c;
    }
  }

  // 🔵 PRIORITY 2: GPS UPDATE
  while (gpsSerial.available()) {
    gps.encode(gpsSerial.read());
  }

  // 🔵 PRIORITY 3: SEND AQI + GPS (NON-BLOCKING)
  static unsigned long lastSend = 0;

  if (millis() - lastSend > 2000) {

    if (gps.location.isValid()) {

      float lat = gps.location.lat();
      float lon = gps.location.lng();

      int raw = analogRead(MQ135_PIN);
      int aqi = map(raw, 0, 4095, 0, 500);

      String data = "LAT:" + String(lat, 6) +
                    ",LON:" + String(lon, 6) +
                    ",AQI:" + String(aqi);

      SerialBT.println(data);
      Serial.println(data);
    }

    lastSend = millis();
  }

  // 🔵 DEBUG SIM RESPONSES
  while (sim800.available()) {
    Serial.write(sim800.read());
  }
}

// ================= SEND SMS =================
void sendSMS(String number, String lat, String lon) {

  Serial.println("Sending SOS...");
  Serial.println("To: " + number);

  // Ensure SMS mode
  sim800.println("AT+CMGF=1");
  delay(1000);

  // Start SMS
  sim800.print("AT+CMGS=\"");
  sim800.print(number);
  sim800.println("\"");
  delay(3000);   // VERY IMPORTANT

  // Message content
  sim800.print("🚨 EMERGENCY! I need help.\n");

  if (lat != "0" && lon != "0") {
    sim800.print("Location: https://maps.google.com/?q=");
    sim800.print(lat);
    sim800.print(",");
    sim800.print(lon);
  } else {
    sim800.print("Location not available");
  }

  delay(2000);

  // Send SMS
  sim800.write(26);  // CTRL+Z

  delay(5000);

  Serial.println("SMS Sent Successfully!");
}