package com.example.smarttourist;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import com.example.smarttourist.AirQualityActivity;
import com.example.smarttourist.SOSActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button btnCheckAir, btnSendSOS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCheckAir = findViewById(R.id.btnCheckAir);
        btnSendSOS = findViewById(R.id.btnSendSOS);
        btnCheckAir.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,AirQualityActivity.class)));
        btnSendSOS.setOnClickListener(v -> startActivity(new Intent (MainActivity.this,SOSActivity.class)));
    }
}