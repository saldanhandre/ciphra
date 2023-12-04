package com.example.uidesign_cistercian;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class ArabicConversionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arabic_conversion_layout);

        EditText inputNumberEditText = findViewById(R.id.inputNumberEditText);
        Button convertButton = findViewById(R.id.convertButton);

        Button openCistercianConversionLayoutButton = findViewById(R.id.openCistercianConversionLayoutButton);
        openCistercianConversionLayoutButton.setOnClickListener(v -> {
            finish(); // This will close the current activity and return to the previous one
        });

        convertButton.setOnClickListener(v -> {
            int number = Integer.parseInt(inputNumberEditText.getText().toString());

            // Break down the number into thousands, hundreds, tens, and units
            int thousands = number / 1000;
            int hundreds = (number % 1000) / 100;
            int tens = (number % 100) / 10;
            int units = number % 10;

            // Update the segments based on these values
            eraseSegments();
            updateSegments(thousands, hundreds, tens, units);
            System.out.println(thousands);
            System.out.println(hundreds);
            System.out.println(tens);
            System.out.println(units);
        });
    }

    private void eraseSegments(){
        findViewById(R.id.central_stem_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment3_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment3_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment4_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment4_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment5_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment6_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment7_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment8_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment8_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment9_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment9_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment10_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment11_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment12_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment13_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment13_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment14_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment14_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment15_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment16_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment17_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment18_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment18_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment19_1_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment19_2_res).setVisibility(View.INVISIBLE);
        findViewById(R.id.segment20_res).setVisibility(View.INVISIBLE);
    }
    private void updateSegments(int thousands, int hundreds, int tens, int units) {
        findViewById(R.id.central_stem_res).setVisibility(View.VISIBLE);
        switch(units){
            case 1:
                findViewById(R.id.segment1_res).setVisibility(View.VISIBLE);
                break;
            case 2:
                findViewById(R.id.segment2_res).setVisibility(View.VISIBLE);
                break;
            case 3:
                findViewById(R.id.segment3_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment3_2_res).setVisibility(View.VISIBLE);
                break;
            case 4:
                findViewById(R.id.segment4_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment4_2_res).setVisibility(View.VISIBLE);
                break;
            case 5:
                findViewById(R.id.segment1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment4_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment4_2_res).setVisibility(View.VISIBLE);
                break;
            case 6:
                findViewById(R.id.segment5_res).setVisibility(View.VISIBLE);
                break;
            case 7:
                findViewById(R.id.segment1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment5_res).setVisibility(View.VISIBLE);
                break;
            case 8:
                findViewById(R.id.segment2_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment5_res).setVisibility(View.VISIBLE);
                break;
            case 9:
                findViewById(R.id.segment1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment2_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment5_res).setVisibility(View.VISIBLE);
                break;
        }

        switch(tens){
            case 1:
                findViewById(R.id.segment6_res).setVisibility(View.VISIBLE);
                break;
            case 2:
                findViewById(R.id.segment7_res).setVisibility(View.VISIBLE);
                break;
            case 3:
                findViewById(R.id.segment8_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment8_2_res).setVisibility(View.VISIBLE);
                break;
            case 4:
                findViewById(R.id.segment9_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment9_2_res).setVisibility(View.VISIBLE);
                break;
            case 5:
                findViewById(R.id.segment6_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment9_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment9_2_res).setVisibility(View.VISIBLE);
                break;
            case 6:
                findViewById(R.id.segment10_res).setVisibility(View.VISIBLE);
                break;
            case 7:
                findViewById(R.id.segment6_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment10_res).setVisibility(View.VISIBLE);
                break;
            case 8:
                findViewById(R.id.segment7_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment10_res).setVisibility(View.VISIBLE);
                break;
            case 9:
                findViewById(R.id.segment6_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment7_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment10_res).setVisibility(View.VISIBLE);
                break;
        }

        switch(hundreds){
            case 1:
                findViewById(R.id.segment11_res).setVisibility(View.VISIBLE);
                break;
            case 2:
                findViewById(R.id.segment12_res).setVisibility(View.VISIBLE);
                break;
            case 3:
                findViewById(R.id.segment13_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment13_2_res).setVisibility(View.VISIBLE);
                break;
            case 4:
                findViewById(R.id.segment14_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment14_2_res).setVisibility(View.VISIBLE);
                break;
            case 5:
                findViewById(R.id.segment11_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment14_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment14_2_res).setVisibility(View.VISIBLE);
                break;
            case 6:
                findViewById(R.id.segment15_res).setVisibility(View.VISIBLE);
                break;
            case 7:
                findViewById(R.id.segment11_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment15_res).setVisibility(View.VISIBLE);
                break;
            case 8:
                findViewById(R.id.segment12_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment15_res).setVisibility(View.VISIBLE);
                break;
            case 9:
                findViewById(R.id.segment11_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment12_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment15_res).setVisibility(View.VISIBLE);
                break;
        }

        switch(thousands){
            case 1:
                findViewById(R.id.segment16_res).setVisibility(View.VISIBLE);
                break;
            case 2:
                findViewById(R.id.segment17_res).setVisibility(View.VISIBLE);
                break;
            case 3:
                findViewById(R.id.segment18_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment18_2_res).setVisibility(View.VISIBLE);
                break;
            case 4:
                findViewById(R.id.segment19_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment19_2_res).setVisibility(View.VISIBLE);
                break;
            case 5:
                findViewById(R.id.segment16_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment19_1_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment19_2_res).setVisibility(View.VISIBLE);
                break;
            case 6:
                findViewById(R.id.segment20_res).setVisibility(View.VISIBLE);
                break;
            case 7:
                findViewById(R.id.segment16_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment20_res).setVisibility(View.VISIBLE);
                break;
            case 8:
                findViewById(R.id.segment17_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment20_res).setVisibility(View.VISIBLE);
                break;
            case 9:
                findViewById(R.id.segment16_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment17_res).setVisibility(View.VISIBLE);
                findViewById(R.id.segment20_res).setVisibility(View.VISIBLE);
                break;
        }

    }
}

