package com.example.uidesign_cistercian;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ConversionResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cistercian_conversion_layout);

        // Get the number passed from MainActivity
        int number = getIntent().getIntExtra("convertedNumber", 0);

        // Find the TextView in the new layout and set the number
        TextView resultTextView = findViewById(R.id.resultTextView);
        resultTextView.setText(String.valueOf(number));
    }
}