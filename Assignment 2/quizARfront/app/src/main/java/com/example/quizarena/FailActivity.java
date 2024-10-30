package com.example.quizarena;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

public class FailActivity extends AppCompatActivity {
    private static final String TAG = "FailActivity";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_unsupported);

        Button againButton = findViewById(R.id.againButton);
        againButton.setOnClickListener(view -> {
            Intent intent = new Intent(FailActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
